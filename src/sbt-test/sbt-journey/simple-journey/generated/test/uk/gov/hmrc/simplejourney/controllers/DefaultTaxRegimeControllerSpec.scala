package uk.gov.hmrc.simplejourney.controllers

import _root_.controllers.actions.* // uk.gov.hmrc.simplejourney.controllers.actions.*
import _root_.controllers.routes // uk.gov.hmrc.simplejourney.controllers.routes
import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.simplejourney.models.{NormalMode,UserAnswers}
import _root_.generators.Generators // uk.gov.hmrc.simplejourney.generators.Generators
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify,when}
import org.mongodb.scala.MongoException
import org.scalacheck.{Arbitrary,Gen}
import org.scalatest.{RecoverMethods,TryValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat.raw
import repositories.SessionRepository // uk.gov.hmrc.simplejourney.repositories.SessionRepository
import uk.gov.hmrc.simplejourney.controllers.routes as journeyRoutes
import uk.gov.hmrc.simplejourney.forms.TaxRegimeBaseFormProvider
import uk.gov.hmrc.simplejourney.forms.*
import uk.gov.hmrc.simplejourney.models.*
import uk.gov.hmrc.simplejourney.navigation.JourneyNavigator
import uk.gov.hmrc.simplejourney.pages.*
import views.html.TaxRegimeView

import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Success,Try}

class DefaultTaxRegimeControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
  given system: ActorSystem = ActorSystem("test")
  given ExecutionContext    = system.dispatcher
  given Messages            = stubMessages()

  private val defaultFormProvider = new DefaultTaxRegimeFormProvider()
  private val mockViewResponse    = "<html>Hello</html>"

  private def makeController(
    userAnswers: Option[UserAnswers],
    navigator: JourneyNavigator = mock[JourneyNavigator],
    repository: SessionRepository = mock[SessionRepository],
    formProvider: TaxRegimeBaseFormProvider = mock[TaxRegimeBaseFormProvider],
    view: TaxRegimeView = mock[TaxRegimeView]
  ) = new DefaultTaxRegimeController(
    new FakeIdentifierAction(stubPlayBodyParsers),
    new FakeDataRetrievalAction(userAnswers),
    new DataRequiredActionImpl(),
    navigator,
    repository,
    formProvider,
    view,
    stubMessagesControllerComponents()
  )

  "DefaultTaxRegimeController.onPageLoad(Yes)" should "return a 200 OK response containing the view HTML when everything is successful" in {
    val view = mock[TaxRegimeView]
    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
    val userAnswers = for {
      answers <- Success(UserAnswers("id"))
      addATaxRegime = Choice.Yes
      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
    } yield answers
    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
    val request = FakeRequest(route)
    val response = controller.onPageLoad(NormalMode)(request)
    status(response) shouldBe OK
    contentAsString(response) shouldBe mockViewResponse
    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
  }

  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: Set[TaxRegime]) =>
    val view = mock[TaxRegimeView]
    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
    val userAnswers = for {
      answers <- Success(UserAnswers("id"))
      addATaxRegime = Choice.Yes
      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
      answers <- answers.set(TaxRegimePage(addATaxRegime), answer)
    } yield answers
    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
    val request = FakeRequest(route)
    val response = controller.onPageLoad(NormalMode)(request)
    status(response) shouldBe OK
    contentAsString(response) shouldBe mockViewResponse
    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Set[TaxRegime]]])
    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
    formCaptor.getValue().value shouldBe Some(answer)
  }

  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
    val controller = makeController(userAnswers = None)
    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
    val request = FakeRequest(route)
    val response = controller.onPageLoad(NormalMode)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }

  it should "redirect to journey recovery when a prerequisite answer is missing" in {
    val userAnswers = Success(UserAnswers("id"))
    val controller = makeController(userAnswers = userAnswers.toOption)
    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
    val request = FakeRequest(route)
    val response = controller.onPageLoad(NormalMode)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }

  it should "throw exceptions to the top level error handler" in {
    val userAnswers = for {
      answers <- Success(UserAnswers("id"))
      addATaxRegime = Choice.Yes
      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
    } yield answers
    val repository = mock[SessionRepository]
    when(repository.get(any())).thenReturn(Future.failed(new MongoException("Error")))
    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
    val request = FakeRequest(route)
    recoverToSucceededIf[MongoException] {
      controller.onPageLoad(NormalMode)(request)
    }
  }

  "DefaultTaxRegimeController.onSubmit(Yes)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: Set[TaxRegime]) =>
    val navigator = mock[JourneyNavigator]
    val nextPage = routes.CheckYourAnswersController.onPageLoad()
    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
    val repository = mock[SessionRepository]
    when(repository.set(any())).thenReturn(Future.successful(true))
    val userAnswers = for {
      answers <- Try(UserAnswers("id"))
      addATaxRegime = Choice.Yes
      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
    } yield answers
    val controller = makeController(
      userAnswers = userAnswers.toOption,
      navigator = navigator,
      repository = repository,
      formProvider = defaultFormProvider
    )
    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
    val form = defaultFormProvider().fill(answer)
    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
    val response = controller.onSubmit(NormalMode)(request)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(nextPage.path())
  }

  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: Set[TaxRegime]) =>
    val controller = makeController(userAnswers = None)
    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
    val form = defaultFormProvider().fill(answer)
    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
    val response = controller.onSubmit(NormalMode)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }

  it should "redirect to journey recovery when a prerequisite answer is missing" in forAll(minSuccessful(5)) { (answer: Set[TaxRegime]) =>
    val userAnswers = Success(UserAnswers("id"))
    val controller = makeController(userAnswers = userAnswers.toOption)
    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
    val form = defaultFormProvider().fill(answer)
    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
    val response = controller.onSubmit(NormalMode)(request)
    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
    status(response) shouldBe SEE_OTHER
    redirectLocation(response) shouldBe Some(journeyRecovery.path())
  }

  it should "throw exceptions to the top level error handler" in {
    val userAnswers = for {
      answers <- Try(UserAnswers("id"))
      addATaxRegime = Choice.Yes
      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
    } yield answers
    val repository = mock[SessionRepository]
    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
    val request = FakeRequest(route)
    recoverToSucceededIf[MongoException] {
      controller.onPageLoad(NormalMode)(request)
    }
  }
}
