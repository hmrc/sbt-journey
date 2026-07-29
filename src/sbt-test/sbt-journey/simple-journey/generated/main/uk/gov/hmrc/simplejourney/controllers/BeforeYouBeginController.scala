package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.* // uk.gov.hmrc.simplejourney.controllers.actions.*

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultBeforeYouBeginController])
trait BeforeYouBeginBaseController extends FrontendBaseController with I18nSupport {
  def onPageLoad: Action[AnyContent]
}

@Singleton
class DefaultBeforeYouBeginController @Inject() (
  identify: IdentifierAction,
  view: views.html.BeforeYouBeginView,
  override val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends BeforeYouBeginBaseController {
  def onPageLoad: Action[AnyContent] = identify { implicit request =>
    Ok(view())
  }
}
