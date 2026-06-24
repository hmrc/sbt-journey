package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import uk.gov.hmrc.simplejourney.models.Choice

case class AddAnotherAuditEventPage private (override val path: JsPath) extends QuestionPage[Choice] {
  override def toString: String = "addAnotherAuditEvent"
}

object AddAnotherAuditEventPage {
  def apply(auditSourcesIndex: Int, auditEventsIndex: Int): AddAnotherAuditEventPage =
    new AddAnotherAuditEventPage(
      JsPath \ "auditSources" \ auditSourcesIndex \ "auditEvents" \ auditEventsIndex
    )

  def unapply(page: AddAnotherAuditEventPage): Option[(Int, Int)] =
    page.path.path match {
      case KeyPathNode("auditSources")
        :: IdxPathNode(auditSourcesIndex)
        :: KeyPathNode("auditEvents")
        :: IdxPathNode(auditEventsIndex)
        :: Nil => Some((auditSourcesIndex, auditEventsIndex))
      case _ => None
    }
}
