package uk.gov.hmrc.fileuploadjourney.controllers

import controllers.actions.*  // uk.gov.hmrc.fileuploadjourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.fileuploadjourney.controllers.routes
import models.Mode // uk.gov.hmrc.fileuploadjourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.fileuploadjourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.fileuploadjourney.repositories.SessionRepository
import uk.gov.hmrc.fileuploadjourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.fileuploadjourney.models.*
import uk.gov.hmrc.fileuploadjourney.forms.AddAnotherSupportingDocumentBaseFormProvider
import uk.gov.hmrc.fileuploadjourney.navigation.*
import uk.gov.hmrc.fileuploadjourney.pages.*
import views.html.AddAnotherSupportingDocumentView

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultAddAnotherSupportingDocumentController])
trait AddAnotherSupportingDocumentBaseController extends FrontendBaseController, I18nSupport {
  def onPageLoad(supportingDocumentsIndex: Int, mode: Mode): Action[AnyContent]
  def onSubmit(supportingDocumentsIndex: Int, mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultAddAnotherSupportingDocumentController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: AddAnotherSupportingDocumentBaseFormProvider,
  view: AddAnotherSupportingDocumentView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends AddAnotherSupportingDocumentBaseController {

  def onPageLoad(supportingDocumentsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.AddAnotherSupportingDocumentBaseController.onSubmit(supportingDocumentsIndex, mode)
    val userAnswers = request.userAnswers
    val result = for {
      addSupportingDocuments <- userAnswers.get(AddSupportingDocumentsPage)
      page = AddAnotherSupportingDocumentPage(addSupportingDocuments, supportingDocumentsIndex)
    } yield Ok(view(form(), submitRoute, mode))
    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }

  def onSubmit(supportingDocumentsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.AddAnotherSupportingDocumentBaseController.onSubmit(supportingDocumentsIndex, mode)
    val userAnswers = request.userAnswers
    val result = for {
      addSupportingDocuments <- userAnswers.get(AddSupportingDocumentsPage)
      page = AddAnotherSupportingDocumentPage(addSupportingDocuments, supportingDocumentsIndex)
    } yield form().bindFromRequest().fold(
      formWithErrors =>
        BadRequest(view(formWithErrors, submitRoute, mode)),
      answer =>
        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
    )
    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }
}
