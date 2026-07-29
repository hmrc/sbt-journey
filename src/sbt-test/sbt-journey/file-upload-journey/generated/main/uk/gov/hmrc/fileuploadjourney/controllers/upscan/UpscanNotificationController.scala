package uk.gov.hmrc.fileuploadjourney.controllers.upscan

import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.fileuploadjourney.models.UploadId
import uk.gov.hmrc.fileuploadjourney.models.upscan.UpscanNotification
import uk.gov.hmrc.fileuploadjourney.repositories.FileUploadRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import com.google.inject.ImplementedBy
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultUpscanNotificationController])
trait UpscanNotificationBaseController extends FrontendBaseController {
  def onNotificationReceived(id: UUID): Action[JsValue]
}

@Singleton
class DefaultUpscanNotificationController @Inject() (
  fileUploadRepository: FileUploadRepository,
  override val controllerComponents: MessagesControllerComponents
)(using ec: ExecutionContext) extends UpscanNotificationBaseController {
  def onNotificationReceived(id: UUID) = Action.async(parse.json) { implicit request =>
    withJsonBody[UpscanNotification] { notification =>
      fileUploadRepository
        .handleNotification(UploadId(id), notification)
        .map { _ => NoContent }
    }
  }
}
