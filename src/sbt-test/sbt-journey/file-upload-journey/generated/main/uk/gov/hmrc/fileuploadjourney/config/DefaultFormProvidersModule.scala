package uk.gov.hmrc.fileuploadjourney.config

import com.google.inject.AbstractModule
import uk.gov.hmrc.fileuploadjourney.forms.*

class DefaultFormProvidersModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AddAnotherSupportingDocumentBaseFormProvider]).to(classOf[DefaultAddAnotherSupportingDocumentFormProvider])
    bind(classOf[ProvideConfidentialInformationBaseFormProvider]).to(classOf[DefaultProvideConfidentialInformationFormProvider])
    bind(classOf[ProvideGoodsNameBaseFormProvider]).to(classOf[DefaultProvideGoodsNameFormProvider])
    bind(classOf[AddSupportingDocumentsBaseFormProvider]).to(classOf[DefaultAddSupportingDocumentsFormProvider])
    bind(classOf[IsDocumentConfidentialBaseFormProvider]).to(classOf[DefaultIsDocumentConfidentialFormProvider])
    bind(classOf[ProvideGoodsDescriptionBaseFormProvider]).to(classOf[DefaultProvideGoodsDescriptionFormProvider])
    bind(classOf[AddConfidentialInformationBaseFormProvider]).to(classOf[DefaultAddConfidentialInformationFormProvider])
    bind(classOf[UploadSupportingDocumentBaseFormProvider]).to(classOf[DefaultUploadSupportingDocumentFormProvider])
  }
}
