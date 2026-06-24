package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}


case class DataDomainPage private (override val path: JsPath) extends QuestionPage[String] {
  override def toString: String = "dataDomain"
}

object DataDomainPage {
  def apply(dataDomainsIndex: Int): DataDomainPage =
    new DataDomainPage(
      JsPath \ "dataDomains" \ dataDomainsIndex \ "dataDomain"
    )

  def unapply(page: DataDomainPage): Option[Int] =
    page.path.path match {
      case KeyPathNode("dataDomains")
        :: IdxPathNode(dataDomainsIndex)
        :: KeyPathNode("dataDomain")
        :: Nil => Some(dataDomainsIndex)
      case _ => None
    }
}
