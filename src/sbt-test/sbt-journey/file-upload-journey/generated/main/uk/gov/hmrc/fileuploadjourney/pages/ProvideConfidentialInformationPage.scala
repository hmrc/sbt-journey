package uk.gov.hmrc.fileuploadjourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath,KeyPathNode}
import uk.gov.hmrc.fileuploadjourney.models.Choice

case class ProvideConfidentialInformationPage private (override val path: JsPath) extends QuestionPage[String] {
  override def toString: String = "provideConfidentialInformation"
}

object ProvideConfidentialInformationPage {
  def apply(addConfidentialInformation: Choice): ProvideConfidentialInformationPage =
    new ProvideConfidentialInformationPage(
      JsPath \ "addConfidentialInformation" \ addConfidentialInformation.toString \ "provideConfidentialInformation"
    )

  def unapply(page: ProvideConfidentialInformationPage): Option[Choice] =
    page.path.path match {
      case KeyPathNode("addConfidentialInformation")
        :: KeyPathNode(addConfidentialInformation)
        :: KeyPathNode("provideConfidentialInformation")
        :: Nil => Some(Choice.valueOf(addConfidentialInformation))
      case _ => None
    }
}
