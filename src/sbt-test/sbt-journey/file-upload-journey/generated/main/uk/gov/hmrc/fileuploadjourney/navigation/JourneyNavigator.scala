package uk.gov.hmrc.fileuploadjourney.navigation

import uk.gov.hmrc.fileuploadjourney.controllers.routes
import uk.gov.hmrc.fileuploadjourney.models.*
import uk.gov.hmrc.fileuploadjourney.pages.*
import _root_.models.Mode // uk.gov.hmrc.fileuploadjourney.models.Mode
import _root_.models.CheckMode // uk.gov.hmrc.fileuploadjourney.models.CheckMode
import _root_.models.NormalMode // uk.gov.hmrc.fileuploadjourney.models.NormalMode
import _root_.models.UserAnswers // uk.gov.hmrc.fileuploadjourney.models.UserAnswers
import _root_.pages.Page // uk.gov.hmrc.fileuploadjourney.pages.Page
import _root_.pages.QuestionPage // uk.gov.hmrc.fileuploadjourney.pages.QuestionPage
import com.google.inject.ImplementedBy
import play.api.mvc.Call

import javax.inject.{Inject,Singleton}

@ImplementedBy(classOf[DefaultJourneyNavigator])
trait JourneyNavigator {
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, latestAnswer: page.AnswerType): Call
}

@Singleton
class DefaultJourneyNavigator @Inject() () extends JourneyNavigator {
  private val normalRoutes: (page: Page) => UserAnswers => page.AnswerType => Call = {
    case ProvideGoodsNamePage => _ => _ =>
      routes.ProvideGoodsDescriptionBaseController.onPageLoad(NormalMode)
    case ProvideGoodsDescriptionPage => _ => _ =>
      routes.AddConfidentialInformationBaseController.onPageLoad(NormalMode)
    case AddConfidentialInformationPage => _ => {
      case Choice.Yes => routes.ProvideConfidentialInformationBaseController.onPageLoad(NormalMode)
      case Choice.No  => routes.AddSupportingDocumentsBaseController.onPageLoad(NormalMode)
    }
    case ProvideConfidentialInformationPage(Choice.Yes) => _ => _ =>
      routes.AddSupportingDocumentsBaseController.onPageLoad(NormalMode)
    case AddSupportingDocumentsPage => _ => {
      case Choice.Yes => routes.UploadSupportingDocumentBaseController.onPageLoad(0, NormalMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case AddAnotherSupportingDocumentPage(Choice.Yes,supportingDocumentsIndex) => _ => {
      case Choice.Yes => routes.UploadSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex + 1, NormalMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case UploadSupportingDocumentPage(Choice.Yes,supportingDocumentsIndex) => _ => _ =>
      routes.IsDocumentConfidentialBaseController.onPageLoad(supportingDocumentsIndex, NormalMode)
    case IsDocumentConfidentialPage(Choice.Yes,supportingDocumentsIndex) => _ => _ =>
      routes.AddAnotherSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex, NormalMode)
  }

  private val checkRoutes: (page: Page) => UserAnswers => page.AnswerType => Call = {
    case ProvideGoodsNamePage => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case ProvideGoodsDescriptionPage => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case AddConfidentialInformationPage => _ => {
      case Choice.Yes => routes.ProvideConfidentialInformationBaseController.onPageLoad(CheckMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case ProvideConfidentialInformationPage(Choice.Yes) => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case AddSupportingDocumentsPage => _ => {
      case Choice.Yes => routes.UploadSupportingDocumentBaseController.onPageLoad(0, CheckMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case AddAnotherSupportingDocumentPage(Choice.Yes,supportingDocumentsIndex) => _ => {
      case Choice.Yes => routes.UploadSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex + 1, CheckMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case UploadSupportingDocumentPage(Choice.Yes,supportingDocumentsIndex) => _ => _ =>
      routes.IsDocumentConfidentialBaseController.onPageLoad(supportingDocumentsIndex, CheckMode)
    case IsDocumentConfidentialPage(Choice.Yes,supportingDocumentsIndex) => _ => _ =>
      routes.AddAnotherSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex, CheckMode)
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, latestAnswer: page.AnswerType): Call =
    mode match {
      case NormalMode => normalRoutes(page)(userAnswers)(latestAnswer)
      case CheckMode  => checkRoutes(page)(userAnswers)(latestAnswer)
    }
}
