package uk.gov.hmrc.fileuploadjourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.fileuploadjourney.forms.mappings.Mappings
import uk.gov.hmrc.fileuploadjourney.models.Choice

trait AddAnotherSupportingDocumentBaseFormProvider {
  def apply(): Form[Choice]
}

class DefaultAddAnotherSupportingDocumentFormProvider
  extends AddAnotherSupportingDocumentBaseFormProvider
  with Mappings {

  def apply(): Form[Choice] = Form(
    "value" -> enumerable[Choice](
      requiredKey = "addAnotherSupportingDocument.error.required",
      invalidKey = "addAnotherSupportingDocument.error.invalid",
    )
  )
}
