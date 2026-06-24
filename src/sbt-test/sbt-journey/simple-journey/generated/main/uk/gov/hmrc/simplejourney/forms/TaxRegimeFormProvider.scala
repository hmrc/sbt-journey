package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.TaxRegime

trait TaxRegimeBaseFormProvider {
  def apply(): Form[TaxRegime]
}

class DefaultTaxRegimeFormProvider
  extends TaxRegimeBaseFormProvider
  with Mappings {

  def apply(): Form[TaxRegime] = Form(
    "value" -> enumerable[TaxRegime](
      requiredKey = "taxRegime.error.required",
      invalidKey = "taxRegime.error.invalid",
    )
  )
}
