package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{JsPath,Reads}

case class AuditEvents(
  auditEvent: AuditEvent
)

object AuditEvents {
  given auditEventsReads: Reads[AuditEvents] =
    (JsPath \ "auditEvent").read[AuditEvent].map(AuditEvents.apply)
}
