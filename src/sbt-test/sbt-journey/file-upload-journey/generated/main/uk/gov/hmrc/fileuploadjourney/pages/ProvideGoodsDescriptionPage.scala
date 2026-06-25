package uk.gov.hmrc.fileuploadjourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.JsPath

object ProvideGoodsDescriptionPage extends QuestionPage[String] {
  override def path: JsPath = JsPath \ "provideGoodsDescription"
  override def toString: String = "provideGoodsDescription"
}
