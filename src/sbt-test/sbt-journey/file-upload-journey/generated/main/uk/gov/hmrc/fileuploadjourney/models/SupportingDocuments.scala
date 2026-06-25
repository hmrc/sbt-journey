package uk.gov.hmrc.fileuploadjourney.models

import play.api.libs.json.{JsPath,Reads}
import play.api.libs.functional.syntax.*

case class SupportingDocuments(
  uploadSupportingDocument: UploadId,
  isDocumentConfidential: Boolean
)

object SupportingDocuments {
  given supportingDocumentsReads: Reads[SupportingDocuments] = {
    (
      (JsPath \ "uploadSupportingDocument").read[UploadId] and
      (JsPath \ "isDocumentConfidential").read[Boolean]
    )(SupportingDocuments.apply)
  }
}
