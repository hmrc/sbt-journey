package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.{mapping,optional,set}
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.Choice

trait AddATaxRegimeBaseFormProvider {
  def apply(): Form[Choice]
}

class DefaultAddATaxRegimeFormProvider
  extends AddATaxRegimeBaseFormProvider
  with Mappings {

  def apply(): Form[Choice] = Form(
    "value" -> enumerable[Choice](
      requiredKey = "addATaxRegime.error.required",
      invalidKey = "addATaxRegime.error.invalid",
    )
  )
}
