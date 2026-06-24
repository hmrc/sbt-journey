package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import uk.gov.hmrc.simplejourney.models.{Choice,TaxRegime}

case class TaxRegimePage private (override val path: JsPath) extends QuestionPage[TaxRegime] {
  override def toString: String = "taxRegime"
}

object TaxRegimePage {
  def apply(addATaxRegime: Choice, taxRegimesIndex: Int): TaxRegimePage =
    new TaxRegimePage(
      JsPath \ "addATaxRegime" \ addATaxRegime.toString \ "taxRegimes" \ taxRegimesIndex \ "taxRegime"
    )

  def unapply(page: TaxRegimePage): Option[(Choice, Int)] =
    page.path.path match {
      case KeyPathNode("addATaxRegime")
        :: KeyPathNode(addATaxRegime)
        :: KeyPathNode("taxRegimes")
        :: IdxPathNode(taxRegimesIndex)
        :: KeyPathNode("taxRegime")
        :: Nil => Some((Choice.valueOf(addATaxRegime), taxRegimesIndex))
      case _ => None
    }
}
