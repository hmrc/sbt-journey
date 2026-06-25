package uk.gov.hmrc.fileuploadjourney.models.upscan

import java.net.URI
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.fileuploadjourney.models.EnumFormats

enum UpscanNotification {
  def reference: UpscanReference
  case Ready(reference: UpscanReference, downloadUrl: URI, uploadDetails: UploadDetails)
  case Failed(reference: UpscanReference, failureDetails: FailureDetails)
}

object UpscanNotification extends EnumFormats {
  private val readyReads: Reads[Ready] = Json.reads[Ready]
  private val failedReads: Reads[Failed] = Json.reads[Failed]

  given Reads[UpscanNotification] = discriminatedReads(
    "fileStatus",
    "READY" -> readyReads,
    "FAILED" -> failedReads
  )
}
