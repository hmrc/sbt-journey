package uk.gov.hmrc.simplejourney.forms

import play.api.data.Form
import play.api.data.Forms.{mapping,optional,set}
import _root_.forms.mappings.Mappings // uk.gov.hmrc.simplejourney.forms.mappings.Mappings
import uk.gov.hmrc.simplejourney.models.AuditEvent
import play.api.i18n.Messages

trait AuditEventBaseFormProvider {
  def apply()(using messages: Messages): Form[AuditEvent]
}

class DefaultAuditEventFormProvider
  extends AuditEventBaseFormProvider
  with Mappings {

  def apply()(using messages: Messages): Form[AuditEvent] = Form(
    "value" -> mapping(
      "auditType" -> text("auditEvent.error.auditType.required"),
      "description" -> text("auditEvent.error.description.required"),
      "expectedGoLiveDate" -> localDate(
        invalidKey = "auditEvent.error.expectedGoLiveDate.invalid",
        allRequiredKey = "auditEvent.error.expectedGoLiveDate.required.all",
        twoRequiredKey = "auditEvent.error.expectedGoLiveDate.required.two",
        requiredKey = "auditEvent.error.expectedGoLiveDate.required",
      ),
      "expectedDecommissioningDate" -> optional(localDate(
        invalidKey = "auditEvent.error.expectedDecommissioningDate.invalid",
        allRequiredKey = "auditEvent.error.expectedDecommissioningDate.required.all",
        twoRequiredKey = "auditEvent.error.expectedDecommissioningDate.required.two",
        requiredKey = "auditEvent.error.expectedDecommissioningDate.required",
      ))
    )(AuditEvent.apply)(o => Some(Tuple.fromProductTyped(o)))
  )
}
