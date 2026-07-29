package uk.gov.hmrc.fileuploadjourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.fileuploadjourney.forms.mappings.Mappings


trait ProvideGoodsDescriptionBaseFormProvider {
  def apply(): Form[String]
}

class DefaultProvideGoodsDescriptionFormProvider
  extends ProvideGoodsDescriptionBaseFormProvider
  with Mappings {

  def apply(): Form[String] = Form(
    "value" -> text("provideGoodsDescription.error.required")
  )
}
