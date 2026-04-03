package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes


case class DataDomainPage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[String] {
  override def submitRoute(mode: Mode): Call = makeRoute(mode)
  override def toString: String = "dataDomain"
}

object DataDomainPage {
  def apply(dataDomainsIndex: Int): DataDomainPage =
    new DataDomainPage(
      JsPath \ "dataDomains" \ dataDomainsIndex \ "dataDomain",
      mode => routes.DataDomainBaseController.onSubmit(dataDomainsIndex, mode)
    )

  def unapply(page: DataDomainPage): Option[Int] =
    page.path.path match {
      case KeyPathNode("dataDomains") :: IdxPathNode(dataDomainsIndex) :: KeyPathNode("dataDomain") :: Nil => Some(dataDomainsIndex)
      case _ => None
    }
}
