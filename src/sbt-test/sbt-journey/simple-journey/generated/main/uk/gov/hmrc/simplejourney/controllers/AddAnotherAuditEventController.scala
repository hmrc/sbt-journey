package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.*  // uk.gov.hmrc.simplejourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.simplejourney.controllers.routes
import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.simplejourney.repositories.SessionRepository
import uk.gov.hmrc.simplejourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.forms.*
import uk.gov.hmrc.simplejourney.navigation.*
import uk.gov.hmrc.simplejourney.pages.*

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultAddAnotherAuditEventController])
trait AddAnotherAuditEventBaseController extends FrontendBaseController, I18nSupport {
  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent]
  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultAddAnotherAuditEventController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: AddAnotherAuditEventBaseFormProvider,
  view: views.html.AddAnotherAuditEventView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends AddAnotherAuditEventBaseController {

  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
    val page = AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex)
    Ok(view(form(), submitRoute, mode))
  }

  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
    val page = AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex)
    val userAnswers = request.userAnswers
    form().bindFromRequest().fold(
      formWithErrors =>
        BadRequest(view(formWithErrors, submitRoute, mode)),
      answer =>
        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
    )
  }
}
