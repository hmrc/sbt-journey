package uk.gov.hmrc.simplejourney.controllers

import controllers.actions.FakeIdentifierAction // uk.gov.hmrc.simplejourney.controllers.actions.FakeIdentifierAction
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
import views.html.BeforeYouBeginView

import scala.concurrent.ExecutionContext

class DefaultBeforeYouBeginControllerSpec extends AnyFlatSpec, Matchers, MockitoSugar {
  given system: ActorSystem = ActorSystem("test")
  given ExecutionContext    = system.dispatcher

  private val beforeYouBeginView = mock[BeforeYouBeginView]

  private val controller = new DefaultBeforeYouBeginController(
    new FakeIdentifierAction(stubPlayBodyParsers),
    beforeYouBeginView,
    stubMessagesControllerComponents()
  )

  "DefaultBeforeYouBeginController.onPageLoad" should "return a 200 OK response containing the view HTML" in {
    val mockResponse = "<html>Hello</html>"
    when(beforeYouBeginView()(any(), any())).thenReturn(HtmlFormat.raw(mockResponse))
    val result = controller.onPageLoad(FakeRequest())
    status(result) shouldBe Status.OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
    contentAsString(result) shouldBe mockResponse
  }
}
