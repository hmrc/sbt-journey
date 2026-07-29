package uk.gov.hmrc.fileuploadjourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.fileuploadjourney.forms.mappings.Mappings
import uk.gov.hmrc.fileuploadjourney.models.Choice

trait AddSupportingDocumentsBaseFormProvider {
  def apply(): Form[Choice]
}

class DefaultAddSupportingDocumentsFormProvider
  extends AddSupportingDocumentsBaseFormProvider
  with Mappings {

  def apply(): Form[Choice] = Form(
    "value" -> enumerable[Choice](
      requiredKey = "addSupportingDocuments.error.required",
      invalidKey = "addSupportingDocuments.error.invalid",
    )
  )
}
