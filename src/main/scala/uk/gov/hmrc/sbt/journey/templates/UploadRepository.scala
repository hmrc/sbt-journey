/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.sbt.journey.templates

import uk.gov.hmrc.sbt.journey.models.QualifiedName

object UploadRepository {
  def render(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "repositories"}
       |
       |import org.bson.UuidRepresentation
       |import org.bson.codecs.UuidCodec
       |import org.mongodb.scala.model.Filters.{eq as eqTo, *}
       |import org.mongodb.scala.model.Updates.*
       |import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
       |import play.api.Configuration
       |import ${basePackage / "models" / "*"}
       |import ${basePackage / "models" / "upscan" / "*"}
       |import uk.gov.hmrc.mongo.MongoComponent
       |import uk.gov.hmrc.mongo.play.json.Codecs
       |import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
       |
       |import java.time.{Clock, Duration}
       |import java.util.concurrent.TimeUnit
       |import javax.inject.{Inject,Singleton}
       |import scala.concurrent.{ExecutionContext, Future}
       |
       |@Singleton
       |class FileUploadRepository @Inject() (
       |  config: Configuration,
       |  mongoComponent: MongoComponent,
       |  clock: Clock
       |)(using
       |  ExecutionContext
       |) extends PlayMongoRepository[FileUpload](
       |    collectionName = "file-uploads",
       |    mongoComponent = mongoComponent,
       |    domainFormat = FileUpload.format,
       |    extraCodecs = Seq(
       |      new UuidCodec(UuidRepresentation.STANDARD),
       |      Codecs.playFormatCodec(UploadStatus.format),
       |      Codecs.playFormatCodec(UploadDetails.format),
       |      Codecs.playFormatCodec(FailureDetails.format)
       |    ),
       |    indexes = Seq(
       |      new IndexModel(Indexes.ascending("id"), IndexOptions().unique(true)),
       |      new IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true)),
       |      new IndexModel(
       |        Indexes.ascending("initiatedAt"),
       |        IndexOptions().expireAfter(
       |          config.get[Duration]("mongodb.collections.file-uploads.initiatedAt.ttl").toSeconds,
       |          TimeUnit.SECONDS
       |        )
       |      ),
       |      new IndexModel(
       |        Indexes.ascending("updatedAt"),
       |        IndexOptions().expireAfter(
       |          config.get[Duration]("mongodb.collections.file-uploads.updatedAt.ttl").toSeconds,
       |          TimeUnit.SECONDS
       |        )
       |      )
       |    )
       |  ) {
       |
       |  def initiate(uploadId: UploadId, userId: String, reference: UpscanReference): Future[UploadId] = {
       |    collection
       |      .insertOne(FileUpload.Initiated(uploadId, userId, reference, clock.instant()))
       |      .toFuture()
       |      .map(_ => uploadId)
       |  }
       |
       |  def setRejected(userId: String, reference: UpscanReference): Future[Unit] = {
       |    collection
       |      .deleteOne(and(
       |        eqTo("reference", reference.reference),
       |        eqTo("userId", userId),
       |        in("uploadStatus", UploadStatus.Initiated, UploadStatus.Processing)
       |      ))
       |      .toFuture()
       |      .map(_ => ())
       |  }
       |
       |  def setProcessing(uploadId: UploadId, userId: String): Future[Unit] = {
       |    collection
       |      .findOneAndUpdate(
       |        and(
       |          eqTo("id", uploadId.id),
       |          eqTo("userId", userId),
       |          in("uploadStatus", UploadStatus.Initiated)
       |        ),
       |        combine(
       |          set("uploadStatus", UploadStatus.Processing),
       |          currentDate("updatedAt"),
       |          unset("initiatedAt")
       |        )
       |      )
       |      .toFuture()
       |      .map(_ => ())
       |  }
       |
       |  def handleNotification(uploadId: UploadId, notification: UpscanNotification): Future[Unit] = {
       |    collection
       |      .findOneAndUpdate(
       |        and(
       |          eqTo("id", uploadId.id),
       |          in("uploadStatus", UploadStatus.Initiated, UploadStatus.Processing)
       |        ),
       |        notification match {
       |          case UpscanNotification.Ready(reference, downloadUrl, uploadDetails) =>
       |            combine(
       |              set("uploadStatus", UploadStatus.Ready),
       |              set("downloadUrl", downloadUrl.toString),
       |              set("uploadDetails", uploadDetails),
       |              currentDate("updatedAt")
       |            )
       |          case UpscanNotification.Failed(reference, failureDetails) =>
       |            combine(
       |              set("uploadStatus", UploadStatus.Failed),
       |              set("failureDetails", failureDetails),
       |              currentDate("updatedAt")
       |            )
       |        }
       |      )
       |      .toFuture()
       |      .map(_ => ())
       |  }
       |
       |  def get(uploadId: UploadId, userId: String): Future[Option[FileUpload]] = {
       |    collection
       |      .find(and(eqTo("id", uploadId.id), eqTo("userId", userId)))
       |      .toFuture()
       |      .map(_.headOption)
       |  }
       |}
       |""".stripMargin
  }

  def renderSpec(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "repositories"}
       |
       |import org.mongodb.scala.MongoWriteException
       |import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
       |import org.scalatest.flatspec.AnyFlatSpec
       |import org.scalatest.matchers.should.Matchers
       |import org.scalatest.{Inside, OptionValues}
       |import play.api.Configuration
       |import ${basePackage / "models" / "upscan" / "*"}
       |import ${basePackage / "models" / "*"}
       |import uk.gov.hmrc.mongo.test.{
       |  CleanMongoCollectionSupport,
       |  IndexedMongoQueriesSupport,
       |  PlayMongoRepositorySupport
       |}
       |
       |import java.net.URI
       |import java.time.temporal.ChronoUnit
       |import java.time.{Clock, Duration, Instant, ZoneOffset}
       |import java.util.UUID
       |import scala.concurrent.ExecutionContext
       |
       |class FileUploadRepositorySpec
       |  extends AnyFlatSpec,
       |    Matchers,
       |    PlayMongoRepositorySupport[FileUpload],
       |    CleanMongoCollectionSupport,
       |    IndexedMongoQueriesSupport,
       |    OptionValues,
       |    ScalaFutures,
       |    Inside,
       |    IntegrationPatience {
       |
       |  given ExecutionContext = ExecutionContext.global
       |
       |  private val fixedInstant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
       |  private val fixedClock   = Clock.fixed(fixedInstant, ZoneOffset.UTC)
       |
       |  override protected val repository: FileUploadRepository = new FileUploadRepository(
       |    Configuration(
       |      "mongodb.collections.file-uploads.initiatedAt.ttl" -> Duration.ofMinutes(15),
       |      "mongodb.collections.file-uploads.updatedAt.ttl"   -> Duration.ofDays(7)
       |    ),
       |    mongoComponent,
       |    fixedClock
       |  )
       |
       |  "FileUploadRepository.get" should "return records for matching upload ID" in {
       |    val uploadId  = UploadId.next()
       |    val userId    = "user-1"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId, userId, reference, fixedInstant)
       |    insert(initiated).futureValue
       |    repository.get(uploadId, userId).futureValue shouldBe Some(initiated)
       |  }
       |
       |  it should "not return records for non-matching upload ID" in {
       |    val uploadId1 = UploadId.next()
       |    val uploadId2 = UploadId.next()
       |    val userId    = "user-1"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId1, userId, reference, fixedInstant)
       |    insert(initiated).futureValue
       |    repository.get(uploadId2, userId).futureValue shouldBe None
       |  }
       |
       |  it should "not return records for non-matching user ID" in {
       |    val uploadId  = UploadId.next()
       |    val userId1   = "user-1"
       |    val userId2   = "user-2"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId, userId1, reference, fixedInstant)
       |    insert(initiated).futureValue
       |    repository.get(uploadId, userId2).futureValue shouldBe None
       |  }
       |
       |  "FileUploadRepository.initiate" should "create a record for a file upload that has been initiated via Upscan" in {
       |    val uploadId  = UploadId.next()
       |    val userId    = "user-1"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId, userId, reference, fixedInstant)
       |    repository.initiate(uploadId, userId, reference).futureValue shouldBe uploadId
       |    repository.get(uploadId, userId).futureValue shouldBe Some(initiated)
       |  }
       |
       |  it should "prevent duplicate records from being written for the same upload ID" in {
       |    val uploadId   = UploadId.next()
       |    val userId1    = "user-1"
       |    val reference1 = UpscanReference(UUID.randomUUID().toString)
       |    repository.initiate(uploadId, userId1, reference1).futureValue shouldBe uploadId
       |
       |    val userId2    = "user-2"
       |    val reference2 = UpscanReference(UUID.randomUUID().toString)
       |    repository
       |      .initiate(uploadId, userId2, reference2)
       |      .failed
       |      .futureValue shouldBe a[MongoWriteException]
       |  }
       |
       |  it should "prevent duplicate records from being written for the same Upscan reference" in {
       |    val uploadId1 = UploadId.next()
       |    val userId1   = "user-1"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    repository.initiate(uploadId1, userId1, reference).futureValue shouldBe uploadId1
       |
       |    val uploadId2 = UploadId.next()
       |    val userId2   = "user-2"
       |    repository
       |      .initiate(uploadId2, userId2, reference)
       |      .failed
       |      .futureValue shouldBe a[MongoWriteException]
       |  }
       |
       |  "FileUploadRepository.setRejected" should "delete the record for a given upload ID and user ID" in {
       |    val uploadId  = UploadId.next()
       |    val userId    = "user-1"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId, userId, reference, fixedInstant)
       |
       |    insert(initiated).futureValue
       |
       |    repository.setRejected(userId, reference).futureValue
       |
       |    repository.get(uploadId, userId).futureValue shouldBe None
       |  }
       |
       |  it should "only delete records for matching user IDs" in {
       |    val uploadId  = UploadId.next()
       |    val userId1   = "user-1"
       |    val userId2   = "user-2"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId, userId1, reference, fixedInstant)
       |
       |    insert(initiated).futureValue
       |
       |    repository.get(uploadId, userId1).futureValue shouldBe Some(initiated)
       |
       |    repository.setRejected(userId2, reference).futureValue
       |
       |    repository.get(uploadId, userId1).futureValue shouldBe Some(initiated)
       |  }
       |
       |  "FileUploadRepository.setProcessing" should "move an initiated record to the processing status" in {
       |    val uploadId  = UploadId.next()
       |    val userId    = "user-1"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId, userId, reference, fixedInstant)
       |
       |    insert(initiated).futureValue
       |
       |    repository.setProcessing(uploadId, userId).futureValue
       |
       |    val result = repository.get(uploadId, userId).futureValue
       |
       |    inside(result.value) { case FileUpload.Processing(id, uid, ref, updatedAt) =>
       |      id shouldBe uploadId
       |      uid shouldBe userId
       |      ref shouldBe reference
       |      assert(
       |        updatedAt.isAfter(fixedInstant),
       |        s"Expected updatedAt to be after the initiated timestamp $$fixedInstant"
       |      )
       |    }
       |  }
       |
       |  it should "not update a record for the wrong user ID" in {
       |    val uploadId  = UploadId.next()
       |    val userId1   = "user-1"
       |    val userId2   = "user-2"
       |    val reference = UpscanReference(UUID.randomUUID().toString)
       |    val initiated = FileUpload.Initiated(uploadId, userId1, reference, fixedInstant)
       |
       |    insert(initiated).futureValue
       |
       |    repository.setProcessing(uploadId, userId2).futureValue
       |
       |    repository.get(uploadId, userId1).futureValue shouldBe Some(
       |      FileUpload.Initiated(uploadId, userId1, reference, fixedInstant)
       |    )
       |  }
       |
       |  it should "not update a record that is already processing" in {
       |    val uploadId   = UploadId.next()
       |    val userId     = "user-1"
       |    val reference  = UpscanReference(UUID.randomUUID().toString)
       |    val processing = FileUpload.Processing(uploadId, userId, reference, fixedInstant)
       |
       |    insert(processing).futureValue
       |
       |    repository.setProcessing(uploadId, userId).futureValue
       |
       |    val result = repository.get(uploadId, userId).futureValue
       |
       |    inside(result.value) { case FileUpload.Processing(_, _, _, updatedAt) =>
       |      updatedAt shouldBe fixedInstant
       |    }
       |  }
       |
       |  it should "not update a record that is ready" in {
       |    val uploadId      = UploadId.next()
       |    val userId        = "user-1"
       |    val reference     = UpscanReference(UUID.randomUUID().toString)
       |    val downloadUrl   = URI.create("https://example.com/file.pdf")
       |    val uploadDetails = UploadDetails("file.pdf", "application/pdf", fixedInstant, "abcdef", 123456)
       |    val ready =
       |      FileUpload.Ready(uploadId, userId, reference, downloadUrl, uploadDetails, fixedInstant)
       |
       |    insert(ready).futureValue
       |
       |    repository.setProcessing(uploadId, userId).futureValue
       |    repository.get(uploadId, userId).futureValue shouldBe Some(ready)
       |  }
       |
       |  it should "not update a record that is failed" in {
       |    val uploadId       = UploadId.next()
       |    val userId         = "user-1"
       |    val reference      = UpscanReference(UUID.randomUUID().toString)
       |    val failureDetails = FailureDetails("QUARANTINE", "MyDoom")
       |    val failed = FileUpload.Failed(uploadId, userId, reference, failureDetails, fixedInstant)
       |
       |    insert(failed).futureValue
       |
       |    repository.setProcessing(uploadId, userId).futureValue
       |    repository.get(uploadId, userId).futureValue shouldBe Some(failed)
       |  }
       |
       |  "FileUploadRepository.handleNotification" should "update processing records when the notification is a ready notification" in {
       |    val uploadId   = UploadId.next()
       |    val userId     = "user-1"
       |    val reference  = UpscanReference(UUID.randomUUID().toString)
       |    val processing = FileUpload.Processing(uploadId, userId, reference, fixedInstant)
       |
       |    insert(processing).futureValue
       |
       |    val downloadUrl   = URI.create("https://example.com/file.pdf")
       |    val uploadDetails = UploadDetails("file.pdf", "application/pdf", fixedInstant, "abcdef", 123456)
       |    val readyNotification = UpscanNotification.Ready(reference, downloadUrl, uploadDetails)
       |
       |    repository.handleNotification(uploadId, readyNotification).futureValue
       |
       |    val result = repository.get(uploadId, userId).futureValue
       |
       |    inside(result.value) { case FileUpload.Ready(id, uid, ref, url, details, updatedAt) =>
       |      id shouldBe uploadId
       |      uid shouldBe userId
       |      ref shouldBe reference
       |      url shouldBe downloadUrl
       |      details shouldBe uploadDetails
       |      assert(
       |        updatedAt.isAfter(fixedInstant),
       |        s"Expected updatedAt to be after the initiated timestamp $$fixedInstant"
       |      )
       |    }
       |  }
       |
       |  it should "update processing records when the notification is a failed notification" in {
       |    val uploadId   = UploadId.next()
       |    val userId     = "user-1"
       |    val reference  = UpscanReference(UUID.randomUUID().toString)
       |    val processing = FileUpload.Processing(uploadId, userId, reference, fixedInstant)
       |
       |    insert(processing).futureValue
       |
       |    val failureDetails     = FailureDetails("QUARANTINE", "MyDoom")
       |    val failedNotification = UpscanNotification.Failed(reference, failureDetails)
       |
       |    repository.handleNotification(uploadId, failedNotification).futureValue
       |
       |    val result = repository.get(uploadId, userId).futureValue
       |
       |    inside(result.value) { case FileUpload.Failed(id, uid, ref, details, updatedAt) =>
       |      id shouldBe uploadId
       |      uid shouldBe userId
       |      ref shouldBe reference
       |      details shouldBe failureDetails
       |      assert(
       |        updatedAt.isAfter(fixedInstant),
       |        s"Expected updatedAt to be after the initiated timestamp $$fixedInstant"
       |      )
       |    }
       |  }
       |
       |  it should "not update a record that is ready" in {
       |    val uploadId      = UploadId.next()
       |    val userId        = "user-1"
       |    val reference     = UpscanReference(UUID.randomUUID().toString)
       |    val downloadUrl   = URI.create("https://example.com/file.pdf")
       |    val uploadDetails = UploadDetails("file.pdf", "application/pdf", fixedInstant, "abcdef", 123456)
       |    val ready =
       |      FileUpload.Ready(uploadId, userId, reference, downloadUrl, uploadDetails, fixedInstant)
       |
       |    insert(ready).futureValue
       |
       |    val failureDetails     = FailureDetails("QUARANTINE", "MyDoom")
       |    val failedNotification = UpscanNotification.Failed(reference, failureDetails)
       |    repository.handleNotification(uploadId, failedNotification).futureValue
       |
       |    repository.get(uploadId, userId).futureValue shouldBe Some(ready)
       |  }
       |
       |  it should "not update a record that is failed" in {
       |    val uploadId       = UploadId.next()
       |    val userId         = "user-1"
       |    val reference      = UpscanReference(UUID.randomUUID().toString)
       |    val failureDetails = FailureDetails("QUARANTINE", "MyDoom")
       |    val failed = FileUpload.Failed(uploadId, userId, reference, failureDetails, fixedInstant)
       |
       |    insert(failed).futureValue
       |
       |    val downloadUrl   = URI.create("https://example.com/file.pdf")
       |    val uploadDetails = UploadDetails("file.pdf", "application/pdf", fixedInstant, "abcdef", 123456)
       |    val readyNotification = UpscanNotification.Ready(reference, downloadUrl, uploadDetails)
       |    repository.handleNotification(uploadId, readyNotification).futureValue
       |
       |    repository.get(uploadId, userId).futureValue shouldBe Some(failed)
       |  }
       |}
       |""".stripMargin
  }
}
