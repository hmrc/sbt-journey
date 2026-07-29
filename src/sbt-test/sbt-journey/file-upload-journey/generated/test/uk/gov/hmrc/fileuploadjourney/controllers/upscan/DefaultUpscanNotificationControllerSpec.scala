package uk.gov.hmrc.fileuploadjourney.controllers.upscan

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset,verify,when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.http.ContentTypes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.fileuploadjourney.models.UploadId
import uk.gov.hmrc.fileuploadjourney.models.upscan.*
import uk.gov.hmrc.fileuploadjourney.repositories.FileUploadRepository

import java.net.URI
import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext,Future}
import scala.util.Random

class DefaultUpscanNotificationControllerSpec extends AnyFlatSpec, Matchers, BeforeAndAfterEach, MockitoSugar {
  given system: ActorSystem = ActorSystem("test")
  given ExecutionContext    = system.dispatcher

  private val repository = mock[FileUploadRepository]
  private val controller = new DefaultUpscanNotificationController(
    repository, stubMessagesControllerComponents())

  private val upscanReference = UUID.randomUUID().toString
  private val downloadUrl = s"http://localhost:9570/upscan/download/$upscanReference"
  private val uploadTimestamp = Instant.now()
  private val checksum = Random.nextBytes(32).map(b => f"$b%02x").mkString
  private val fileSize = Random.nextInt()

  private val readyNotificationJson = Json.obj(
    "fileStatus" -> "READY",
    "reference" -> upscanReference,
    "downloadUrl" -> downloadUrl,
    "uploadDetails" -> Json.obj(
      "fileName" -> "sample.png",
      "fileMimeType" -> "image/png",
      "uploadTimestamp" -> uploadTimestamp.toString,
      "checksum" -> checksum,
      "size" -> fileSize
    )
  )
  private val readyNotification = UpscanNotification.Ready(
    UpscanReference(upscanReference),
    URI.create(downloadUrl),
    UploadDetails("sample.png", "image/png", uploadTimestamp, checksum, fileSize)
  )

  private val failedNotificationJson = Json.obj(
    "fileStatus" -> "FAILED",
    "reference" -> upscanReference,
    "failureDetails" -> Json.obj(
      "failureReason" -> "QUARANTINE",
      "message" -> "MyDoom"
    )
  )
  private val failedNotification = UpscanNotification.Failed(
    UpscanReference(upscanReference),
    FailureDetails("QUARANTINE", "MyDoom")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(repository)
  }

  "DefaultUpscanNotificationController.onPageLoad" should "return a 204 No Content response when a ready notification is received" in {
    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
    val uploadId = UUID.randomUUID()
    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
    val request = FakeRequest(route)
      .withBody(readyNotificationJson)
      .withHeaders(CONTENT_TYPE -> JSON)
    val response = controller.onNotificationReceived(uploadId)(request)
    status(response) shouldBe NO_CONTENT
    verify(repository).handleNotification(UploadId(uploadId), readyNotification)
  }

  it should "return a 204 No Content response when a failed notification is received" in {
    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
    val uploadId = UUID.randomUUID()
    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
    val request = FakeRequest(route)
      .withBody(failedNotificationJson)
      .withHeaders(CONTENT_TYPE -> JSON)
    val response = controller.onNotificationReceived(uploadId)(request)
    status(response) shouldBe NO_CONTENT
    verify(repository).handleNotification(UploadId(uploadId), failedNotification)
  }

  it should "return a 400 Bad Request response when the request body is not a valid UpscanNotification" in {
    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
    val uploadId = UUID.randomUUID()
    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
    val request = FakeRequest(route)
      .withBody(Json.obj())
      .withHeaders(CONTENT_TYPE -> JSON)
    val response = controller.onNotificationReceived(uploadId)(request)
    status(response) shouldBe BAD_REQUEST
  }

  it should "return a 400 Bad Request response when the request body cannot be parsed as JSON" in {
    when(repository.handleNotification(UploadId(any()), any())).thenReturn(Future.unit)
    val uploadId = UUID.randomUUID()
    val route = routes.UpscanNotificationBaseController.onNotificationReceived(uploadId)
    val request = FakeRequest(route)
      .withBody("""{"fileStatus":"READY}""")
      .withHeaders(CONTENT_TYPE -> JSON)
    val response = controller.onNotificationReceived(uploadId)(request)
    status(response) shouldBe BAD_REQUEST
  }
}
