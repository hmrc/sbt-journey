package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.*
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings


trait AuditProviderBaseFormProvider {
  def apply(): Form[String]
}

class DefaultAuditProviderFormProvider
  extends AuditProviderBaseFormProvider
  with Mappings {

  def apply(): Form[String] = Form(
    "value" -> text("auditProvider.error.required")
  )
}
