package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{Json, Reads}


case class AuditSources(
  auditSource: String,
  auditEvents: List[AuditEvent]
)

object AuditSources {
  given Reads[AuditSources] = Json.reads[AuditSources]
}
