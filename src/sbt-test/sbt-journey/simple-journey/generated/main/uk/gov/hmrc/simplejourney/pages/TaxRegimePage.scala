package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath,KeyPathNode}
import uk.gov.hmrc.simplejourney.models.{Choice,TaxRegime}

case class TaxRegimePage private (override val path: JsPath) extends QuestionPage[Set[TaxRegime]] {
  override def toString: String = "taxRegime"
}

object TaxRegimePage {
  def apply(addATaxRegime: Choice): TaxRegimePage =
    new TaxRegimePage(
      JsPath \ "addATaxRegime" \ addATaxRegime.toString \ "taxRegime"
    )

  def unapply(page: TaxRegimePage): Option[Choice] =
    page.path.path match {
      case KeyPathNode("addATaxRegime")
        :: KeyPathNode(addATaxRegime)
        :: KeyPathNode("taxRegime")
        :: Nil => Some(Choice.valueOf(addATaxRegime))
      case _ => None
    }
}
