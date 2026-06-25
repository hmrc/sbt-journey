package uk.gov.hmrc.fileuploadjourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.fileuploadjourney.forms.mappings.Mappings


trait ProvideGoodsNameBaseFormProvider {
  def apply(): Form[String]
}

class DefaultProvideGoodsNameFormProvider
  extends ProvideGoodsNameBaseFormProvider
  with Mappings {

  def apply(): Form[String] = Form(
    "value" -> text("provideGoodsName.error.required")
  )
}
