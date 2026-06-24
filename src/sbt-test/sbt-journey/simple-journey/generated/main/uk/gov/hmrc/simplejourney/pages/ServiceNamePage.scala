package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.JsPath


object ServiceNamePage extends QuestionPage[String] {
  override def path: JsPath = JsPath \ "serviceName"
  override def toString: String = "serviceName"
}
