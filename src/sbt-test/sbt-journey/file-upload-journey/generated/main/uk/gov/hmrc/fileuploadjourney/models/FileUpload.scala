package uk.gov.hmrc.fileuploadjourney.models

import play.api.libs.json.{Json, JsonConfiguration, OFormat, OWrites, Reads}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.fileuploadjourney.models.upscan.{UpscanReference, UploadDetails, FailureDetails}

import java.net.URI
import java.time.Instant

enum FileUpload {
  def id: UploadId
  def userId: String
  def reference: UpscanReference
  case Initiated(id: UploadId, userId: String, reference: UpscanReference, initiatedAt: Instant)
  case Processing(id: UploadId, userId: String, reference: UpscanReference, updatedAt: Instant)
  case Failed(id: UploadId, userId: String, reference: UpscanReference, failureDetails: FailureDetails, updatedAt: Instant)
  case Ready(id: UploadId, userId: String, reference: UpscanReference, downloadUrl: URI, uploadDetails: UploadDetails, updatedAt: Instant)
}

object FileUpload extends EnumFormats, UploadId.MongoFormat, MongoJavatimeFormats.Implicits {
  private val initiatedFormat: OFormat[Initiated] = Json.format[Initiated]
  private val processingFormat: OFormat[Processing] = Json.format[Processing]
  private val failedFormat: OFormat[Failed] = Json.format[Failed]
  private val readyFormat: OFormat[Ready] = Json.format[Ready]

  given reads: Reads[FileUpload] = discriminatedReads(
    "uploadStatus",
    "Initiated" -> initiatedFormat,
    "Processing" -> processingFormat,
    "Failed" -> failedFormat,
    "Ready" -> readyFormat
  )

  given writes(using config: JsonConfiguration): OWrites[FileUpload] = OWrites {
    case initiated: Initiated =>
      Json.obj("uploadStatus" -> "Initiated") ++ initiatedFormat.writes(initiated)
    case processing: Processing =>
      Json.obj("uploadStatus" -> "Processing") ++ processingFormat.writes(processing)
    case failed: Failed =>
      Json.obj("uploadStatus" -> "Failed") ++ failedFormat.writes(failed)
    case ready: Ready =>
      Json.obj("uploadStatus" -> "Ready") ++ readyFormat.writes(ready)
  }

  given format: OFormat[FileUpload] = OFormat(reads, writes)
}
