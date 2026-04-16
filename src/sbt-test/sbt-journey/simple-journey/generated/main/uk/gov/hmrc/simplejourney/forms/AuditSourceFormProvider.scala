package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.{mapping,optional,set}
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings


trait AuditSourceBaseFormProvider {
  def apply(): Form[String]
}

class DefaultAuditSourceFormProvider
  extends AuditSourceBaseFormProvider
  with Mappings {

  def apply(): Form[String] = Form(
    "value" -> text("auditSource.error.required")
  )
}
