package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import uk.gov.hmrc.simplejourney.models.Choice

case class AddAnotherDataDomainPage private (override val path: JsPath) extends QuestionPage[Choice] {
  override def toString: String = "addAnotherDataDomain"
}

object AddAnotherDataDomainPage {
  def apply(dataDomainsIndex: Int): AddAnotherDataDomainPage =
    new AddAnotherDataDomainPage(
      JsPath \ "dataDomains" \ dataDomainsIndex
    )

  def unapply(page: AddAnotherDataDomainPage): Option[Int] =
    page.path.path match {
      case KeyPathNode("dataDomains")
        :: IdxPathNode(dataDomainsIndex)
        :: Nil => Some(dataDomainsIndex)
      case _ => None
    }
}
