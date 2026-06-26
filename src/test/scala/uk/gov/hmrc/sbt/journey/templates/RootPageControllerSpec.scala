/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.sbt.journey.templates

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.sbt.journey.models.{QualifiedName, RootPage}

class RootPageControllerSpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  "RootPageController.render" should "render a controller class for a root page of the application" in {
    val rootPage = RootPage(
      titleKey = "beforeYouStart.title",
      headingKey = "beforeYouStart.heading",
      viewRoute = "before-you-start",
      controllerClass = (basePackage / "controllers" / "DefaultBeforeYouStartController").toString,
      viewClass = "views.html.BeforeYouStartView",
      withDefaultController = true
    )

    RootPageController.render(basePackage, "beforeYouStart", rootPage) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultBeforeYouStartController])
        |trait BeforeYouStartBaseController extends FrontendBaseController with I18nSupport {
        |  def onPageLoad: Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultBeforeYouStartController @Inject() (
        |  identify: IdentifierAction,
        |  view: views.html.BeforeYouStartView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends BeforeYouStartBaseController {
        |  def onPageLoad: Action[AnyContent] = identify { implicit request =>
        |    Ok(view())
        |  }
        |}
        |""".stripMargin
  }

  it should "provide no default controller implementation if requested" in {
    val rootPage = RootPage(
      titleKey = "beforeYouStart.title",
      headingKey = "beforeYouStart.heading",
      viewRoute = "before-you-start",
      controllerClass = (basePackage / "controllers" / "DefaultBeforeYouStartController").toString,
      viewClass = "views.html.BeforeYouStartView",
      withDefaultController = false
    )

    RootPageController.render(basePackage, "beforeYouStart", rootPage) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |trait BeforeYouStartBaseController extends FrontendBaseController with I18nSupport {
        |  def onPageLoad: Action[AnyContent]
        |}
        |""".stripMargin
  }

  "RootPageController.renderSpec" should "render a controller test suite for a root page of the application" in {
    val rootPage = RootPage(
      titleKey = "beforeYouStart.title",
      headingKey = "beforeYouStart.heading",
      viewRoute = "before-you-start",
      controllerClass = (basePackage / "controllers" / "DefaultBeforeYouStartController").toString,
      viewClass = "views.html.BeforeYouStartView",
      withDefaultController = true
    )

    RootPageController.renderSpec(basePackage, "beforeYouStart", rootPage) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.FakeIdentifierAction // uk.gov.hmrc.sbtjourneytest.controllers.actions.FakeIdentifierAction
        |import org.apache.pekko.actor.ActorSystem
        |import org.mockito.ArgumentMatchers.any
        |import org.mockito.Mockito.when
        |import org.scalatest.flatspec.AnyFlatSpec
        |import org.scalatest.matchers.should.Matchers
        |import org.scalatestplus.mockito.MockitoSugar
        |import play.api.http.{MimeTypes, Status}
        |import play.api.test.FakeRequest
        |import play.api.test.Helpers.*
        |import play.twirl.api.HtmlFormat
        |import views.html.BeforeYouStartView
        |
        |import scala.concurrent.ExecutionContext
        |
        |class DefaultBeforeYouStartControllerSpec extends AnyFlatSpec, Matchers, MockitoSugar {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |
        |  private val beforeYouStartView = mock[BeforeYouStartView]
        |
        |  private val controller = new DefaultBeforeYouStartController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    beforeYouStartView,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultBeforeYouStartController.onPageLoad" should "return a 200 OK response containing the view HTML" in {
        |    val mockResponse = "<html>Hello</html>"
        |    when(beforeYouStartView()(any(), any())).thenReturn(HtmlFormat.raw(mockResponse))
        |    val result = controller.onPageLoad(FakeRequest())
        |    status(result) shouldBe Status.OK
        |    contentType(result) shouldBe Some(MimeTypes.HTML)
        |    contentAsString(result) shouldBe mockResponse
        |  }
        |}
        |""".stripMargin
  }
}
