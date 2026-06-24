package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.JsPath
import uk.gov.hmrc.simplejourney.models.Choice

object AddATaxRegimePage extends QuestionPage[Choice] {
  override def path: JsPath = JsPath \ "addATaxRegime"
  override def toString: String = "addATaxRegime"
}
