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
  def render(basePackage: QualifiedName) = {
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
}
