package uk.gov.hmrc.simplejourney.config

import com.google.inject.AbstractModule
import uk.gov.hmrc.simplejourney.forms.*

class DefaultFormProvidersModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AddAnotherDataDomainBaseFormProvider]).to(classOf[DefaultAddAnotherDataDomainFormProvider])
    bind(classOf[AuditEventBaseFormProvider]).to(classOf[DefaultAuditEventFormProvider])
    bind(classOf[ServiceNameBaseFormProvider]).to(classOf[DefaultServiceNameFormProvider])
    bind(classOf[AddAnotherAuditSourceBaseFormProvider]).to(classOf[DefaultAddAnotherAuditSourceFormProvider])
    bind(classOf[AuditSourceBaseFormProvider]).to(classOf[DefaultAuditSourceFormProvider])
    bind(classOf[DataDomainBaseFormProvider]).to(classOf[DefaultDataDomainFormProvider])
    bind(classOf[CipAssessmentPageBaseFormProvider]).to(classOf[DefaultCipAssessmentPageFormProvider])
    bind(classOf[TaxRegimeBaseFormProvider]).to(classOf[DefaultTaxRegimeFormProvider])
    bind(classOf[AuditProviderBaseFormProvider]).to(classOf[DefaultAuditProviderFormProvider])
    bind(classOf[ServiceDescriptionBaseFormProvider]).to(classOf[DefaultServiceDescriptionFormProvider])
    bind(classOf[AddATaxRegimeBaseFormProvider]).to(classOf[DefaultAddATaxRegimeFormProvider])
    bind(classOf[AddAnotherAuditEventBaseFormProvider]).to(classOf[DefaultAddAnotherAuditEventFormProvider])
    bind(classOf[CipAssessmentTicketBaseFormProvider]).to(classOf[DefaultCipAssessmentTicketFormProvider])
  }
}
