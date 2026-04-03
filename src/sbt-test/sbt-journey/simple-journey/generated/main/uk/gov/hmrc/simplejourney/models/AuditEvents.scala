package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{Json, JsPath, Reads}
import play.api.libs.functional.syntax.*


case class AuditEvents(
  auditEvent: AuditEvent
)

object AuditEvents {
  given auditEventsReads: Reads[AuditEvents] =
    (JsPath \ "auditEvent").read[AuditEvent].map(AuditEvents.apply)
}
