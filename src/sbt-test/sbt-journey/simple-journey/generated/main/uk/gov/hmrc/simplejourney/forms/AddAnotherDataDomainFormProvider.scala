package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.{mapping,optional}
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.Choice

trait AddAnotherDataDomainBaseFormProvider {
  def apply(): Form[Choice]
}

class DefaultAddAnotherDataDomainFormProvider
  extends AddAnotherDataDomainBaseFormProvider
  with Mappings {

  def apply(): Form[Choice] = Form(
    "value" -> enumerable[Choice](
      requiredKey = "addAnotherDataDomain.error.required",
      invalidKey = "addAnotherDataDomain.error.invalid",
    )
  )
}
