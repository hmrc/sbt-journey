package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{Json, Format}
import java.time.LocalDate
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

case class AuditEvent(
  auditType: String,
  description: String,
  expectedGoLiveDate: LocalDate,
  expectedDecommissioningDate: Option[LocalDate]
)

object AuditEvent extends MongoJavatimeFormats.Implicits {
  given Format[AuditEvent] = Json.format[AuditEvent]
}
