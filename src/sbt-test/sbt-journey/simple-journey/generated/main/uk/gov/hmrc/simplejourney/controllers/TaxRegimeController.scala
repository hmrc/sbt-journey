package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.*  // uk.gov.hmrc.simplejourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.simplejourney.controllers.routes
import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.simplejourney.repositories.SessionRepository
import uk.gov.hmrc.simplejourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.forms.TaxRegimeBaseFormProvider
import uk.gov.hmrc.simplejourney.navigation.*
import uk.gov.hmrc.simplejourney.pages.*
import views.html.TaxRegimeView

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultTaxRegimeController])
trait TaxRegimeBaseController extends FrontendBaseController, I18nSupport {
  def onPageLoad(mode: Mode): Action[AnyContent]
  def onSubmit(mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultTaxRegimeController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: TaxRegimeBaseFormProvider,
  view: TaxRegimeView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends TaxRegimeBaseController {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(mode)
    val userAnswers = request.userAnswers
    val result = for {
      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
      page = TaxRegimePage(addATaxRegime)
      preparedForm = userAnswers.get(page)
        .map(form().fill)
        .getOrElse(form())
    } yield Ok(view(preparedForm, submitRoute, mode))
    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(mode)
    val userAnswers = request.userAnswers
    val result = for {
      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
      page = TaxRegimePage(addATaxRegime)
    } yield form().bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
      answer =>
        for {
          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
          _ <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
    )
    result.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
  }
}
