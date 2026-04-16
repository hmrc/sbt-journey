package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes
import uk.gov.hmrc.simplejourney.models.Choice

case class AddAnotherTaxRegimePage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[Choice] {
  override def submitRoute(mode: Mode): Call = makeRoute(mode)
  override def toString: String = "addAnotherTaxRegime"
}

object AddAnotherTaxRegimePage {
  def apply(addATaxRegime: Choice, taxRegimesIndex: Int): AddAnotherTaxRegimePage =
    new AddAnotherTaxRegimePage(
      JsPath \ "addATaxRegime" \ addATaxRegime.toString \ "taxRegimes" \ taxRegimesIndex,
      mode => routes.AddAnotherTaxRegimeBaseController.onSubmit(taxRegimesIndex, mode)
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
