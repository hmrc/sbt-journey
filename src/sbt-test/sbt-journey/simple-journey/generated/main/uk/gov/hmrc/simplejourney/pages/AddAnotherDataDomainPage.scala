package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes
import uk.gov.hmrc.simplejourney.models.Choice

case class AddAnotherDataDomainPage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[Choice] {
  override def submitRoute(mode: Mode): Call = makeRoute(mode)
  override def toString: String = "addAnotherDataDomain"
}

object AddAnotherDataDomainPage {
  def apply(dataDomainsIndex: Int): AddAnotherDataDomainPage =
    new AddAnotherDataDomainPage(
      JsPath \ "dataDomains" \ dataDomainsIndex,
      mode => routes.AddAnotherDataDomainBaseController.onSubmit(dataDomainsIndex, mode)
    )

  def unapply(page: AddAnotherDataDomainPage): Option[Int] =
    page.path.path match {
      case KeyPathNode("dataDomains") :: IdxPathNode(dataDomainsIndex) :: Nil => Some(dataDomainsIndex)
      case _ => None
    }
}
