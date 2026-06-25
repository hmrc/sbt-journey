package uk.gov.hmrc.fileuploadjourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.fileuploadjourney.forms.mappings.Mappings


trait ProvideConfidentialInformationBaseFormProvider {
  def apply(): Form[String]
}

class DefaultProvideConfidentialInformationFormProvider
  extends ProvideConfidentialInformationBaseFormProvider
  with Mappings {

  def apply(): Form[String] = Form(
    "value" -> text("provideConfidentialInformation.error.required")
  )
}
