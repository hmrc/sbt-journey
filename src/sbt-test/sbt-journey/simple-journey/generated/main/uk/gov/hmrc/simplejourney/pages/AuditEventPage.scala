package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import uk.gov.hmrc.simplejourney.models.AuditEvent

case class AuditEventPage private (override val path: JsPath) extends QuestionPage[AuditEvent] {
  override def toString: String = "auditEvent"
}

object AuditEventPage {
  def apply(auditSourcesIndex: Int, auditEventsIndex: Int): AuditEventPage =
    new AuditEventPage(
      JsPath \ "auditSources" \ auditSourcesIndex \ "auditEvents" \ auditEventsIndex \ "auditEvent"
    )

  def unapply(page: AuditEventPage): Option[(Int, Int)] =
    page.path.path match {
      case KeyPathNode("auditSources")
        :: IdxPathNode(auditSourcesIndex)
        :: KeyPathNode("auditEvents")
        :: IdxPathNode(auditEventsIndex)
        :: KeyPathNode("auditEvent")
        :: Nil => Some((auditSourcesIndex, auditEventsIndex))
      case _ => None
    }
}
