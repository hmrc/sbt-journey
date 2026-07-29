package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{IdxPathNode,JsPath,KeyPathNode}
import uk.gov.hmrc.simplejourney.models.Choice

case class AddAnotherAuditSourcePage private (override val path: JsPath) extends QuestionPage[Choice] {
  override def toString: String = "addAnotherAuditSource"
}

object AddAnotherAuditSourcePage {
  def apply(auditSourcesIndex: Int): AddAnotherAuditSourcePage =
    new AddAnotherAuditSourcePage(
      JsPath \ "auditSources" \ auditSourcesIndex
    )

  def unapply(page: AddAnotherAuditSourcePage): Option[Int] =
    page.path.path match {
      case KeyPathNode("auditSources")
        :: IdxPathNode(auditSourcesIndex)
        :: Nil => Some(auditSourcesIndex)
      case _ => None
    }
}
