package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings


trait CipAssessmentTicketBaseFormProvider {
  def apply(): Form[String]
}

class DefaultCipAssessmentTicketFormProvider
  extends CipAssessmentTicketBaseFormProvider
  with Mappings {

  def apply(): Form[String] = Form(
    "value" -> text("cipAssessmentTicket.error.required")
  )
}
