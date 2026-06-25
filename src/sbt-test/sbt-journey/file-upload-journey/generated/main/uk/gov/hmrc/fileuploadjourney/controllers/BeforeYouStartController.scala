package uk.gov.hmrc.fileuploadjourney.controllers

import controllers.actions.* // uk.gov.hmrc.fileuploadjourney.controllers.actions.*

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultBeforeYouStartController])
trait BeforeYouStartBaseController extends FrontendBaseController with I18nSupport {
  def onPageLoad: Action[AnyContent]
}

@Singleton
class DefaultBeforeYouStartController @Inject() (
  identify: IdentifierAction,
  view: views.html.BeforeYouStartView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends BeforeYouStartBaseController {
  def onPageLoad: Action[AnyContent] = identify { implicit request =>
    Ok(view())
  }
}
