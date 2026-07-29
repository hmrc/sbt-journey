package uk.gov.hmrc.fileuploadjourney.models.upscan

import play.api.libs.json.{Json, OFormat}

case class FailureDetails(
  failureReason: String,
  message: String
)

object FailureDetails {
  given format: OFormat[FailureDetails] = Json.format[FailureDetails]
}
