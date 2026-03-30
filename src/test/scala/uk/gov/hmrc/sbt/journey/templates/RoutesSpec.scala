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

class RoutesSpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  def rootPage(pageKey: String, viewRoute: String = "") = RootPage(
    s"$pageKey.title",
    s"$pageKey.heading",
    if (viewRoute.isEmpty) s"/${kebabCase(pageKey)}" else viewRoute,
    (basePackage / "controllers" / s"Default${pascalCase(pageKey)}Controller").toString,
    s"views.html.${pascalCase(pageKey)}View",
    withDefaultController = true
  )

  def journeyPage(pageKey: String, answerType: FieldType) = JourneyPage(
    pageKey,
    s"$pageKey.title",
    s"$pageKey.heading",
    s"/${kebabCase(pageKey)}",
    s"/change-${kebabCase(pageKey)}",
    (basePackage / "controllers" / s"Default${pascalCase(pageKey)}Controller").toString,
    (basePackage / "forms" / s"Default${pascalCase(pageKey)}FormProvider").toString,
    s"views.html.${pascalCase(pageKey)}View",
    withDefaultController = true,
    withDefaultFormProvider = true,
    answerType
  )

  def journeyConfig(journey: (String, Journey)) =
    JourneyConfig(
      basePackage.toString,
      Map.empty,
      Map.empty,
      Map(journey)
    )

  "Routes.render" should "render routes for root pages" in {
    val config = JourneyConfig(
      basePackage.toString,
      Map("index" -> rootPage("index", "/"), "checkYourAnswers" -> rootPage("checkYourAnswers")),
      Map.empty,
      Map.empty
    )

    Routes.render(config) shouldBe
      """GET  /  uk.gov.hmrc.sbtjourneytest.controllers.DefaultIndexController.onPageLoad
        |
        |GET  /check-your-answers  uk.gov.hmrc.sbtjourneytest.controllers.DefaultCheckYourAnswersController.onPageLoad""".stripMargin
  }

  it should "render routes for a single journey page" in {
    val pageKey = "contactDetails"

    val contactDetailsPage =
      journeyPage(pageKey, ClassType(basePackage / "models" / "ContactDetails"))

    val config = journeyConfig(
      "contactDetails" -> Journey(
        pages = Map(pageKey -> contactDetailsPage),
        journey = List(SinglePagePart(pageKey, None))
      )
    )

    Routes.render(config) shouldBe
      """GET  /contact-details         uk.gov.hmrc.sbtjourneytest.controllers.DefaultContactDetailsController.onPageLoad(mode: Mode = NormalMode)
        |POST /contact-details         uk.gov.hmrc.sbtjourneytest.controllers.DefaultContactDetailsController.onSubmit(mode: Mode = NormalMode)
        |GET  /change-contact-details  uk.gov.hmrc.sbtjourneytest.controllers.DefaultContactDetailsController.onPageLoad(mode: Mode = CheckMode)
        |POST /change-contact-details  uk.gov.hmrc.sbtjourneytest.controllers.DefaultContactDetailsController.onSubmit(mode: Mode = CheckMode)""".stripMargin
  }

  it should "render routes with index parameters for a subjourney page of a do-while journey" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      "auditEvents" -> Journey(
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
    )


    Routes.render(config) shouldBe
      """GET  /audit-events/:auditEvents/add-another-audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onPageLoad(auditEvents: Int, mode: Mode = NormalMode)
        |POST /audit-events/:auditEvents/add-another-audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onSubmit(auditEvents: Int, mode: Mode = NormalMode)
        |GET  /audit-events/:auditEvents/change-add-another-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onPageLoad(auditEvents: Int, mode: Mode = CheckMode)
        |POST /audit-events/:auditEvents/change-add-another-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onSubmit(auditEvents: Int, mode: Mode = CheckMode)
        |
        |GET  /audit-events/:auditEvents/audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onPageLoad(auditEvents: Int, mode: Mode = NormalMode)
        |POST /audit-events/:auditEvents/audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onSubmit(auditEvents: Int, mode: Mode = NormalMode)
        |GET  /audit-events/:auditEvents/change-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onPageLoad(auditEvents: Int, mode: Mode = CheckMode)
        |POST /audit-events/:auditEvents/change-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onSubmit(auditEvents: Int, mode: Mode = CheckMode)""".stripMargin
  }

  it should "render non-parameterised routes for subjourney pages of switch-case journeys" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      "whichTaxRegime" -> Journey(
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
    )

    Routes.render(config) shouldBe
      """GET  /sa-info         uk.gov.hmrc.sbtjourneytest.controllers.DefaultSaInfoController.onPageLoad(mode: Mode = NormalMode)
        |POST /sa-info         uk.gov.hmrc.sbtjourneytest.controllers.DefaultSaInfoController.onSubmit(mode: Mode = NormalMode)
        |GET  /change-sa-info  uk.gov.hmrc.sbtjourneytest.controllers.DefaultSaInfoController.onPageLoad(mode: Mode = CheckMode)
        |POST /change-sa-info  uk.gov.hmrc.sbtjourneytest.controllers.DefaultSaInfoController.onSubmit(mode: Mode = CheckMode)
        |
        |GET  /vat-info         uk.gov.hmrc.sbtjourneytest.controllers.DefaultVatInfoController.onPageLoad(mode: Mode = NormalMode)
        |POST /vat-info         uk.gov.hmrc.sbtjourneytest.controllers.DefaultVatInfoController.onSubmit(mode: Mode = NormalMode)
        |GET  /change-vat-info  uk.gov.hmrc.sbtjourneytest.controllers.DefaultVatInfoController.onPageLoad(mode: Mode = CheckMode)
        |POST /change-vat-info  uk.gov.hmrc.sbtjourneytest.controllers.DefaultVatInfoController.onSubmit(mode: Mode = CheckMode)
        |
        |GET  /which-tax-regime         uk.gov.hmrc.sbtjourneytest.controllers.DefaultWhichTaxRegimeController.onPageLoad(mode: Mode = NormalMode)
        |POST /which-tax-regime         uk.gov.hmrc.sbtjourneytest.controllers.DefaultWhichTaxRegimeController.onSubmit(mode: Mode = NormalMode)
        |GET  /change-which-tax-regime  uk.gov.hmrc.sbtjourneytest.controllers.DefaultWhichTaxRegimeController.onPageLoad(mode: Mode = CheckMode)
        |POST /change-which-tax-regime  uk.gov.hmrc.sbtjourneytest.controllers.DefaultWhichTaxRegimeController.onSubmit(mode: Mode = CheckMode)""".stripMargin
  }

  it should "render routes with multiple index parameters for subjourney pages of nested do-while journeys" in {
    val auditSource          = journeyPage("auditSource", FieldType.STRING)
    val auditEvent           = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditEvent = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val config = journeyConfig(
      "auditSources" -> Journey(
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
    )

    Routes.render(config) shouldBe
      """GET  /audit-sources/:auditSources/audit-events/:auditEvents/add-another-audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onPageLoad(auditSources: Int, auditEvents: Int, mode: Mode = NormalMode)
        |POST /audit-sources/:auditSources/audit-events/:auditEvents/add-another-audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onSubmit(auditSources: Int, auditEvents: Int, mode: Mode = NormalMode)
        |GET  /audit-sources/:auditSources/audit-events/:auditEvents/change-add-another-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onPageLoad(auditSources: Int, auditEvents: Int, mode: Mode = CheckMode)
        |POST /audit-sources/:auditSources/audit-events/:auditEvents/change-add-another-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAddAnotherAuditEventController.onSubmit(auditSources: Int, auditEvents: Int, mode: Mode = CheckMode)
        |
        |GET  /audit-sources/:auditSources/audit-events/:auditEvents/audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onPageLoad(auditSources: Int, auditEvents: Int, mode: Mode = NormalMode)
        |POST /audit-sources/:auditSources/audit-events/:auditEvents/audit-event         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onSubmit(auditSources: Int, auditEvents: Int, mode: Mode = NormalMode)
        |GET  /audit-sources/:auditSources/audit-events/:auditEvents/change-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onPageLoad(auditSources: Int, auditEvents: Int, mode: Mode = CheckMode)
        |POST /audit-sources/:auditSources/audit-events/:auditEvents/change-audit-event  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditEventController.onSubmit(auditSources: Int, auditEvents: Int, mode: Mode = CheckMode)
        |
        |GET  /audit-sources/:auditSources/audit-source         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditSourceController.onPageLoad(auditSources: Int, mode: Mode = NormalMode)
        |POST /audit-sources/:auditSources/audit-source         uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditSourceController.onSubmit(auditSources: Int, mode: Mode = NormalMode)
        |GET  /audit-sources/:auditSources/change-audit-source  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditSourceController.onPageLoad(auditSources: Int, mode: Mode = CheckMode)
        |POST /audit-sources/:auditSources/change-audit-source  uk.gov.hmrc.sbtjourneytest.controllers.DefaultAuditSourceController.onSubmit(auditSources: Int, mode: Mode = CheckMode)""".stripMargin
  }
}
