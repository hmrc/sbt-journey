package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.*  // uk.gov.hmrc.simplejourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.simplejourney.controllers.routes
import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.simplejourney.repositories.SessionRepository
import uk.gov.hmrc.simplejourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.forms.DataDomainBaseFormProvider
import uk.gov.hmrc.simplejourney.navigation.*
import uk.gov.hmrc.simplejourney.pages.*
import views.html.DataDomainView

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultDataDomainController])
trait DataDomainBaseController extends FrontendBaseController, I18nSupport {
  def onPageLoad(dataDomainsIndex: Int, mode: Mode): Action[AnyContent]
  def onSubmit(dataDomainsIndex: Int, mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultDataDomainController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: DataDomainBaseFormProvider,
  view: DataDomainView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends DataDomainBaseController {

  def onPageLoad(dataDomainsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.DataDomainBaseController.onSubmit(dataDomainsIndex, mode)
    val page = DataDomainPage(dataDomainsIndex)
    val userAnswers = request.userAnswers
    val preparedForm = userAnswers
      .get(page)
      .map(form().fill)
      .getOrElse(form())
    Ok(view(preparedForm, submitRoute, mode))
  }

  def onSubmit(dataDomainsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val submitRoute = journeyRoutes.DataDomainBaseController.onSubmit(dataDomainsIndex, mode)
    val page = DataDomainPage(dataDomainsIndex)
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
