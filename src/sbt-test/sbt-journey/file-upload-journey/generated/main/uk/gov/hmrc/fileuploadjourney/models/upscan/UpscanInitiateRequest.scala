package uk.gov.hmrc.fileuploadjourney.models.upscan

import play.api.libs.json.{Json, OWrites}

case class UpscanInitiateRequest(
  callbackUrl: String,
  successRedirect: Option[String],
  errorRedirect: Option[String],
  minimumFileSize: Option[Long] = None,
  maximumFileSize: Option[Long] = None
)

object UpscanInitiateRequest {
  given OWrites[UpscanInitiateRequest] = Json.writes[UpscanInitiateRequest]
}
