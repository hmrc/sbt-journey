package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes
import uk.gov.hmrc.simplejourney.models.{Choice,TaxRegime}

case class TaxRegimePage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[TaxRegime] {
  override def submitRoute(mode: Mode): Call = makeRoute(mode)
  override def toString: String = "taxRegime"
}

object TaxRegimePage {
  def apply(addATaxRegime: Choice, taxRegimesIndex: Int): TaxRegimePage =
    new TaxRegimePage(
      JsPath \ "addATaxRegime" \ addATaxRegime.toString \ "taxRegimes" \ taxRegimesIndex \ "taxRegime",
      mode => routes.TaxRegimeBaseController.onSubmit(taxRegimesIndex, mode)
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
