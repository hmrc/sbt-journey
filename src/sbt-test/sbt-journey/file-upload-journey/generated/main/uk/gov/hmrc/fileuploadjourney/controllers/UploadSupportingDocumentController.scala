package uk.gov.hmrc.fileuploadjourney.controllers

import controllers.actions.*  // uk.gov.hmrc.fileuploadjourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.fileuploadjourney.controllers.routes
import models.Mode // uk.gov.hmrc.fileuploadjourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.fileuploadjourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.fileuploadjourney.repositories.SessionRepository
import uk.gov.hmrc.fileuploadjourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.fileuploadjourney.controllers.upscan.{routes as upscanRoutes}
import uk.gov.hmrc.fileuploadjourney.connectors.UpscanConnector
import uk.gov.hmrc.fileuploadjourney.models.*
import uk.gov.hmrc.fileuploadjourney.models.upscan.*
import uk.gov.hmrc.fileuploadjourney.forms.*
import uk.gov.hmrc.fileuploadjourney.navigation.*
import uk.gov.hmrc.fileuploadjourney.repositories.FileUploadRepository
import uk.gov.hmrc.fileuploadjourney.pages.*

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultUploadSupportingDocumentController])
trait UploadSupportingDocumentBaseController extends FrontendBaseController, I18nSupport, Logging {
  def onPageLoad(supportingDocumentsIndex: Int, mode: Mode): Action[AnyContent]
  def onUploadSuccess(supportingDocumentsIndex: Int, id: UUID, mode: Mode): Action[AnyContent]
  def onUploadFailure(supportingDocumentsIndex: Int, id: UUID, mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultUploadSupportingDocumentController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  upscanConnector: UpscanConnector,
  fileUploadRepository: FileUploadRepository,
  form: UploadSupportingDocumentBaseFormProvider,
  view: views.html.UploadSupportingDocumentView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends UploadSupportingDocumentBaseController {

  def onPageLoad(supportingDocumentsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val uploadId = UploadId.next()
    for {
      initiateResponse <- upscanConnector.initiate(
        callbackUrl = upscanRoutes.UpscanNotificationBaseController.onNotificationReceived(uploadId.id),
        successRedirect = journeyRoutes.UploadSupportingDocumentBaseController.onUploadSuccess(supportingDocumentsIndex: Int, uploadId.id, mode),
        errorRedirect = journeyRoutes.UploadSupportingDocumentBaseController.onUploadFailure(supportingDocumentsIndex: Int, uploadId.id, mode)
      )
      uploadId <- fileUploadRepository.initiate(uploadId, request.userId, initiateResponse.reference)
      formTemplate = initiateResponse.uploadRequest
      preparedForm <- request.getQueryString("errorCode").fold(Future.successful(form())) { errorCode =>
        val reference = UpscanReference(request.getQueryString("key").orNull)
        fileUploadRepository.setRejected(request.userId, reference).map { _ =>
          val errorMessage = request.getQueryString("errorMessage").orNull
          logger.error(s"File upload with reference $reference failed with error code $errorCode: $errorMessage")
          val uploadError = UploadError.fromErrorCode(errorCode)
          form().withError("file", uploadError.messageKey)
        }
      }
    } yield Ok(view(preparedForm, formTemplate, mode))
  }

  def onUploadSuccess(supportingDocumentsIndex: Int, id: UUID, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val uploadId = UploadId(id)
    val userAnswers = request.userAnswers
    val result = for {
      addSupportingDocuments <- userAnswers.get(AddSupportingDocumentsPage)
      page = UploadSupportingDocumentPage(addSupportingDocuments, supportingDocumentsIndex)
    } yield for {
      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
    result.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
  }

  def onUploadFailure(supportingDocumentsIndex: Int, id: UUID, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Redirect(journeyRoutes.UploadSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex, mode).path, request.queryString)
  }
}
