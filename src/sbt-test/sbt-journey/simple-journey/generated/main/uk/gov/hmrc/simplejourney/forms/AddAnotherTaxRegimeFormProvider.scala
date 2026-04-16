package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.{mapping,optional,set}
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.Choice

trait AddAnotherTaxRegimeBaseFormProvider {
  def apply(): Form[Choice]
}

class DefaultAddAnotherTaxRegimeFormProvider
  extends AddAnotherTaxRegimeBaseFormProvider
  with Mappings {

  def apply(): Form[Choice] = Form(
    "value" -> enumerable[Choice](
      requiredKey = "addAnotherTaxRegime.error.required",
      invalidKey = "addAnotherTaxRegime.error.invalid",
    )
  )
}
