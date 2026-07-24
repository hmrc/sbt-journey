package uk.gov.hmrc.fileuploadjourney.controllers

import controllers.actions.*  // uk.gov.hmrc.fileuploadjourney.controllers.actions.*
import controllers.routes // uk.gov.hmrc.fileuploadjourney.controllers.routes
import models.Mode // uk.gov.hmrc.fileuploadjourney.models.Mode
import models.UserAnswers // uk.gov.hmrc.fileuploadjourney.models.UserAnswers
import repositories.SessionRepository // uk.gov.hmrc.fileuploadjourney.repositories.SessionRepository
import uk.gov.hmrc.fileuploadjourney.controllers.{routes as journeyRoutes}
import uk.gov.hmrc.fileuploadjourney.models.*
import uk.gov.hmrc.fileuploadjourney.forms.ProvideGoodsDescriptionBaseFormProvider
import uk.gov.hmrc.fileuploadjourney.navigation.*
import uk.gov.hmrc.fileuploadjourney.pages.*
import views.html.ProvideGoodsDescriptionView

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultProvideGoodsDescriptionController])
trait ProvideGoodsDescriptionBaseController extends FrontendBaseController, I18nSupport {
  def onPageLoad(mode: Mode): Action[AnyContent]
  def onSubmit(mode: Mode): Action[AnyContent]
}

@Singleton
class DefaultProvideGoodsDescriptionController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: JourneyNavigator,
  sessionRepository: SessionRepository,
  form: ProvideGoodsDescriptionBaseFormProvider,
  view: ProvideGoodsDescriptionView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends ProvideGoodsDescriptionBaseController {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val submitRoute = journeyRoutes.ProvideGoodsDescriptionBaseController.onSubmit(mode)
    val page = ProvideGoodsDescriptionPage
    val userAnswers = request.userAnswers
    val preparedForm = userAnswers
      .get(page)
      .map(form().fill)
      .getOrElse(form())
    Ok(view(preparedForm, submitRoute, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val submitRoute = journeyRoutes.ProvideGoodsDescriptionBaseController.onSubmit(mode)
    val page = ProvideGoodsDescriptionPage
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
