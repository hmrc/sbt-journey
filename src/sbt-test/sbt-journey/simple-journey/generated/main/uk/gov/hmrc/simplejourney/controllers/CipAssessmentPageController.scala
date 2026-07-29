package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.*  // uk.gov.hmrc.simplejourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.simplejourney.controllers.routes
import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.simplejourney.repositories.SessionRepository
import uk.gov.hmrc.simplejourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.forms.CipAssessmentPageBaseFormProvider
import uk.gov.hmrc.simplejourney.navigation.*
import uk.gov.hmrc.simplejourney.pages.*
import views.html.CipAssessmentPageView

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultCipAssessmentPageController])
trait CipAssessmentPageBaseController extends FrontendBaseController, I18nSupport {
  def onPageLoad(mode: Mode): Action[AnyContent]
  def onSubmit(mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultCipAssessmentPageController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: CipAssessmentPageBaseFormProvider,
  view: CipAssessmentPageView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends CipAssessmentPageBaseController {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.CipAssessmentPageBaseController.onSubmit(mode)
    val page = CipAssessmentPagePage
    val userAnswers = request.userAnswers
    val preparedForm = userAnswers
      .get(page)
      .map(form().fill)
      .getOrElse(form())
    Ok(view(preparedForm, submitRoute, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val submitRoute = journeyRoutes.CipAssessmentPageBaseController.onSubmit(mode)
    val page = CipAssessmentPagePage
    val userAnswers = request.userAnswers
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
