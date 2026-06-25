package uk.gov.hmrc.fileuploadjourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.fileuploadjourney.forms.mappings.Mappings


trait IsDocumentConfidentialBaseFormProvider {
  def apply(): Form[Boolean]
}

class DefaultIsDocumentConfidentialFormProvider
  extends IsDocumentConfidentialBaseFormProvider
  with Mappings {

  def apply(): Form[Boolean] = Form(
    "value" -> boolean(
      requiredKey = "isDocumentConfidential.error.required",
      invalidKey = "isDocumentConfidential.error.boolean",
    )
  )
}
