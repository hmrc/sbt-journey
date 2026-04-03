package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{Json, JsPath, Reads}
import play.api.libs.functional.syntax.*


case class AuditSources(
  auditSource: String,
  auditEvents: List[AuditEvent]
)

object AuditSources {
  given auditSourcesReads: Reads[AuditSources] = {
    val auditEvents = Reads.list(Reads.at[AuditEvent](JsPath \ "auditEvent"))
    (
      (JsPath \ "auditSource").read[String] and
      (JsPath \ "auditEvents").read[List[AuditEvent]](using auditEvents)
    )(AuditSources.apply)
  }
}
