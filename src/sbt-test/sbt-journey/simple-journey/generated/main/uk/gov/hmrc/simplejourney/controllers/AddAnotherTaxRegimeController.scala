package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.*  // uk.gov.hmrc.simplejourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.simplejourney.controllers.routes
import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.simplejourney.repositories.SessionRepository
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

@ImplementedBy(classOf[DefaultAddAnotherTaxRegimeController])
trait AddAnotherTaxRegimeBaseController extends FrontendBaseController with I18nSupport {
  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent]
  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultAddAnotherTaxRegimeController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: AddAnotherTaxRegimeBaseFormProvider,
  view: views.html.AddAnotherTaxRegimeView,
  override val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext) extends AddAnotherTaxRegimeBaseController {

  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val userAnswers = request.userAnswers
    val result = for {
      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
      page = AddAnotherTaxRegimePage(addATaxRegime, taxRegimesIndex)
    } yield Ok(view(form(), page.submitRoute(mode), mode))
    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }

  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val userAnswers = request.userAnswers
    val result = for {
      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
      page = AddAnotherTaxRegimePage(addATaxRegime, taxRegimesIndex)
    } yield form().bindFromRequest().fold(
      formWithErrors =>
        BadRequest(view(formWithErrors, page.submitRoute(mode), mode)),
      answer =>
        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
    )
    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }
}
