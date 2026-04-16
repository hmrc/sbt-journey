package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes
import uk.gov.hmrc.simplejourney.models.AuditEvent

case class AuditEventPage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[AuditEvent] {
  override def submitRoute(mode: Mode): Call = makeRoute(mode)
  override def toString: String = "auditEvent"
}

object AuditEventPage {
  def apply(auditSourcesIndex: Int, auditEventsIndex: Int): AuditEventPage =
    new AuditEventPage(
      JsPath \ "auditSources" \ auditSourcesIndex \ "auditEvents" \ auditEventsIndex \ "auditEvent",
      mode => routes.AuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
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
