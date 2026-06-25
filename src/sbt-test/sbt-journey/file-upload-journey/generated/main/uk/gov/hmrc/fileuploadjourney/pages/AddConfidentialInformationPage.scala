package uk.gov.hmrc.fileuploadjourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.JsPath
import uk.gov.hmrc.fileuploadjourney.models.Choice

object AddConfidentialInformationPage extends QuestionPage[Choice] {
  override def path: JsPath = JsPath \ "addConfidentialInformation"
  override def toString: String = "addConfidentialInformation"
}
