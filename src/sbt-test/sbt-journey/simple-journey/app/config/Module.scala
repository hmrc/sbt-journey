package config

import com.google.inject.AbstractModule
import controllers.CheckYourAnswersController
import controllers.actions.*
import uk.gov.hmrc.simplejourney.controllers.*
import uk.gov.hmrc.simplejourney.forms.*

import java.time.{Clock, ZoneOffset}

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()

    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))

    bind(classOf[CheckYourAnswersBaseController]).to(classOf[CheckYourAnswersController])

    bind(classOf[AddAnotherAuditEventBaseFormProvider]).to(classOf[DefaultAddAnotherAuditEventFormProvider])
    bind(classOf[AddAnotherAuditSourceBaseFormProvider]).to(classOf[DefaultAddAnotherAuditSourceFormProvider])
    // bind(classOf[AddAnotherCipAssessmentTicketBaseFormProvider]).to(classOf[DefaultAddAnotherCipAssessmentTicketFormProvider])
    bind(classOf[AddAnotherDataDomainBaseFormProvider]).to(classOf[DefaultAddAnotherDataDomainFormProvider])
    bind(classOf[AddAnotherTaxRegimeBaseFormProvider]).to(classOf[DefaultAddAnotherTaxRegimeFormProvider])
    bind(classOf[AddATaxRegimeBaseFormProvider]).to(classOf[DefaultAddATaxRegimeFormProvider])
    bind(classOf[AuditEventBaseFormProvider]).to(classOf[DefaultAuditEventFormProvider])
    bind(classOf[AuditProviderBaseFormProvider]).to(classOf[DefaultAuditProviderFormProvider])
    bind(classOf[AuditSourceBaseFormProvider]).to(classOf[DefaultAuditSourceFormProvider])
    bind(classOf[CipAssessmentPageBaseFormProvider]).to(classOf[DefaultCipAssessmentPageFormProvider])
    bind(classOf[CipAssessmentTicketBaseFormProvider]).to(classOf[DefaultCipAssessmentTicketFormProvider])
    bind(classOf[DataDomainBaseFormProvider]).to(classOf[DefaultDataDomainFormProvider])
    bind(classOf[SaInfoBaseFormProvider]).to(classOf[DefaultSaInfoFormProvider])
    bind(classOf[ServiceDescriptionBaseFormProvider]).to(classOf[DefaultServiceDescriptionFormProvider])
    bind(classOf[ServiceNameBaseFormProvider]).to(classOf[DefaultServiceNameFormProvider])
    bind(classOf[TaxRegimeBaseFormProvider]).to(classOf[DefaultTaxRegimeFormProvider])
    bind(classOf[VatInfoBaseFormProvider]).to(classOf[DefaultVatInfoFormProvider])
    bind(classOf[WhichTaxRegimeBaseFormProvider]).to(classOf[DefaultWhichTaxRegimeFormProvider])
  }
}
