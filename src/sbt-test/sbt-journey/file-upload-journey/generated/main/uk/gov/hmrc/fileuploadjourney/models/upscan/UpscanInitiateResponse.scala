package uk.gov.hmrc.fileuploadjourney.models.upscan

import play.api.libs.json.{Json, Reads}

case class UpscanInitiateResponse(
  reference: UpscanReference,
  uploadRequest: UpscanFormTemplate
)

object UpscanInitiateResponse {
  given Reads[UpscanInitiateResponse] = Json.reads[UpscanInitiateResponse]
}
