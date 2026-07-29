package uk.gov.hmrc.fileuploadjourney.models.upscan

import java.time.Instant
import play.api.libs.json.{Json, OFormat}

case class UploadDetails(
  fileName: String,
  fileMimeType: String,
  uploadTimestamp: Instant,
  checksum: String,
  size: Long
)

object UploadDetails {
  given format: OFormat[UploadDetails] = Json.format[UploadDetails]
}
