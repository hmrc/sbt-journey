package uk.gov.hmrc.simplejourney.navigation

import uk.gov.hmrc.simplejourney.controllers.routes
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.pages.*
import _root_.models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.models.CheckMode // uk.gov.hmrc.simplejourney.models.CheckMode
import _root_.models.NormalMode // uk.gov.hmrc.simplejourney.models.NormalMode
import _root_.models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import _root_.pages.Page // uk.gov.hmrc.simplejourney.pages.Page
import _root_.pages.QuestionPage // uk.gov.hmrc.simplejourney.pages.QuestionPage
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
    case CipAssessmentTicketPage => _ => _ =>
      routes.CipAssessmentPageBaseController.onPageLoad(NormalMode)
    case CipAssessmentPagePage => _ => _ =>
      routes.ServiceNameBaseController.onPageLoad(NormalMode)
    case ServiceNamePage => _ => _ =>
      routes.ServiceDescriptionBaseController.onPageLoad(NormalMode)
    case ServiceDescriptionPage => _ => _ =>
      routes.DataDomainBaseController.onPageLoad(0, NormalMode)
    case AddAnotherDataDomainPage(dataDomainsIndex) => _ => {
      case Choice.Yes => routes.DataDomainBaseController.onPageLoad(dataDomainsIndex + 1, NormalMode)
      case Choice.No  => routes.AddATaxRegimeBaseController.onPageLoad(NormalMode)
    }
    case DataDomainPage(dataDomainsIndex) => _ => _ =>
      routes.AddAnotherDataDomainBaseController.onPageLoad(dataDomainsIndex, NormalMode)
    case AddATaxRegimePage => _ => {
      case Choice.Yes => routes.TaxRegimeBaseController.onPageLoad(NormalMode)
      case Choice.No  => routes.AuditProviderBaseController.onPageLoad(NormalMode)
    }
    case TaxRegimePage(Choice.Yes) => _ => _ =>
      routes.AuditProviderBaseController.onPageLoad(NormalMode)
    case AuditProviderPage => _ => _ =>
      routes.AuditSourceBaseController.onPageLoad(0, NormalMode)
    case AddAnotherAuditSourcePage(auditSourcesIndex) => _ => {
      case Choice.Yes => routes.AuditSourceBaseController.onPageLoad(auditSourcesIndex + 1, NormalMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case AuditSourcePage(auditSourcesIndex) => _ => _ =>
      routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, 0, NormalMode)
    case AddAnotherAuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => {
      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, NormalMode)
      case Choice.No  => routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, NormalMode)
    }
    case AuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => _ =>
      routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, NormalMode)
  }

  private val checkRoutes: (page: Page) => UserAnswers => page.AnswerType => Call = {
    case CipAssessmentTicketPage => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case CipAssessmentPagePage => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case ServiceNamePage => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case ServiceDescriptionPage => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case AddAnotherDataDomainPage(dataDomainsIndex) => _ => {
      case Choice.Yes => routes.DataDomainBaseController.onPageLoad(dataDomainsIndex + 1, CheckMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case DataDomainPage(dataDomainsIndex) => _ => _ =>
      routes.AddAnotherDataDomainBaseController.onPageLoad(dataDomainsIndex, CheckMode)
    case AddATaxRegimePage => _ => {
      case Choice.Yes => routes.TaxRegimeBaseController.onPageLoad(CheckMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case TaxRegimePage(Choice.Yes) => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case AuditProviderPage => _ => _ =>
      routes.CheckYourAnswersBaseController.onPageLoad
    case AddAnotherAuditSourcePage(auditSourcesIndex) => _ => {
      case Choice.Yes => routes.AuditSourceBaseController.onPageLoad(auditSourcesIndex + 1, CheckMode)
      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
    }
    case AuditSourcePage(auditSourcesIndex) => _ => _ =>
      routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, 0, CheckMode)
    case AddAnotherAuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => {
      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, CheckMode)
      case Choice.No  => routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, CheckMode)
    }
    case AuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => _ =>
      routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, CheckMode)
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, latestAnswer: page.AnswerType): Call =
    mode match {
      case NormalMode => normalRoutes(page)(userAnswers)(latestAnswer)
      case CheckMode  => checkRoutes(page)(userAnswers)(latestAnswer)
    }
}
