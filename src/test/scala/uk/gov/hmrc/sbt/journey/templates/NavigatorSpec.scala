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

class NavigatorSpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  val navigator = new Navigator(
    Map(
      "Choice"    -> EnumModel("Choice", List("Yes", "No")),
      "TaxRegime" -> EnumModel("TaxRegime", List("SA", "VAT")),
      "Domicile" -> EnumModel(
        "Domicile",
        List("ENGLAND_WALES", "SCOTLAND", "NORTHERN_IRELAND", "OTHER")
      )
    )
  )

  def rootPage(pageKey: String, viewRoute: String = "") = RootPage(
    s"$pageKey.title",
    s"$pageKey.heading",
    if (viewRoute.isEmpty) s"/${kebabCase(pageKey)}" else viewRoute,
    (basePackage / "controllers" / s"${pascalCase(pageKey)}BaseController").toString,
    s"views.html.${pascalCase(pageKey)}View",
    withDefaultController = true
  )

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

  def journeyConfig(journey: (String, Journey), rootPages: Map[String, RootPage] = Map.empty) =
    JourneyConfig(
      "sbt-journey-test",
      basePackage.toString,
      Map.empty,
      Map.empty,
      Map(journey)
    )

  "Navigator.normalRoutesFor" should "render navigations between the pages of a linear journey" in {
    val cipAssessmentTicket =
      journeyPage("cipAssessmentTicket", FieldType.STRING)
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "cipAssessment" -> Journey(
        pages = Map(
          "cipAssessmentTicket" -> cipAssessmentTicket,
          "cipAssessmentPage"   -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentTicket", None),
          SinglePagePart("cipAssessmentPage", None),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case CipAssessmentTicketPage => _ => _ =>
        |      routes.CipAssessmentPageBaseController.onPageLoad(NormalMode)
        |    case CipAssessmentPagePage => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render navigations between the pages of a do-while journey" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case AddAnotherAuditEventPage(auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, NormalMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AuditEventPage(auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, NormalMode)""".stripMargin
  }

  it should "render a navigation from a single journey page to a do-while journey" in {
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent,
          "cipAssessmentPage"    -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentPage", None),
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case CipAssessmentPagePage => _ => _ =>
        |      routes.AuditEventBaseController.onPageLoad(0, NormalMode)
        |    case AddAnotherAuditEventPage(auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, NormalMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AuditEventPage(auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, NormalMode)""".stripMargin
  }

  it should "render a navigation from a do-while journey to a single journey page" in {
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent,
          "cipAssessmentPage"    -> cipAssessmentPage
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("cipAssessmentPage", None),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case AddAnotherAuditEventPage(auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, NormalMode)
        |      case Choice.No  => routes.CipAssessmentPageBaseController.onPageLoad(NormalMode)
        |    }
        |    case AuditEventPage(auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, NormalMode)
        |    case CipAssessmentPagePage => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render a navigation from a single journey page to a nested do-while journey" in {
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)
    val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"            -> auditEvent,
          "addAnotherAuditEvent"  -> addAnotherAuditEvent,
          "addAnotherAuditSource" -> addAnotherAuditSource,
          "cipAssessmentPage"     -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentPage", None),
          DoWhilePart(
            "addAnotherAuditSource",
            List(
              DoWhilePart(
                "addAnotherAuditEvent",
                List(SinglePagePart("auditEvent", None)),
                "auditEvents"
              )
            ),
            "auditSources"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case CipAssessmentPagePage => _ => _ =>
        |      routes.AuditEventBaseController.onPageLoad(0, 0, NormalMode)
        |    case AddAnotherAuditSourcePage(auditSourcesIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex + 1, NormalMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AddAnotherAuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, NormalMode)
        |      case Choice.No  => routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, NormalMode)
        |    }
        |    case AuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, NormalMode)""".stripMargin
  }

  it should "render navigations between the pages of a switch-case journey" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
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
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case WhichTaxRegimePage => _ => {
        |      case TaxRegime.SA => routes.SaInfoBaseController.onPageLoad(NormalMode)
        |      case TaxRegime.VAT => routes.VatInfoBaseController.onPageLoad(NormalMode)
        |    }
        |    case SaInfoPage(TaxRegime.SA) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad
        |    case VatInfoPage(TaxRegime.VAT) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render navigations between the pages of a switch-case journey with a default case" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "whichTaxRegime" -> whichTaxRegime,
          "saInfo"         -> saInfo,
          "vatInfo"        -> vatInfo
        ),
        journey = List(
          SwitchCasePart(
            "whichTaxRegime",
            Map(
              "SA"      -> List(SinglePagePart("saInfo", None)),
              "default" -> List(SinglePagePart("vatInfo", None))
            ),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case WhichTaxRegimePage => _ => {
        |      case TaxRegime.SA => routes.SaInfoBaseController.onPageLoad(NormalMode)
        |      case _ => routes.VatInfoBaseController.onPageLoad(NormalMode)
        |    }
        |    case SaInfoPage(TaxRegime.SA) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad
        |    case VatInfoPage(_) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render navigations between the pages of a switch-case journey when there is one uncovered case" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "whichTaxRegime" -> whichTaxRegime,
          "saInfo"         -> saInfo
        ),
        journey = List(
          SwitchCasePart(
            "whichTaxRegime",
            Map("SA" -> List(SinglePagePart("saInfo", None))),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case WhichTaxRegimePage => _ => {
        |      case TaxRegime.SA => routes.SaInfoBaseController.onPageLoad(NormalMode)
        |      case TaxRegime.VAT => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case SaInfoPage(TaxRegime.SA) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render navigations between the pages of a switch-case journey when there are multiple uncovered cases" in {
    val whereDomiciled =
      journeyPage("whereDomiciled", ClassType(basePackage / "models" / "Domicile"))
    val longTermResident =
      journeyPage("longTermResident", FieldType.BOOLEAN)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whereDomiciled" -> Journey(
        pages = Map(
          "whereDomiciled"   -> whereDomiciled,
          "longTermResident" -> longTermResident
        ),
        journey = List(
          SwitchCasePart(
            "whereDomiciled",
            Map("OTHER" -> List(SinglePagePart("longTermResident", None))),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case WhereDomiciledPage => _ => {
        |      case Domicile.OTHER => routes.LongTermResidentBaseController.onPageLoad(NormalMode)
        |      case _ => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case LongTermResidentPage(Domicile.OTHER) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render navigations between the pages of an if-then journey" in {
    val addATaxRegime =
      journeyPage("addATaxRegime", ClassType(basePackage / "Choice"))
    val taxRegime =
      journeyPage("taxRegime", FieldType.STRING)
    val addAnotherTaxRegime =
      journeyPage("addAnotherTaxRegime", ClassType(basePackage / "Choice"))

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "addATaxRegime"       -> addATaxRegime,
          "taxRegime"           -> taxRegime,
          "addAnotherTaxRegime" -> addAnotherTaxRegime
        ),
        journey = List(
          IfThenPart(
            addATaxRegime.pageKey,
            List(
              DoWhilePart(
                addAnotherTaxRegime.pageKey,
                List(SinglePagePart(taxRegime.pageKey, None)),
                "taxRegimes"
              )
            ),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case AddATaxRegimePage => _ => {
        |      case Choice.Yes => routes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AddAnotherTaxRegimePage(Choice.Yes,taxRegimesIndex) => _ => {
        |      case Choice.Yes => routes.TaxRegimeBaseController.onPageLoad(taxRegimesIndex + 1, NormalMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case TaxRegimePage(Choice.Yes,taxRegimesIndex) => _ => _ =>
        |      routes.AddAnotherTaxRegimeBaseController.onPageLoad(taxRegimesIndex, NormalMode)""".stripMargin
  }

  it should "render navigations between the pages of a nested do-while journey" in {
    val auditSource = journeyPage("auditSource", FieldType.STRING)
    val auditEvent  = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditSource =
      journeyPage("addAnotherAuditSource", ClassType(basePackage / "Choice"))
    val addAnotherAuditEvent =
      journeyPage("addAnotherAuditEvent", ClassType(basePackage / "Choice"))

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditSources" -> Journey(
        pages = Map(
          "auditEvent"            -> auditEvent,
          "auditSource"           -> auditSource,
          "addAnotherAuditSource" -> addAnotherAuditSource,
          "addAnotherAuditEvent"  -> addAnotherAuditEvent
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
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.normalRoutesFor(config) shouldBe
      """    case AddAnotherAuditSourcePage(auditSourcesIndex) => _ => {
        |      case Choice.Yes => routes.AuditSourceBaseController.onPageLoad(auditSourcesIndex + 1, NormalMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AuditSourcePage(auditSourcesIndex) => _ => _ =>
        |      routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, 0, NormalMode)
        |    case AddAnotherAuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, NormalMode)
        |      case Choice.No  => routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, NormalMode)
        |    }
        |    case AuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, NormalMode)""".stripMargin
  }

  "Navigator.checkRoutesFor" should "render navigations between the pages of a linear journey" in {
    val cipAssessmentTicket =
      journeyPage("cipAssessmentTicket", FieldType.STRING)
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "cipAssessment" -> Journey(
        pages = Map(
          "cipAssessmentTicket" -> cipAssessmentTicket,
          "cipAssessmentPage"   -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentTicket", None),
          SinglePagePart("cipAssessmentPage", None),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case CipAssessmentTicketPage => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad
        |    case CipAssessmentPagePage => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render navigations between the pages of a do-while journey" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case AddAnotherAuditEventPage(auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, CheckMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AuditEventPage(auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, CheckMode)""".stripMargin
  }

  it should "render a navigation from a single journey page to a do-while journey" in {
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent,
          "cipAssessmentPage"    -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentPage", None),
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case CipAssessmentPagePage => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad
        |    case AddAnotherAuditEventPage(auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, CheckMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AuditEventPage(auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, CheckMode)""".stripMargin
  }

  it should "render a navigation from a do-while journey to a single journey page" in {
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent,
          "cipAssessmentPage"    -> cipAssessmentPage
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("cipAssessmentPage", None),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case AddAnotherAuditEventPage(auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, CheckMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AuditEventPage(auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, CheckMode)
        |    case CipAssessmentPagePage => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render a navigation from a single journey page to a nested do-while journey" in {
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)
    val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"            -> auditEvent,
          "addAnotherAuditEvent"  -> addAnotherAuditEvent,
          "addAnotherAuditSource" -> addAnotherAuditSource,
          "cipAssessmentPage"     -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentPage", None),
          DoWhilePart(
            "addAnotherAuditSource",
            List(
              DoWhilePart(
                "addAnotherAuditEvent",
                List(SinglePagePart("auditEvent", None)),
                "auditEvents"
              )
            ),
            "auditSources"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case CipAssessmentPagePage => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad
        |    case AddAnotherAuditSourcePage(auditSourcesIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex + 1, CheckMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AddAnotherAuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, CheckMode)
        |      case Choice.No  => routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, CheckMode)
        |    }
        |    case AuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, CheckMode)""".stripMargin
  }

  it should "render navigations between the pages of a switch-case journey" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
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
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case WhichTaxRegimePage => _ => {
        |      case TaxRegime.SA => routes.SaInfoBaseController.onPageLoad(CheckMode)
        |      case TaxRegime.VAT => routes.VatInfoBaseController.onPageLoad(CheckMode)
        |    }
        |    case SaInfoPage(TaxRegime.SA) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad
        |    case VatInfoPage(TaxRegime.VAT) => _ => _ =>
        |      routes.CheckYourAnswersBaseController.onPageLoad""".stripMargin
  }

  it should "render navigations between the pages of an if-then journey" in {
    val addATaxRegime =
      journeyPage("addATaxRegime", ClassType(basePackage / "Choice"))
    val taxRegime =
      journeyPage("taxRegime", FieldType.STRING)
    val addAnotherTaxRegime =
      journeyPage("addAnotherTaxRegime", ClassType(basePackage / "Choice"))

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "addATaxRegime"       -> addATaxRegime,
          "taxRegime"           -> taxRegime,
          "addAnotherTaxRegime" -> addAnotherTaxRegime
        ),
        journey = List(
          IfThenPart(
            addATaxRegime.pageKey,
            List(
              DoWhilePart(
                addAnotherTaxRegime.pageKey,
                List(SinglePagePart(taxRegime.pageKey, None)),
                "taxRegimes"
              )
            ),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case AddATaxRegimePage => _ => {
        |      case Choice.Yes => routes.TaxRegimeBaseController.onPageLoad(0, CheckMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AddAnotherTaxRegimePage(Choice.Yes,taxRegimesIndex) => _ => {
        |      case Choice.Yes => routes.TaxRegimeBaseController.onPageLoad(taxRegimesIndex + 1, CheckMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case TaxRegimePage(Choice.Yes,taxRegimesIndex) => _ => _ =>
        |      routes.AddAnotherTaxRegimeBaseController.onPageLoad(taxRegimesIndex, CheckMode)""".stripMargin
  }

  it should "render navigations between the pages of a nested do-while journey" in {
    val auditSource = journeyPage("auditSource", FieldType.STRING)
    val auditEvent  = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditSource =
      journeyPage("addAnotherAuditSource", ClassType(basePackage / "Choice"))
    val addAnotherAuditEvent =
      journeyPage("addAnotherAuditEvent", ClassType(basePackage / "Choice"))

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditSources" -> Journey(
        pages = Map(
          "auditEvent"            -> auditEvent,
          "auditSource"           -> auditSource,
          "addAnotherAuditSource" -> addAnotherAuditSource,
          "addAnotherAuditEvent"  -> addAnotherAuditEvent
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
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.checkRoutesFor(config) shouldBe
      """    case AddAnotherAuditSourcePage(auditSourcesIndex) => _ => {
        |      case Choice.Yes => routes.AuditSourceBaseController.onPageLoad(auditSourcesIndex + 1, CheckMode)
        |      case Choice.No  => routes.CheckYourAnswersBaseController.onPageLoad
        |    }
        |    case AuditSourcePage(auditSourcesIndex) => _ => _ =>
        |      routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, 0, CheckMode)
        |    case AddAnotherAuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => {
        |      case Choice.Yes => routes.AuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex + 1, CheckMode)
        |      case Choice.No  => routes.AddAnotherAuditSourceBaseController.onPageLoad(auditSourcesIndex, CheckMode)
        |    }
        |    case AuditEventPage(auditSourcesIndex,auditEventsIndex) => _ => _ =>
        |      routes.AddAnotherAuditEventBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, CheckMode)""".stripMargin
  }

  "Navigator.render" should "render a Navigator interface and default implementation" in {
    val config =
      JourneyConfig("sbt-journey-test", basePackage.toString, Map.empty, Map.empty, Map.empty)

    navigator.render(config) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.navigation
        |
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import _root_.models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import _root_.models.CheckMode // uk.gov.hmrc.sbtjourneytest.models.CheckMode
        |import _root_.models.NormalMode // uk.gov.hmrc.sbtjourneytest.models.NormalMode
        |import _root_.models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import _root_.pages.Page // uk.gov.hmrc.sbtjourneytest.pages.Page
        |import _root_.pages.QuestionPage // uk.gov.hmrc.sbtjourneytest.pages.QuestionPage
        |import com.google.inject.ImplementedBy
        |import play.api.mvc.Call
        |
        |import javax.inject.{Inject,Singleton}
        |
        |@ImplementedBy(classOf[DefaultJourneyNavigator])
        |trait JourneyNavigator {
        |  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, latestAnswer: page.AnswerType): Call
        |}
        |
        |@Singleton
        |class DefaultJourneyNavigator @Inject() () extends JourneyNavigator {
        |  private val normalRoutes: (page: Page) => UserAnswers => page.AnswerType => Call = {
        |
        |  }
        |
        |  private val checkRoutes: (page: Page) => UserAnswers => page.AnswerType => Call = {
        |
        |  }
        |
        |  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, latestAnswer: page.AnswerType): Call =
        |    mode match {
        |      case NormalMode => normalRoutes(page)(userAnswers)(latestAnswer)
        |      case CheckMode  => checkRoutes(page)(userAnswers)(latestAnswer)
        |    }
        |}
        |""".stripMargin
  }

  def renderCases(cases: List[String]) = {
    val NL = System.lineSeparator()
    if (cases.isEmpty) ""
    else
      cases.mkString(
        s"""  "DefaultJourneyNavigator" """,
        s"${NL * 2}  it ",
        ""
      )
  }

  "Navigator.normalModeTestsFor" should "render tests for navigations between the pages of a linear journey" in {
    val cipAssessmentTicket =
      journeyPage("cipAssessmentTicket", FieldType.STRING)
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "cipAssessment" -> Journey(
        pages = Map(
          "cipAssessmentTicket" -> cipAssessmentTicket,
          "cipAssessmentPage"   -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentTicket", None),
          SinglePagePart("cipAssessmentPage", None),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.normalModeTestsFor(config)) shouldBe
      s"""  "DefaultJourneyNavigator" should "navigate from CipAssessmentTicketPage to CipAssessmentPagePage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
         |    navigator.nextPage(CipAssessmentTicketPage, NormalMode, userAnswers, answer) shouldBe routes.CipAssessmentPageBaseController.onPageLoad(NormalMode)
         |  }
         |
         |  it should "navigate from CipAssessmentPagePage to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
         |    navigator.nextPage(CipAssessmentPagePage, NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
         |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a do-while journey" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.normalModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from AuditEventPage(i) to AddAnotherAuditEventPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val auditEventsIndex = 0
        |    navigator.nextPage(AuditEventPage(auditEventsIndex), NormalMode, userAnswers, answer) shouldBe routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, NormalMode)
        |  }
        |
        |  it should "navigate from AddAnotherAuditEventPage(i) to AuditEventPage at the next index when the user chooses Yes in normal mode" in {
        |    val auditEventsIndex = 0
        |    navigator.nextPage(AddAnotherAuditEventPage(auditEventsIndex), NormalMode, userAnswers, Choice.Yes) shouldBe routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, NormalMode)
        |  }
        |
        |  it should "navigate from AddAnotherAuditEventPage(i) to CheckYourAnswersPage when the user chooses No in normal mode" in {
        |    val auditEventsIndex = 0
        |    navigator.nextPage(AddAnotherAuditEventPage(auditEventsIndex), NormalMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of an if-then journey" in {
    val addATaxRegime =
      journeyPage("addATaxRegime", ClassType(basePackage / "Choice"))
    val taxRegime =
      journeyPage("taxRegime", FieldType.STRING)
    val addAnotherTaxRegime =
      journeyPage("addAnotherTaxRegime", ClassType(basePackage / "Choice"))

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "addATaxRegime"       -> addATaxRegime,
          "taxRegime"           -> taxRegime,
          "addAnotherTaxRegime" -> addAnotherTaxRegime
        ),
        journey = List(
          IfThenPart(
            addATaxRegime.pageKey,
            List(
              DoWhilePart(
                addAnotherTaxRegime.pageKey,
                List(SinglePagePart(taxRegime.pageKey, None)),
                "taxRegimes"
              )
            ),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.normalModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from AddATaxRegimePage to TaxRegimePage when the user chooses Yes in normal mode" in {
        |    navigator.nextPage(AddATaxRegimePage, NormalMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
        |  }
        |
        |  it should "navigate from AddATaxRegimePage to CheckYourAnswersPage when the user chooses No in normal mode" in {
        |    navigator.nextPage(AddATaxRegimePage, NormalMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from TaxRegimePage(Yes, i) to AddAnotherTaxRegimePage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val taxRegimesIndex = 0
        |    navigator.nextPage(TaxRegimePage(Choice.Yes, taxRegimesIndex), NormalMode, userAnswers, answer) shouldBe routes.AddAnotherTaxRegimeBaseController.onPageLoad(taxRegimesIndex, NormalMode)
        |  }
        |
        |  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to TaxRegimePage at the next index when the user chooses Yes in normal mode" in {
        |    val taxRegimesIndex = 0
        |    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), NormalMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(taxRegimesIndex + 1, NormalMode)
        |  }
        |
        |  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to CheckYourAnswersPage when the user chooses No in normal mode" in {
        |    val taxRegimesIndex = 0
        |    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), NormalMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a switch-case journey" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
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
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.normalModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhichTaxRegimePage to SaInfoPage when the user chooses SA in normal mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, NormalMode, userAnswers, TaxRegime.SA) shouldBe routes.SaInfoBaseController.onPageLoad(NormalMode)
        |  }
        |
        |  it should "navigate from WhichTaxRegimePage to VatInfoPage when the user chooses VAT in normal mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, NormalMode, userAnswers, TaxRegime.VAT) shouldBe routes.VatInfoBaseController.onPageLoad(NormalMode)
        |  }
        |
        |  it should "navigate from SaInfoPage(SA) to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(SaInfoPage(TaxRegime.SA), NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from VatInfoPage(VAT) to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(VatInfoPage(TaxRegime.VAT), NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a switch-case journey with a default case" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "whichTaxRegime" -> whichTaxRegime,
          "saInfo"         -> saInfo,
          "vatInfo"        -> vatInfo
        ),
        journey = List(
          SwitchCasePart(
            "whichTaxRegime",
            Map(
              "SA"      -> List(SinglePagePart("saInfo", None)),
              "default" -> List(SinglePagePart("vatInfo", None))
            ),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.normalModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhichTaxRegimePage to SaInfoPage when the user chooses SA in normal mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, NormalMode, userAnswers, TaxRegime.SA) shouldBe routes.SaInfoBaseController.onPageLoad(NormalMode)
        |  }
        |
        |  it should "navigate from WhichTaxRegimePage to VatInfoPage when the user chooses VAT in normal mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, NormalMode, userAnswers, TaxRegime.VAT) shouldBe routes.VatInfoBaseController.onPageLoad(NormalMode)
        |  }
        |
        |  it should "navigate from SaInfoPage(SA) to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(SaInfoPage(TaxRegime.SA), NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from VatInfoPage(VAT) to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(VatInfoPage(TaxRegime.VAT), NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a switch-case journey when there is one uncovered case" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "whichTaxRegime" -> whichTaxRegime,
          "saInfo"         -> saInfo
        ),
        journey = List(
          SwitchCasePart(
            "whichTaxRegime",
            Map("SA" -> List(SinglePagePart("saInfo", None))),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.normalModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhichTaxRegimePage to SaInfoPage when the user chooses SA in normal mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, NormalMode, userAnswers, TaxRegime.SA) shouldBe routes.SaInfoBaseController.onPageLoad(NormalMode)
        |  }
        |
        |  it should "navigate from WhichTaxRegimePage to CheckYourAnswersPage when the user chooses VAT in normal mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, NormalMode, userAnswers, TaxRegime.VAT) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from SaInfoPage(SA) to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(SaInfoPage(TaxRegime.SA), NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render navigations between the pages of a switch-case journey when there are multiple uncovered cases" in {
    val whereDomiciled =
      journeyPage("whereDomiciled", ClassType(basePackage / "models" / "Domicile"))
    val longTermResident =
      journeyPage("longTermResident", FieldType.BOOLEAN)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whereDomiciled" -> Journey(
        pages = Map(
          "whereDomiciled"   -> whereDomiciled,
          "longTermResident" -> longTermResident
        ),
        journey = List(
          SwitchCasePart(
            "whereDomiciled",
            Map("OTHER" -> List(SinglePagePart("longTermResident", None))),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.normalModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhereDomiciledPage to LongTermResidentPage when the user chooses OTHER in normal mode" in {
        |    navigator.nextPage(WhereDomiciledPage, NormalMode, userAnswers, Domicile.OTHER) shouldBe routes.LongTermResidentBaseController.onPageLoad(NormalMode)
        |  }
        |
        |  it should "navigate from WhereDomiciledPage to CheckYourAnswersPage when the user chooses ENGLAND_WALES in normal mode" in {
        |    navigator.nextPage(WhereDomiciledPage, NormalMode, userAnswers, Domicile.ENGLAND_WALES) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from WhereDomiciledPage to CheckYourAnswersPage when the user chooses SCOTLAND in normal mode" in {
        |    navigator.nextPage(WhereDomiciledPage, NormalMode, userAnswers, Domicile.SCOTLAND) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from WhereDomiciledPage to CheckYourAnswersPage when the user chooses NORTHERN_IRELAND in normal mode" in {
        |    navigator.nextPage(WhereDomiciledPage, NormalMode, userAnswers, Domicile.NORTHERN_IRELAND) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from LongTermResidentPage(OTHER) to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    navigator.nextPage(LongTermResidentPage(Domicile.OTHER), NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  "Navigator.checkModeTestsFor" should "render tests for navigations between the pages of a linear journey" in {
    val cipAssessmentTicket =
      journeyPage("cipAssessmentTicket", FieldType.STRING)
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "cipAssessment" -> Journey(
        pages = Map(
          "cipAssessmentTicket" -> cipAssessmentTicket,
          "cipAssessmentPage"   -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentTicket", None),
          SinglePagePart("cipAssessmentPage", None),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.checkModeTestsFor(config)) shouldBe
      s"""  "DefaultJourneyNavigator" should "navigate from CipAssessmentTicketPage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
         |    navigator.nextPage(CipAssessmentTicketPage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
         |  }
         |
         |  it should "navigate from CipAssessmentPagePage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
         |    navigator.nextPage(CipAssessmentPagePage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
         |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a do-while journey" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "auditEvents" -> Journey(
        pages = Map(
          "auditEvent"           -> auditEvent,
          "addAnotherAuditEvent" -> addAnotherAuditEvent
        ),
        journey = List(
          DoWhilePart(
            "addAnotherAuditEvent",
            List(SinglePagePart("auditEvent", None)),
            "auditEvents"
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.checkModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from AuditEventPage(i) to AddAnotherAuditEventPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val auditEventsIndex = 0
        |    navigator.nextPage(AuditEventPage(auditEventsIndex), CheckMode, userAnswers, answer) shouldBe routes.AddAnotherAuditEventBaseController.onPageLoad(auditEventsIndex, CheckMode)
        |  }
        |
        |  it should "navigate from AddAnotherAuditEventPage(i) to AuditEventPage at the next index when the user chooses Yes in check mode" in {
        |    val auditEventsIndex = 0
        |    navigator.nextPage(AddAnotherAuditEventPage(auditEventsIndex), CheckMode, userAnswers, Choice.Yes) shouldBe routes.AuditEventBaseController.onPageLoad(auditEventsIndex + 1, CheckMode)
        |  }
        |
        |  it should "navigate from AddAnotherAuditEventPage(i) to CheckYourAnswersPage when the user chooses No in check mode" in {
        |    val auditEventsIndex = 0
        |    navigator.nextPage(AddAnotherAuditEventPage(auditEventsIndex), CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of an if-then journey" in {
    val addATaxRegime =
      journeyPage("addATaxRegime", ClassType(basePackage / "Choice"))
    val taxRegime =
      journeyPage("taxRegime", FieldType.STRING)
    val addAnotherTaxRegime =
      journeyPage("addAnotherTaxRegime", ClassType(basePackage / "Choice"))

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "addATaxRegime"       -> addATaxRegime,
          "taxRegime"           -> taxRegime,
          "addAnotherTaxRegime" -> addAnotherTaxRegime
        ),
        journey = List(
          IfThenPart(
            addATaxRegime.pageKey,
            List(
              DoWhilePart(
                addAnotherTaxRegime.pageKey,
                List(SinglePagePart(taxRegime.pageKey, None)),
                "taxRegimes"
              )
            ),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.checkModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from AddATaxRegimePage to TaxRegimePage when the user chooses Yes in check mode" in {
        |    navigator.nextPage(AddATaxRegimePage, CheckMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(0, CheckMode)
        |  }
        |
        |  it should "navigate from AddATaxRegimePage to CheckYourAnswersPage when the user chooses No in check mode" in {
        |    navigator.nextPage(AddATaxRegimePage, CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from TaxRegimePage(Yes, i) to AddAnotherTaxRegimePage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val taxRegimesIndex = 0
        |    navigator.nextPage(TaxRegimePage(Choice.Yes, taxRegimesIndex), CheckMode, userAnswers, answer) shouldBe routes.AddAnotherTaxRegimeBaseController.onPageLoad(taxRegimesIndex, CheckMode)
        |  }
        |
        |  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to TaxRegimePage at the next index when the user chooses Yes in check mode" in {
        |    val taxRegimesIndex = 0
        |    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), CheckMode, userAnswers, Choice.Yes) shouldBe routes.TaxRegimeBaseController.onPageLoad(taxRegimesIndex + 1, CheckMode)
        |  }
        |
        |  it should "navigate from AddAnotherTaxRegimePage(Yes, i) to CheckYourAnswersPage when the user chooses No in check mode" in {
        |    val taxRegimesIndex = 0
        |    navigator.nextPage(AddAnotherTaxRegimePage(Choice.Yes, taxRegimesIndex), CheckMode, userAnswers, Choice.No) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a switch-case journey" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
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
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.checkModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhichTaxRegimePage to SaInfoPage when the user chooses SA in check mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, CheckMode, userAnswers, TaxRegime.SA) shouldBe routes.SaInfoBaseController.onPageLoad(CheckMode)
        |  }
        |
        |  it should "navigate from WhichTaxRegimePage to VatInfoPage when the user chooses VAT in check mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, CheckMode, userAnswers, TaxRegime.VAT) shouldBe routes.VatInfoBaseController.onPageLoad(CheckMode)
        |  }
        |
        |  it should "navigate from SaInfoPage(SA) to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(SaInfoPage(TaxRegime.SA), CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from VatInfoPage(VAT) to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(VatInfoPage(TaxRegime.VAT), CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a switch-case journey with a default case" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "whichTaxRegime" -> whichTaxRegime,
          "saInfo"         -> saInfo,
          "vatInfo"        -> vatInfo
        ),
        journey = List(
          SwitchCasePart(
            "whichTaxRegime",
            Map(
              "SA"      -> List(SinglePagePart("saInfo", None)),
              "default" -> List(SinglePagePart("vatInfo", None))
            ),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.checkModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhichTaxRegimePage to SaInfoPage when the user chooses SA in check mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, CheckMode, userAnswers, TaxRegime.SA) shouldBe routes.SaInfoBaseController.onPageLoad(CheckMode)
        |  }
        |
        |  it should "navigate from WhichTaxRegimePage to VatInfoPage when the user chooses VAT in check mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, CheckMode, userAnswers, TaxRegime.VAT) shouldBe routes.VatInfoBaseController.onPageLoad(CheckMode)
        |  }
        |
        |  it should "navigate from SaInfoPage(SA) to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(SaInfoPage(TaxRegime.SA), CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from VatInfoPage(VAT) to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(VatInfoPage(TaxRegime.VAT), CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render tests for navigations between the pages of a switch-case journey when there is one uncovered case" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whichTaxRegime" -> Journey(
        pages = Map(
          "whichTaxRegime" -> whichTaxRegime,
          "saInfo"         -> saInfo
        ),
        journey = List(
          SwitchCasePart(
            "whichTaxRegime",
            Map("SA" -> List(SinglePagePart("saInfo", None))),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.checkModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhichTaxRegimePage to SaInfoPage when the user chooses SA in check mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, CheckMode, userAnswers, TaxRegime.SA) shouldBe routes.SaInfoBaseController.onPageLoad(CheckMode)
        |  }
        |
        |  it should "navigate from WhichTaxRegimePage to CheckYourAnswersPage when the user chooses VAT in check mode" in {
        |    navigator.nextPage(WhichTaxRegimePage, CheckMode, userAnswers, TaxRegime.VAT) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from SaInfoPage(SA) to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(SaInfoPage(TaxRegime.SA), CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  it should "render navigations between the pages of a switch-case journey when there are multiple uncovered cases" in {
    val whereDomiciled =
      journeyPage("whereDomiciled", ClassType(basePackage / "models" / "Domicile"))
    val longTermResident =
      journeyPage("longTermResident", FieldType.BOOLEAN)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "whereDomiciled" -> Journey(
        pages = Map(
          "whereDomiciled"   -> whereDomiciled,
          "longTermResident" -> longTermResident
        ),
        journey = List(
          SwitchCasePart(
            "whereDomiciled",
            Map("OTHER" -> List(SinglePagePart("longTermResident", None))),
            None
          ),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    renderCases(navigator.checkModeTestsFor(config)) shouldBe
      """  "DefaultJourneyNavigator" should "navigate from WhereDomiciledPage to LongTermResidentPage when the user chooses OTHER in check mode" in {
        |    navigator.nextPage(WhereDomiciledPage, CheckMode, userAnswers, Domicile.OTHER) shouldBe routes.LongTermResidentBaseController.onPageLoad(CheckMode)
        |  }
        |
        |  it should "navigate from WhereDomiciledPage to CheckYourAnswersPage when the user chooses ENGLAND_WALES in check mode" in {
        |    navigator.nextPage(WhereDomiciledPage, CheckMode, userAnswers, Domicile.ENGLAND_WALES) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from WhereDomiciledPage to CheckYourAnswersPage when the user chooses SCOTLAND in check mode" in {
        |    navigator.nextPage(WhereDomiciledPage, CheckMode, userAnswers, Domicile.SCOTLAND) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from WhereDomiciledPage to CheckYourAnswersPage when the user chooses NORTHERN_IRELAND in check mode" in {
        |    navigator.nextPage(WhereDomiciledPage, CheckMode, userAnswers, Domicile.NORTHERN_IRELAND) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from LongTermResidentPage(OTHER) to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    navigator.nextPage(LongTermResidentPage(Domicile.OTHER), CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }""".stripMargin
  }

  "Navigator.renderSpec" should "render a spec for the journey navigator default implementation" in {
    val cipAssessmentTicket =
      journeyPage("cipAssessmentTicket", FieldType.STRING)
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val config = journeyConfig(
      rootPages = Map("checkYourAnswers" -> rootPage("checkYourAnswers")),
      journey = "cipAssessment" -> Journey(
        pages = Map(
          "cipAssessmentTicket" -> cipAssessmentTicket,
          "cipAssessmentPage"   -> cipAssessmentPage
        ),
        journey = List(
          SinglePagePart("cipAssessmentTicket", None),
          SinglePagePart("cipAssessmentPage", None),
          SinglePagePart("checkYourAnswers", None)
        )
      )
    )

    navigator.renderSpec(config) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.navigation
        |
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
        |import _root_.models.CheckMode // uk.gov.hmrc.sbtjourneytest.models.CheckMode
        |import _root_.models.NormalMode // uk.gov.hmrc.sbtjourneytest.models.NormalMode
        |import _root_.models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import org.scalatest.flatspec.AnyFlatSpec
        |import org.scalatest.matchers.should.Matchers
        |import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
        |
        |class DefaultJourneyNavigatorSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, Generators {
        |  private val navigator = new DefaultJourneyNavigator()
        |  private val userAnswers = UserAnswers("userId")
        |
        |  "DefaultJourneyNavigator" should "navigate from CipAssessmentTicketPage to CipAssessmentPagePage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(CipAssessmentTicketPage, NormalMode, userAnswers, answer) shouldBe routes.CipAssessmentPageBaseController.onPageLoad(NormalMode)
        |  }
        |
        |  it should "navigate from CipAssessmentPagePage to CheckYourAnswersPage for all answers in normal mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(CipAssessmentPagePage, NormalMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from CipAssessmentTicketPage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(CipAssessmentTicketPage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |
        |  it should "navigate from CipAssessmentPagePage to CheckYourAnswersPage for all answers in check mode" in forAll(minSuccessful(5)) { (answer: String) =>
        |    navigator.nextPage(CipAssessmentPagePage, CheckMode, userAnswers, answer) shouldBe routes.CheckYourAnswersBaseController.onPageLoad
        |  }
        |}
        |""".stripMargin
  }
}
