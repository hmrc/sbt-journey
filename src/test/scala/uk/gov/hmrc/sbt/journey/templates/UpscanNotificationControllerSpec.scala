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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.sbt.journey.models.QualifiedName

class UpscanNotificationControllerSpec extends AnyFlatSpec with Matchers {
  private val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  "UpscanNotificationController.render" should "render a template for UpscanNotificationController relative to the base package" in {
    UpscanNotificationController.render(basePackage) shouldBe
      s"""package uk.gov.hmrc.sbtjourneytest.controllers.upscan
         |
         |import play.api.libs.json.JsValue
         |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
         |import uk.gov.hmrc.sbtjourneytest.models.UploadId
         |import uk.gov.hmrc.sbtjourneytest.models.upscan.UpscanNotification
         |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
         |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
         |
         |import com.google.inject.ImplementedBy
         |import java.util.UUID
         |import javax.inject.{Inject, Singleton}
         |import scala.concurrent.{ExecutionContext, Future}
         |
         |@ImplementedBy(classOf[DefaultUpscanNotificationController])
         |trait UpscanNotificationBaseController extends FrontendBaseController {
         |  def onNotificationReceived(id: UUID): Action[JsValue]
         |}
         |
         |@Singleton
         |class DefaultUpscanNotificationController @Inject() (
         |  fileUploadRepository: FileUploadRepository,
         |  override val controllerComponents: MessagesControllerComponents
         |)(using ec: ExecutionContext) extends UpscanNotificationBaseController {
         |  def onNotificationReceived(id: UUID) = Action.async(parse.json) { implicit request =>
         |    withJsonBody[UpscanNotification] { notification =>
         |      fileUploadRepository
         |        .handleNotification(UploadId(id), notification)
         |        .map { _ => NoContent }
         |    }
         |  }
         |}
         |""".stripMargin
  }

  "UpscanNotificationController.renderSpec" should "render a template for DefaultUpscanNotificationControllerSpec relative to the base package" in {
    UpscanNotificationController.renderSpec(basePackage) shouldBe
      s"""package uk.gov.hmrc.sbtjourneytest.controllers.upscan
         |
         |import org.apache.pekko.actor.ActorSystem
         |import org.mockito.ArgumentMatchers.any
         |import org.mockito.Mockito.{reset,verify,when}
         |import org.scalatest.BeforeAndAfterEach
         |import org.scalatest.flatspec.AnyFlatSpec
         |import org.scalatest.matchers.should.Matchers
         |import org.scalatestplus.mockito.MockitoSugar
         |import play.api.libs.json.Json
         |import play.api.http.ContentTypes
         |import play.api.test.FakeRequest
         |import play.api.test.Helpers.*
         |import uk.gov.hmrc.sbtjourneytest.models.UploadId
         |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
         |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
         |
         |import java.net.URI
         |import java.time.Instant
         |import java.util.UUID
         |import scala.concurrent.{ExecutionContext,Future}
         |import scala.util.Random
         |
         |class DefaultUpscanNotificationControllerSpec extends AnyFlatSpec, Matchers, BeforeAndAfterEach, MockitoSugar {
         |  given system: ActorSystem = ActorSystem("test")
         |  given ExecutionContext    = system.dispatcher
         |
         |  private val repository = mock[FileUploadRepository]
         |  private val controller = new DefaultUpscanNotificationController(
         |    repository, stubMessagesControllerComponents())
         |
         |  private val upscanReference = UUID.randomUUID().toString
         |  private val downloadUrl = s"http://localhost:9570/upscan/download/$$upscanReference"
         |  private val uploadTimestamp = Instant.now()
         |  private val checksum = Random.nextBytes(32).map(b => f"$$b%02x").mkString
         |  private val fileSize = Random.nextInt()
         |
         |  private val readyNotificationJson = Json.obj(
         |    "fileStatus" -> "READY",
         |    "reference" -> upscanReference,
         |    "downloadUrl" -> downloadUrl,
         |    "uploadDetails" -> Json.obj(
         |      "fileName" -> "sample.png",
         |      "fileMimeType" -> "image/png",
         |      "uploadTimestamp" -> uploadTimestamp.toString,
         |      "checksum" -> checksum,
         |      "size" -> fileSize
         |    )
         |  )
         |  private val readyNotification = UpscanNotification.Ready(
         |    UpscanReference(upscanReference),
         |    URI.create(downloadUrl),
         |    UploadDetails("sample.png", "image/png", uploadTimestamp, checksum, fileSize)
         |  )
         |
         |  private val failedNotificationJson = Json.obj(
         |    "fileStatus" -> "FAILED",
         |    "reference" -> upscanReference,
         |    "failureDetails" -> Json.obj(
         |      "failureReason" -> "QUARANTINE",
         |      "message" -> "MyDoom"
         |    )
         |  )
         |  private val failedNotification = UpscanNotification.Failed(
         |    UpscanReference(upscanReference),
         |    FailureDetails("QUARANTINE", "MyDoom")
         |  )
         |
         |  override def beforeEach(): Unit = {
         |    super.beforeEach()
         |    reset(repository)
         |  }
         |
         |  "DefaultUpscanNotificationController.onPageLoad" should "return a 204 No Content response when a ready notification is received" in {
         |    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
         |    val uploadId = UUID.randomUUID()
         |    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
         |    val request = FakeRequest(route)
         |      .withBody(readyNotificationJson)
         |      .withHeaders(CONTENT_TYPE -> JSON)
         |    val response = controller.onNotificationReceived(uploadId)(request)
         |    status(response) shouldBe NO_CONTENT
         |    verify(repository).handleNotification(UploadId(uploadId), readyNotification)
         |  }
         |
         |  it should "return a 204 No Content response when a failed notification is received" in {
         |    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
         |    val uploadId = UUID.randomUUID()
         |    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
         |    val request = FakeRequest(route)
         |      .withBody(failedNotificationJson)
         |      .withHeaders(CONTENT_TYPE -> JSON)
         |    val response = controller.onNotificationReceived(uploadId)(request)
         |    status(response) shouldBe NO_CONTENT
         |    verify(repository).handleNotification(UploadId(uploadId), failedNotification)
         |  }
         |
         |  it should "return a 400 Bad Request response when the request body is not a valid UpscanNotification" in {
         |    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
         |    val uploadId = UUID.randomUUID()
         |    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
         |    val request = FakeRequest(route)
         |      .withBody(Json.obj())
         |      .withHeaders(CONTENT_TYPE -> JSON)
         |    val response = controller.onNotificationReceived(uploadId)(request)
         |    status(response) shouldBe BAD_REQUEST
         |  }
         |
         |  it should "return a 400 Bad Request response when the request body cannot be parsed as JSON" in {
         |    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
         |    val uploadId = UUID.randomUUID()
         |    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
         |    val request = FakeRequest(route)
         |      .withBody(\"\"\"{"fileStatus":"READY}\"\"\")
         |      .withHeaders(CONTENT_TYPE -> JSON)
         |    val response = controller.onNotificationReceived(uploadId)(request)
         |    status(response) shouldBe BAD_REQUEST
         |  }
         |}
         |""".stripMargin
  }
}
