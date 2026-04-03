package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.JsPath
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes


object ServiceNamePage extends QuestionPage[String] {
  override def path: JsPath = JsPath \ "serviceName"
  override def submitRoute(mode: Mode): Call = routes.ServiceNameBaseController.onSubmit(mode)
  override def toString: String = "serviceName"
}
