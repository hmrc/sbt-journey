package uk.gov.hmrc.fileuploadjourney.controllers

import _root_.controllers.actions.* // uk.gov.hmrc.fileuploadjourney.controllers.actions.*
import _root_.controllers.routes // uk.gov.hmrc.fileuploadjourney.controllers.routes
import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.fileuploadjourney.models.{NormalMode,UserAnswers}
import _root_.generators.Generators // uk.gov.hmrc.fileuploadjourney.generators.Generators
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify,when}
import org.mongodb.scala.MongoException
import org.scalacheck.{Arbitrary,Gen}
import org.scalatest.{RecoverMethods,TryValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form,FormError}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat.raw
import repositories.SessionRepository // uk.gov.hmrc.fileuploadjourney.repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.fileuploadjourney.connectors.UpscanConnector
import uk.gov.hmrc.fileuploadjourney.controllers.routes as journeyRoutes
import uk.gov.hmrc.fileuploadjourney.forms.UploadSupportingDocumentBaseFormProvider
import uk.gov.hmrc.fileuploadjourney.forms.*
import uk.gov.hmrc.fileuploadjourney.models.*
import uk.gov.hmrc.fileuploadjourney.models.upscan.*
import uk.gov.hmrc.fileuploadjourney.navigation.JourneyNavigator
import uk.gov.hmrc.fileuploadjourney.pages.*
import uk.gov.hmrc.fileuploadjourney.repositories.FileUploadRepository
import views.html.UploadSupportingDocumentView

import java.time.{Instant,ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Success,Try}

class DefaultUploadSupportingDocumentControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
  given system: ActorSystem = ActorSystem("test")
  given ExecutionContext    = system.dispatcher
  given Messages            = stubMessages()

  private val defaultFormProvider = new DefaultUploadSupportingDocumentFormProvider()
  private val mockViewResponse    = "<html>Hello</html>"
  private val fileReference       = UUID.randomUUID().toString
  private val responseTime        = Instant.now().atZone(ZoneOffset.UTC)
  private val formatter           = DateTimeFormatter.ISO_ZONED_DATE_TIME
  private val shortFormatter      = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")

  private val initiateResponse = UpscanInitiateResponse(
    reference = UpscanReference(fileReference),
    uploadRequest = UpscanFormTemplate(
      href = "http://localhost:9570/upscan/upload-proxy",
      fields = Map(
        "success_action_redirect" -> s"http://localhost:9000/file-upload-journey/",
        "x-amz-credential"                    -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
        "x-amz-meta-upscan-initiate-response" -> formatter.format(responseTime),
        "x-amz-meta-original-filename"        -> "${filename}",
        "x-amz-algorithm"                     -> "AWS4-HMAC-SHA256",
        "x-amz-signature"                     -> "xxxx",
        "error_action_redirect"   -> "http://localhost:9000/file-upload-journey/",
        "x-amz-meta-session-id"   -> UUID.randomUUID().toString,
        "x-amz-meta-callback-url" -> "http://localhost:9000/file-upload-journey/",
        "x-amz-date"              -> shortFormatter.format(responseTime),
        "x-amz-meta-upscan-initiate-received" -> formatter.format(responseTime),
        "x-amz-meta-request-id"               -> UUID.randomUUID().toString,
        "key"                                 -> fileReference,
        "acl"                                 -> "private",
        "x-amz-meta-consuming-service"        -> "file-upload-journey",
        "policy" -> "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMCwxMDQ4NTc2MDBdXX0="
      )
    )
  )

  private def makeController(
    userAnswers: Option[UserAnswers],
    navigator: JourneyNavigator = mock[JourneyNavigator],
    repository: SessionRepository = mock[SessionRepository],
    upscanConnector: UpscanConnector = mock[UpscanConnector],
    fileUploadRepository: FileUploadRepository = mock[FileUploadRepository],
    formProvider: UploadSupportingDocumentBaseFormProvider = mock[UploadSupportingDocumentBaseFormProvider],
    view: views.html.UploadSupportingDocumentView = mock[views.html.UploadSupportingDocumentView]
  ) = new DefaultUploadSupportingDocumentController(
    new FakeIdentifierAction(stubPlayBodyParsers),
    new FakeDataRetrievalAction(userAnswers),
    new DataRequiredActionImpl(),
    navigator,
    repository,
    upscanConnector,
    fileUploadRepository,
    formProvider,
    view,
    stubMessagesControllerComponents()
  )

  "DefaultUploadSupportingDocumentController.onPageLoad(Yes, i)" should "return a 200 OK response containing the view HTML when everything is successful" in {
    val upscanConnector = mock[UpscanConnector]
    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
    val fileUploadRepository = mock[FileUploadRepository]
    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
    val view = mock[UploadSupportingDocumentView]
    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
    val controller = makeController(
      userAnswers = Some(UserAnswers("id")),
      upscanConnector = upscanConnector,
      fileUploadRepository = fileUploadRepository,
      formProvider = defaultFormProvider,
      view = view
    )
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onPageLoad(0, NormalMode, None, None, None)
    val request = FakeRequest(route)
    val response = controller.onPageLoad(0, NormalMode, None, None, None)(request)
    status(response) shouldBe OK
    contentAsString(response) shouldBe mockViewResponse
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
    formCaptor.getValue().errors should be(empty)
  }

  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
    val controller = makeController(userAnswers = None)
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onPageLoad(0, NormalMode, None, None, None)
    val request = FakeRequest(route)
    val response = controller.onPageLoad(0, NormalMode, None, None, None)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }

  it should "render the upload form with errors when the user is redirected to the page with error detail parameters" in {
    val upscanConnector = mock[UpscanConnector]
    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
    val fileUploadRepository = mock[FileUploadRepository]
    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
    when(fileUploadRepository.setRejected(any(), UpscanReference(any()))).thenReturn(Future.unit)
    val view = mock[UploadSupportingDocumentView]
    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
    val controller = makeController(
      userAnswers = Some(UserAnswers("id")),
      upscanConnector = upscanConnector,
      fileUploadRepository = fileUploadRepository,
      formProvider = defaultFormProvider,
      view = view
    )
    val key = Some(fileReference)
    val errorCode = Some("EntityTooSmall")
    val errorMessage = Some("we were instructed to reject this upload")
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onPageLoad(0, NormalMode, key, errorCode, errorMessage)
    val request = FakeRequest(route)
    val response = controller.onPageLoad(0, NormalMode, key, errorCode, errorMessage)(request)
    status(response) shouldBe OK
    contentAsString(response) shouldBe mockViewResponse
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
    formCaptor.getValue().errors should contain(FormError("file", "upload.error.fileTooSmall"))
  }

  it should "throw exceptions to the top level error handler" in {
    val upscanConnector = mock[UpscanConnector]
    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
    val controller = makeController(userAnswers = Some(UserAnswers("id")), upscanConnector = upscanConnector)
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onPageLoad(0, NormalMode, None, None, None)
    val request = FakeRequest(route)
    recoverToSucceededIf[UpstreamErrorResponse] {
      controller.onPageLoad(0, NormalMode, None, None, None)(request)
    }
  }

  "DefaultUploadSupportingDocumentController.onUploadSuccess(Yes, i)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: UploadId) =>
    val navigator = mock[JourneyNavigator]
    val nextPage = routes.CheckYourAnswersController.onPageLoad()
    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
    val repository = mock[SessionRepository]
    when(repository.set(any())).thenReturn(Future.successful(true))
    val fileUploadRepository = mock[FileUploadRepository]
    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
    val userAnswers = for {
      answers <- Try(UserAnswers("id"))
      addSupportingDocuments = Choice.Yes
      answers <- answers.set(AddSupportingDocumentsPage, addSupportingDocuments)
    } yield answers
    val controller = makeController(
      userAnswers = userAnswers.toOption,
      navigator = navigator,
      repository = repository,
      fileUploadRepository = fileUploadRepository,
      formProvider = defaultFormProvider
    )
    val uploadId = UUID.randomUUID()
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onUploadSuccess(0, uploadId, NormalMode)
    val request = FakeRequest(route)
    val response = controller.onUploadSuccess(0, uploadId, NormalMode)(request)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(nextPage.path())
  }

  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: UploadId) =>
    val controller = makeController(userAnswers = None)
    val uploadId = UUID.randomUUID()
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onUploadSuccess(0, uploadId, NormalMode)
    val request = FakeRequest(route)
    val response = controller.onUploadSuccess(0, uploadId, NormalMode)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }

  it should "redirect to journey recovery when a prerequisite answer is missing" in {
    val userAnswers = Success(UserAnswers("id"))
    val controller = makeController(userAnswers = userAnswers.toOption)
    val uploadId = UUID.randomUUID()
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onUploadSuccess(0, uploadId, NormalMode)
    val request = FakeRequest(route)
    val response = controller.onUploadSuccess(0, uploadId, NormalMode)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }

  it should "throw exceptions to the top level error handler" in {
    val fileUploadRepository = mock[FileUploadRepository]
    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.failed(new MongoException("Error")))
    val userAnswers = for {
      answers <- Try(UserAnswers("id"))
      addSupportingDocuments = Choice.Yes
      answers <- answers.set(AddSupportingDocumentsPage, addSupportingDocuments)
    } yield answers
    val controller = makeController(userAnswers = userAnswers.toOption, fileUploadRepository = fileUploadRepository)
    val uploadId = UUID.randomUUID()
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onUploadSuccess(0, uploadId, NormalMode)
    val request = FakeRequest(route)
    recoverToSucceededIf[MongoException] {
      controller.onUploadSuccess(0, uploadId, NormalMode)(request)
    }
  }

  "DefaultUploadSupportingDocumentController.onUploadFailure(Yes, i)" should "redirect to onPageLoad preserving the query string" in {
    val controller = makeController(userAnswers = Some(UserAnswers("id")))
    val uploadId = UUID.randomUUID()
    val key = Some(fileReference)
    val errorCode = Some("EntityTooSmall")
    val errorMessage = Some("we were instructed to reject this upload")
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)
    val request = FakeRequest(route)
    val response = controller.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)(request)
    val nextPage = journeyRoutes.UploadSupportingDocumentBaseController.onPageLoad(0, NormalMode, key, errorCode, errorMessage)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(nextPage.path())
  }

  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
    val controller = makeController(userAnswers = None)
    val uploadId = UUID.randomUUID()
    val key = Some(fileReference)
    val errorCode = Some("EntityTooSmall")
    val errorMessage = Some("we were instructed to reject this upload")
    val route = journeyRoutes.UploadSupportingDocumentBaseController.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)
    val request = FakeRequest(route)
    val response = controller.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }
}
