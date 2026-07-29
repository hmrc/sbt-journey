package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.TaxRegime

trait TaxRegimeBaseFormProvider {
  def apply(): Form[Set[TaxRegime]]
}

class DefaultTaxRegimeFormProvider
  extends TaxRegimeBaseFormProvider
  with Mappings {

  def apply(): Form[Set[TaxRegime]] = Form(
    "value" -> set(enumerable[TaxRegime](
      requiredKey = "taxRegime.error.required",
      invalidKey = "taxRegime.error.invalid",
    ))
  )
}
