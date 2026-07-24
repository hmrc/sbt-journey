package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.*  // uk.gov.hmrc.simplejourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.simplejourney.controllers.routes
import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.simplejourney.repositories.SessionRepository
import uk.gov.hmrc.simplejourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.forms.CipAssessmentTicketBaseFormProvider
import uk.gov.hmrc.simplejourney.navigation.*
import uk.gov.hmrc.simplejourney.pages.*
import views.html.CipAssessmentTicketView

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultCipAssessmentTicketController])
trait CipAssessmentTicketBaseController extends FrontendBaseController, I18nSupport {
  def onPageLoad(mode: Mode): Action[AnyContent]
  def onSubmit(mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultCipAssessmentTicketController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: CipAssessmentTicketBaseFormProvider,
  view: CipAssessmentTicketView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends CipAssessmentTicketBaseController {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val submitRoute = journeyRoutes.CipAssessmentTicketBaseController.onSubmit(mode)
    val page = CipAssessmentTicketPage
    val userAnswers = request.userAnswers
      .getOrElse(UserAnswers(request.userId))
    val preparedForm = userAnswers
      .get(page)
      .map(form().fill)
      .getOrElse(form())
    Ok(view(preparedForm, submitRoute, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val submitRoute = journeyRoutes.CipAssessmentTicketBaseController.onSubmit(mode)
    val page = CipAssessmentTicketPage
    val userAnswers = request.userAnswers
      .getOrElse(UserAnswers(request.userId))
    form().bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
      answer =>
        for {
          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
          _ <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
    )
  }
}
