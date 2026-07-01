package uk.gov.hmrc.fileuploadjourney.navigation

import _root_.generators.Generators // uk.gov.hmrc.fileuploadjourney.generators.Generators
import _root_.models.CheckMode // uk.gov.hmrc.fileuploadjourney.models.CheckMode
import _root_.models.NormalMode // uk.gov.hmrc.fileuploadjourney.models.NormalMode
import _root_.models.UserAnswers // uk.gov.hmrc.fileuploadjourney.models.UserAnswers
import uk.gov.hmrc.fileuploadjourney.controllers.routes
import uk.gov.hmrc.fileuploadjourney.models.*
import uk.gov.hmrc.fileuploadjourney.pages.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DefaultJourneyNavigatorSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, Generators {
  private val navigator = new DefaultJourneyNavigator()
  private val userAnswers = UserAnswers("userId")

  "DefaultJourneyNavigator" should "navigate from ProvideGoodsNamePage to ProvideGoodsDescriptionPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ProvideGoodsNamePage, NormalMode, userAnswers, answer) shouldBe routes.ProvideGoodsDescriptionBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from ProvideGoodsDescriptionPage to AddConfidentialInformationPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ProvideGoodsDescriptionPage, NormalMode, userAnswers, answer) shouldBe routes.AddConfidentialInformationBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from AddConfidentialInformationPage to ProvideConfidentialInformationPage when the user chooses Yes in normal mode" in {
    navigator.nextPage(AddConfidentialInformationPage, NormalMode, userAnswers, Choice.Yes) shouldBe routes.ProvideConfidentialInformationBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from AddConfidentialInformationPage to AddSupportingDocumentsPage when the user chooses No in normal mode" in {
    navigator.nextPage(AddConfidentialInformationPage, NormalMode, userAnswers, Choice.No) shouldBe routes.AddSupportingDocumentsBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from ProvideConfidentialInformationPage(Yes) to AddSupportingDocumentsPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ProvideConfidentialInformationPage(Choice.Yes), NormalMode, userAnswers, answer) shouldBe routes.AddSupportingDocumentsBaseController.onPageLoad(NormalMode)
  }

  it should "navigate from AddSupportingDocumentsPage to UploadSupportingDocumentPage when the user chooses Yes in normal mode" in {
    navigator.nextPage(AddSupportingDocumentsPage, NormalMode, userAnswers, Choice.Yes) shouldBe routes.UploadSupportingDocumentBaseController.onPageLoad(0, NormalMode)
  }

  it should "navigate from AddSupportingDocumentsPage to CheckYourAnswersPage when the user chooses No in normal mode" in {
    navigator.nextPage(AddSupportingDocumentsPage, NormalMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from UploadSupportingDocumentPage(Yes, i) to IsDocumentConfidentialPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: UploadId) =>
    val supportingDocumentsIndex = 0
    navigator.nextPage(UploadSupportingDocumentPage(Choice.Yes, supportingDocumentsIndex), NormalMode, userAnswers, answer) shouldBe routes.IsDocumentConfidentialBaseController.onPageLoad(supportingDocumentsIndex, NormalMode)
  }

  it should "navigate from IsDocumentConfidentialPage(Yes, i) to AddAnotherSupportingDocumentPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: Boolean) =>
    val supportingDocumentsIndex = 0
    navigator.nextPage(IsDocumentConfidentialPage(Choice.Yes, supportingDocumentsIndex), NormalMode, userAnswers, answer) shouldBe routes.AddAnotherSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex, NormalMode)
  }

  it should "navigate from AddAnotherSupportingDocumentPage(Yes, i) to UploadSupportingDocumentPage at the next index when the user chooses Yes in normal mode" in {
    val supportingDocumentsIndex = 0
    navigator.nextPage(AddAnotherSupportingDocumentPage(Choice.Yes, supportingDocumentsIndex), NormalMode, userAnswers, Choice.Yes) shouldBe routes.UploadSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex + 1, NormalMode)
  }

  it should "navigate from AddAnotherSupportingDocumentPage(Yes, i) to CheckYourAnswersPage when the user chooses No in normal mode" in {
    val supportingDocumentsIndex = 0
    navigator.nextPage(AddAnotherSupportingDocumentPage(Choice.Yes, supportingDocumentsIndex), NormalMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from ProvideGoodsNamePage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ProvideGoodsNamePage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from ProvideGoodsDescriptionPage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ProvideGoodsDescriptionPage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from AddConfidentialInformationPage to ProvideConfidentialInformationPage when the user chooses Yes in check mode" in {
    navigator.nextPage(AddConfidentialInformationPage, CheckMode, userAnswers, Choice.Yes) shouldBe routes.ProvideConfidentialInformationBaseController.onPageLoad(CheckMode)
  }

  it should "navigate from AddConfidentialInformationPage to CheckYourAnswersPage when the user chooses No in check mode" in {
    navigator.nextPage(AddConfidentialInformationPage, CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from ProvideConfidentialInformationPage(Yes) to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
    navigator.nextPage(ProvideConfidentialInformationPage(Choice.Yes), CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from AddSupportingDocumentsPage to UploadSupportingDocumentPage when the user chooses Yes in check mode" in {
    navigator.nextPage(AddSupportingDocumentsPage, CheckMode, userAnswers, Choice.Yes) shouldBe routes.UploadSupportingDocumentBaseController.onPageLoad(0, CheckMode)
  }

  it should "navigate from AddSupportingDocumentsPage to CheckYourAnswersPage when the user chooses No in check mode" in {
    navigator.nextPage(AddSupportingDocumentsPage, CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }

  it should "navigate from UploadSupportingDocumentPage(Yes, i) to IsDocumentConfidentialPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: UploadId) =>
    val supportingDocumentsIndex = 0
    navigator.nextPage(UploadSupportingDocumentPage(Choice.Yes, supportingDocumentsIndex), CheckMode, userAnswers, answer) shouldBe routes.IsDocumentConfidentialBaseController.onPageLoad(supportingDocumentsIndex, CheckMode)
  }

  it should "navigate from IsDocumentConfidentialPage(Yes, i) to AddAnotherSupportingDocumentPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: Boolean) =>
    val supportingDocumentsIndex = 0
    navigator.nextPage(IsDocumentConfidentialPage(Choice.Yes, supportingDocumentsIndex), CheckMode, userAnswers, answer) shouldBe routes.AddAnotherSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex, CheckMode)
  }

  it should "navigate from AddAnotherSupportingDocumentPage(Yes, i) to UploadSupportingDocumentPage at the next index when the user chooses Yes in check mode" in {
    val supportingDocumentsIndex = 0
    navigator.nextPage(AddAnotherSupportingDocumentPage(Choice.Yes, supportingDocumentsIndex), CheckMode, userAnswers, Choice.Yes) shouldBe routes.UploadSupportingDocumentBaseController.onPageLoad(supportingDocumentsIndex + 1, CheckMode)
  }

  it should "navigate from AddAnotherSupportingDocumentPage(Yes, i) to CheckYourAnswersPage when the user chooses No in check mode" in {
    val supportingDocumentsIndex = 0
    navigator.nextPage(AddAnotherSupportingDocumentPage(Choice.Yes, supportingDocumentsIndex), CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
  }
}
