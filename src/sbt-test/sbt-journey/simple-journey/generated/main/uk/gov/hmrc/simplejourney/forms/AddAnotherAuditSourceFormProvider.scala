package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.{mapping,optional}
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.Choice

trait AddAnotherAuditSourceBaseFormProvider {
  def apply(): Form[Choice]
}

class DefaultAddAnotherAuditSourceFormProvider
  extends AddAnotherAuditSourceBaseFormProvider
  with Mappings {

  def apply(): Form[Choice] = Form(
    "value" -> enumerable[Choice](
      requiredKey = "addAnotherAuditSource.error.required",
      invalidKey = "addAnotherAuditSource.error.invalid",
    )
  )
}
