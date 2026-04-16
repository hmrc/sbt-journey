package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.{mapping,optional,set}
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings


trait CipAssessmentPageBaseFormProvider {
  def apply(): Form[String]
}

class DefaultCipAssessmentPageFormProvider
  extends CipAssessmentPageBaseFormProvider
  with Mappings {

  def apply(): Form[String] = Form(
    "value" -> text("cipAssessmentPage.error.required")
  )
}
