package uk.gov.hmrc.fileuploadjourney.controllers

import controllers.actions.FakeIdentifierAction // uk.gov.hmrc.fileuploadjourney.controllers.actions.FakeIdentifierAction
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.{MimeTypes, Status}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import views.html.BeforeYouStartView

import scala.concurrent.ExecutionContext

class DefaultBeforeYouStartControllerSpec extends AnyFlatSpec, Matchers, MockitoSugar {
  given system: ActorSystem = ActorSystem("test")
  given ExecutionContext    = system.dispatcher

  private val beforeYouStartView = mock[BeforeYouStartView]

  private val controller = new DefaultBeforeYouStartController(
    new FakeIdentifierAction(stubPlayBodyParsers),
    beforeYouStartView,
    stubMessagesControllerComponents()
  )

  "DefaultBeforeYouStartController.onPageLoad" should "return a 200 OK response containing the view HTML" in {
    val mockResponse = "<html>Hello</html>"
    when(beforeYouStartView()(any(), any())).thenReturn(HtmlFormat.raw(mockResponse))
    val result = controller.onPageLoad(FakeRequest())
    status(result) shouldBe Status.OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
    contentAsString(result) shouldBe mockResponse
  }
}
