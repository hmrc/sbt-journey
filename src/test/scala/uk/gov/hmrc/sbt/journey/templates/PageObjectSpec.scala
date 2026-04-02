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
import uk.gov.hmrc.sbt.journey.models.*
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{kebabCase, pascalCase}

class PageObjectSpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  def journeyPage(pageKey: String, answerType: FieldType) = JourneyPage(
    pageKey,
    s"$pageKey.title",
    s"$pageKey.heading",
    s"/${kebabCase(pageKey)}",
    s"/change-${kebabCase(pageKey)}",
    (basePackage / "controllers" / s"${pascalCase(pageKey)}BaseController").toString,
    (basePackage / "forms" / s"${pascalCase(pageKey)}BaseFormProvider").toString,
    s"views.html.${pascalCase(pageKey)}View",
    withDefaultController = true,
    withDefaultFormProvider = true,
    answerType
  )

  "PageObject.render" should "render a page object for a single journey page" in {
    val pageKey = "contactDetails"

    val contactDetailsPage =
      journeyPage(pageKey, ClassType(basePackage / "models" / "ContactDetails"))

    val journey =
      Journey(
        pages = Map(pageKey -> contactDetailsPage),
        journey = List(SinglePagePart(pageKey, None))
      )

    PageObject.render(basePackage, journey, contactDetailsPage) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.pages
        |
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import _root_.pages.* // TODO: Remove this once we have a better template
        |import play.api.libs.json.JsPath
        |import play.api.mvc.Call
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import uk.gov.hmrc.sbtjourneytest.models.ContactDetails
        |
        |object ContactDetailsPage extends QuestionPage[ContactDetails] {
        |  override def path: JsPath = JsPath \ "contactDetails"
        |  override def submitRoute(mode: Mode): Call = routes.ContactDetailsBaseController.onSubmit(mode)
        |  override def toString: String = "contactDetails"
        |}
        |""".stripMargin
  }

  it should "render a page object with index parameters for a subjourney page of a do-while journey" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val journey =
      Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          )
        )
      )

    PageObject.render(basePackage, journey, auditEvent) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.pages
        |
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import _root_.pages.* // TODO: Remove this once we have a better template
        |import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
        |import play.api.mvc.Call
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes
        |
        |
        |case class AuditEventPage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[String] {
        |  override def submitRoute(mode: Mode): Call = makeRoute(mode)
        |  override def toString: String = "auditEvent"
        |}
        |
        |object AuditEventPage {
        |  def apply(auditEventsIndex: Int): AuditEventPage =
        |    new AuditEventPage(
        |      JsPath \ "auditEvents" \ auditEventsIndex \ "auditEvent",
        |      mode => routes.AuditEventBaseController.onSubmit(auditEventsIndex, mode)
        |    )
        |
        |  def unapply(page: AuditEventPage): Option[Int] =
        |    page.path.path match {
        |      case KeyPathNode("auditEvents") :: IdxPathNode(auditEventsIndex) :: KeyPathNode("auditEvent") :: Nil => Some(auditEventsIndex)
        |      case _ => None
        |    }
        |}
        |""".stripMargin
  }

  it should "render a page object with choice parameters for a subjourney page of a switch-case journey" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val journey =
      Journey(
        pages = Map(
          "whichTaxRegime" -> whichTaxRegime,
          "saInfo"         -> saInfo,
          "vatInfo"        -> vatInfo
        ),
        journey = List(
          SwitchCasePart(
            "whichTaxRegime",
            Map(
              "SA"  -> List(SinglePagePart("saInfo", None)),
              "VAT" -> List(SinglePagePart("vatInfo", None))
            ),
            None
          )
        )
      )

    PageObject.render(basePackage, journey, saInfo) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.pages
        |
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import _root_.pages.* // TODO: Remove this once we have a better template
        |import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
        |import play.api.mvc.Call
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import uk.gov.hmrc.sbtjourneytest.models.TaxRegime
        |
        |case class SaInfoPage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[String] {
        |  override def submitRoute(mode: Mode): Call = makeRoute(mode)
        |  override def toString: String = "saInfo"
        |}
        |
        |object SaInfoPage {
        |  def apply(whichTaxRegime: TaxRegime): SaInfoPage =
        |    new SaInfoPage(
        |      JsPath \ "whichTaxRegime" \ whichTaxRegime.toString \ "saInfo",
        |      routes.SaInfoBaseController.onSubmit
        |    )
        |
        |  def unapply(page: SaInfoPage): Option[TaxRegime] =
        |    page.path.path match {
        |      case KeyPathNode("whichTaxRegime") :: KeyPathNode(whichTaxRegime) :: KeyPathNode("saInfo") :: Nil => Some(TaxRegime.valueOf(whichTaxRegime))
        |      case _ => None
        |    }
        |}
        |""".stripMargin
  }

  it should "render a page object with multiple index parameters for a subjourney page of a nested do-while journey" in {
    val auditSource          = journeyPage("auditSource", FieldType.STRING)
    val auditEvent           = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditEvent = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val journey =
      Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "auditSource"          -> auditSource,
          "addAnotherAuditEvent" -> addAnotherAuditEvent
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditSource",
            List(
              SinglePagePart("auditSource", None),
              DoWhilePart(
                "addAnotherAuditEvent",
                List(SinglePagePart("auditEvent", None)),
                "auditEvents"
              )
            ),
            "auditSources"
          )
        )
      )

    PageObject.render(basePackage, journey, auditEvent) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.pages
        |
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import _root_.pages.* // TODO: Remove this once we have a better template
        |import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
        |import play.api.mvc.Call
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes
        |
        |
        |case class AuditEventPage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[String] {
        |  override def submitRoute(mode: Mode): Call = makeRoute(mode)
        |  override def toString: String = "auditEvent"
        |}
        |
        |object AuditEventPage {
        |  def apply(auditSourcesIndex: Int, auditEventsIndex: Int): AuditEventPage =
        |    new AuditEventPage(
        |      JsPath \ "auditSources" \ auditSourcesIndex \ "auditEvents" \ auditEventsIndex \ "auditEvent",
        |      mode => routes.AuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
        |    )
        |
        |  def unapply(page: AuditEventPage): Option[(Int, Int)] =
        |    page.path.path match {
        |      case KeyPathNode("auditSources") :: IdxPathNode(auditSourcesIndex) :: KeyPathNode("auditEvents") :: IdxPathNode(auditEventsIndex) :: KeyPathNode("auditEvent") :: Nil => Some((auditSourcesIndex, auditEventsIndex))
        |      case _ => None
        |    }
        |}
        |""".stripMargin
  }
}
