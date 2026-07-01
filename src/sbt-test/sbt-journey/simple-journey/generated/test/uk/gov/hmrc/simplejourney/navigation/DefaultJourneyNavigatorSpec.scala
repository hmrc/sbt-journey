package uk.gov.hmrc.simplejourney.navigation

import _root_.generators.Generators // uk.gov.hmrc.simplejourney.generators.Generators
import _root_.models.CheckMode // uk.gov.hmrc.simplejourney.models.CheckMode
import _root_.models.NormalMode // uk.gov.hmrc.simplejourney.models.NormalMode
import _root_.models.UserAnswers // uk.gov.hmrc.simplejourney.models.UserAnswers
import uk.gov.hmrc.simplejourney.controllers.routes
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.pages.*
import java.time.LocalDate
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DefaultJourneyNavigatorSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, Generators {
  private val navigator = new DefaultJourneyNavigator()
  private val userAnswers = UserAnswers("userId")

  "DefaultJourneyNavigator" should "navigate from CipAssessmentTicketPage to CipAssessmentPagePage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(CipAssessmentTicketPage, NormalMode, userAnswers, answer) shouldBe routes.CipAssessmentPageBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from CipAssessmentPagePage to ServiceNamePage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(CipAssessmentPagePage, NormalMode, userAnswers, answer) shouldBe routes.ServiceNameBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from ServiceNamePage to ServiceDescriptionPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ServiceNamePage, NormalMode, userAnswers, answer) shouldBe routes.ServiceDescriptionBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from ServiceDescriptionPage to DataDomainPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ServiceDescriptionPage, NormalMode, userAnswers, answer) shouldBe routes.DataDomainBaseController.onPageLoad(0, NormalMode)
  }

  it should "navigate from DataDomainPage(i) to AddAnotherDataDomainPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    val dataDomainsIndex = 0
    navigator.nextPage(DataDomainPage(dataDomainsIndex), NormalMode, userAnswers, answer) shouldBe routes.AddAnotherDataDomainBaseController.onPageLoad(dataDomainsIndex, NormalMode)
  }

  it should "navigate from AddAnotherDataDomainPage(i) to DataDomainPage at the next index when the user chooses Yes in normal mode" in {
    val dataDomainsIndex = 0
    navigator.nextPage(AddAnotherDataDomainPage(dataDomainsIndex), NormalMode, userAnswers, Choice.Yes) shouldBe routes.DataDomainBaseController.onPageLoad(dataDomainsIndex + 1, NormalMode)
  }

  it should "navigate from AddAnotherDataDomainPage(i) to AddATaxRegimePage when the user chooses No in normal mode" in {
    val dataDomainsIndex = 0
    navigator.nextPage(AddAnotherDataDomainPage(dataDomainsIndex), NormalMode, userAnswers, Choice.No) shouldBe routes.AddATaxRegimeBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from AddATaxRegimePage to TaxRegimePage when the user chooses Yes in normal mode" in {
    navigator.nextPage(AddATaxRegimePage, NormalMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
  }

  it should "navigate from AddATaxRegimePage to AuditProviderPage when the user chooses No in normal mode" in {
    navigator.nextPage(AddATaxRegimePage, NormalMode, userAnswers, Choice.No) shouldBe routes.AuditProviderBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from TaxRegimePage(Yes, i) to AddAnotherTaxRegimePage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
    val taxRegimesIndex = 0
    navigator.nextPage(TaxRegimePage(Choice.Yes, taxRegimesIndex), NormalMode, userAnswers, answer) shouldBe routes.AddAnotherTaxRegimeBaseController.onPageLoad(taxRegimesIndex, NormalMode)
  }

  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to TaxRegimePage at the next index when the user chooses Yes in normal mode" in {
    val taxRegimesIndex = 0
    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), NormalMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(taxRegimesIndex + 1, NormalMode)
  }

  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to AuditProviderPage when the user chooses No in normal mode" in {
    val taxRegimesIndex = 0
    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), NormalMode, userAnswers, Choice.No) shouldBe routes.AuditProviderBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from AuditProviderPage to AuditSourcePage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(AuditProviderPage, NormalMode, userAnswers, answer) shouldBe routes.AuditSourceBaseController.onPageLoad(0, NormalMode)
  }

  it should "navigate from AuditSourcePage(i) to AuditEventPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    val auditSourcesIndex = 0
    navigator.nextPage(AuditSourcePage(auditSourcesIndex), NormalMode, userAnswers, answer) shouldBe routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, 0, NormalMode)
  }

  it should "navigate from AuditEventPage(i, j) to AddAnotherAuditEventPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: AuditEvent) =>
    val auditSourcesIndex = 0
    val auditEventsIndex = 0
    navigator.nextPage(AuditEventPage(auditSourcesIndex, auditEventsIndex), NormalMode, userAnswers, answer) shouldBe routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, NormalMode)
  }

  it should "navigate from AddAnotherAuditEventPage(i, j) to AuditEventPage at the next index when the user chooses Yes in normal mode" in {
    val auditSourcesIndex = 0
    val auditEventsIndex = 0
    navigator.nextPage(AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex), NormalMode, userAnswers, Choice.Yes) shouldBe routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, NormalMode)
  }

  it should "navigate from AddAnotherAuditEventPage(i, j) to AddAnotherAuditSourcePage when the user chooses No in normal mode" in {
    val auditSourcesIndex = 0
    val auditEventsIndex = 0
    navigator.nextPage(AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex), NormalMode, userAnswers, Choice.No) shouldBe routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, NormalMode)
  }

  it should "navigate from AddAnotherAuditSourcePage(i) to AuditSourcePage at the next index when the user chooses Yes in normal mode" in {
    val auditSourcesIndex = 0
    navigator.nextPage(AddAnotherAuditSourcePage(auditSourcesIndex), NormalMode, userAnswers, Choice.Yes) shouldBe routes.AuditSourceBaseController.onPageLoad(auditSourcesIndex + 1, NormalMode)
  }

  it should "navigate from AddAnotherAuditSourcePage(i) to CheckYourAnswersPage when the user chooses No in normal mode" in {
    val auditSourcesIndex = 0
    navigator.nextPage(AddAnotherAuditSourcePage(auditSourcesIndex), NormalMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from CipAssessmentTicketPage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(CipAssessmentTicketPage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from CipAssessmentPagePage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(CipAssessmentPagePage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from ServiceNamePage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ServiceNamePage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from ServiceDescriptionPage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ServiceDescriptionPage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from DataDomainPage(i) to AddAnotherDataDomainPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    val dataDomainsIndex = 0
    navigator.nextPage(DataDomainPage(dataDomainsIndex), CheckMode, userAnswers, answer) shouldBe routes.AddAnotherDataDomainBaseController.onPageLoad(dataDomainsIndex, CheckMode)
  }

  it should "navigate from AddAnotherDataDomainPage(i) to DataDomainPage at the next index when the user chooses Yes in check mode" in {
    val dataDomainsIndex = 0
    navigator.nextPage(AddAnotherDataDomainPage(dataDomainsIndex), CheckMode, userAnswers, Choice.Yes) shouldBe routes.DataDomainBaseController.onPageLoad(dataDomainsIndex + 1, CheckMode)
  }

  it should "navigate from AddAnotherDataDomainPage(i) to CheckYourAnswersPage when the user chooses No in check mode" in {
    val dataDomainsIndex = 0
    navigator.nextPage(AddAnotherDataDomainPage(dataDomainsIndex), CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from AddATaxRegimePage to TaxRegimePage when the user chooses Yes in check mode" in {
    navigator.nextPage(AddATaxRegimePage, CheckMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(0, CheckMode)
  }

  it should "navigate from AddATaxRegimePage to CheckYourAnswersPage when the user chooses No in check mode" in {
    navigator.nextPage(AddATaxRegimePage, CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from TaxRegimePage(Yes, i) to AddAnotherTaxRegimePage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
    val taxRegimesIndex = 0
    navigator.nextPage(TaxRegimePage(Choice.Yes, taxRegimesIndex), CheckMode, userAnswers, answer) shouldBe routes.AddAnotherTaxRegimeBaseController.onPageLoad(taxRegimesIndex, CheckMode)
  }

  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to TaxRegimePage at the next index when the user chooses Yes in check mode" in {
    val taxRegimesIndex = 0
    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), CheckMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(taxRegimesIndex + 1, CheckMode)
  }

  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to CheckYourAnswersPage when the user chooses No in check mode" in {
    val taxRegimesIndex = 0
    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from AuditProviderPage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(AuditProviderPage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from AuditSourcePage(i) to AuditEventPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    val auditSourcesIndex = 0
    navigator.nextPage(AuditSourcePage(auditSourcesIndex), CheckMode, userAnswers, answer) shouldBe routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, 0, CheckMode)
  }

  it should "navigate from AuditEventPage(i, j) to AddAnotherAuditEventPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: AuditEvent) =>
    val auditSourcesIndex = 0
    val auditEventsIndex = 0
    navigator.nextPage(AuditEventPage(auditSourcesIndex, auditEventsIndex), CheckMode, userAnswers, answer) shouldBe routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, CheckMode)
  }

  it should "navigate from AddAnotherAuditEventPage(i, j) to AuditEventPage at the next index when the user chooses Yes in check mode" in {
    val auditSourcesIndex = 0
    val auditEventsIndex = 0
    navigator.nextPage(AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex), CheckMode, userAnswers, Choice.Yes) shouldBe routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, CheckMode)
  }

  it should "navigate from AddAnotherAuditEventPage(i, j) to AddAnotherAuditSourcePage when the user chooses No in check mode" in {
    val auditSourcesIndex = 0
    val auditEventsIndex = 0
    navigator.nextPage(AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex), CheckMode, userAnswers, Choice.No) shouldBe routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, CheckMode)
  }

  it should "navigate from AddAnotherAuditSourcePage(i) to AuditSourcePage at the next index when the user chooses Yes in check mode" in {
    val auditSourcesIndex = 0
    navigator.nextPage(AddAnotherAuditSourcePage(auditSourcesIndex), CheckMode, userAnswers, Choice.Yes) shouldBe routes.AuditSourceBaseController.onPageLoad(auditSourcesIndex + 1, CheckMode)
  }

  it should "navigate from AddAnotherAuditSourcePage(i) to CheckYourAnswersPage when the user chooses No in check mode" in {
    val auditSourcesIndex = 0
    navigator.nextPage(AddAnotherAuditSourcePage(auditSourcesIndex), CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }
}
