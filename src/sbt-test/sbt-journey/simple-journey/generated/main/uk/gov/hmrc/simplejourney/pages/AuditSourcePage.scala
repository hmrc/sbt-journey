package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{IdxPathNode,JsPath,KeyPathNode}

case class AuditSourcePage private (override val path: JsPath) extends QuestionPage[String] {
  override def toString: String = "auditSource"
}

object AuditSourcePage {
  def apply(auditSourcesIndex: Int): AuditSourcePage =
    new AuditSourcePage(
      JsPath \ "auditSources" \ auditSourcesIndex \ "auditSource"
    )

  def unapply(page: AuditSourcePage): Option[Int] =
    page.path.path match {
      case KeyPathNode("auditSources")
        :: IdxPathNode(auditSourcesIndex)
        :: KeyPathNode("auditSource")
        :: Nil => Some(auditSourcesIndex)
      case _ => None
    }
}
