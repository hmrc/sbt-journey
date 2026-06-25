package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{IdxPathNode,JsPath,KeyPathNode}
import uk.gov.hmrc.simplejourney.models.Choice

case class AddAnotherTaxRegimePage private (override val path: JsPath) extends QuestionPage[Choice] {
  override def toString: String = "addAnotherTaxRegime"
}

object AddAnotherTaxRegimePage {
  def apply(addATaxRegime: Choice, taxRegimesIndex: Int): AddAnotherTaxRegimePage =
    new AddAnotherTaxRegimePage(
      JsPath \ "addATaxRegime" \ addATaxRegime.toString \ "taxRegimes" \ taxRegimesIndex
    )

  def unapply(page: AddAnotherTaxRegimePage): Option[(Choice, Int)] =
    page.path.path match {
      case KeyPathNode("addATaxRegime")
        :: KeyPathNode(addATaxRegime)
        :: KeyPathNode("taxRegimes")
        :: IdxPathNode(taxRegimesIndex)
        :: Nil => Some((Choice.valueOf(addATaxRegime), taxRegimesIndex))
      case _ => None
    }
}
