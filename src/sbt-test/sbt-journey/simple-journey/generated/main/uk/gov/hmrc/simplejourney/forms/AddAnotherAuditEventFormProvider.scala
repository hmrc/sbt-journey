package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.Choice

trait AddAnotherAuditEventBaseFormProvider {
  def apply(): Form[Choice]
}

class DefaultAddAnotherAuditEventFormProvider
  extends AddAnotherAuditEventBaseFormProvider
  with Mappings {

  def apply(): Form[Choice] = Form(
    "value" -> enumerable[Choice](
      requiredKey = "addAnotherAuditEvent.error.required",
      invalidKey = "addAnotherAuditEvent.error.invalid",
    )
  )
}
