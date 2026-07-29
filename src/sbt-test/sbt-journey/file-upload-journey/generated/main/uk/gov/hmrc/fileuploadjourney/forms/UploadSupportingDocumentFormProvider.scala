package uk.gov.hmrc.fileuploadjourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.fileuploadjourney.forms.mappings.Mappings
import uk.gov.hmrc.fileuploadjourney.models.UploadId

trait UploadSupportingDocumentBaseFormProvider {
  def apply(): Form[UploadId]
}

class DefaultUploadSupportingDocumentFormProvider
  extends UploadSupportingDocumentBaseFormProvider
  with Mappings {

  def apply(): Form[UploadId] = Form(
    "value" -> uuid.transform(UploadId.apply, _.id)
  )
}
