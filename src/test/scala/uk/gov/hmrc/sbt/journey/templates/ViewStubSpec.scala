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

import java.time.{DayOfWeek, LocalDate}

class ViewStubSpec extends AnyFlatSpec with Matchers {

  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  def journeyPage(pageKey: String, answerType: FieldType) =
    JourneyPage(
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

  "ViewStub.renderNoForm" should "render a simple stub for a view with no form" in {
    ViewStub.renderNoForm("beforeYouStart") shouldBe
      s"""@this(
         |    layout: templates.Layout
         |)
         |
         |@()(implicit request: Request[_], messages: Messages)
         |
         |@layout(
         |    pageTitle = titleNoForm(messages("beforeYouStart.title")),
         |    showBackLink = false
         |) {
         |
         |<h1 class="govuk-heading-xl">@messages("beforeYouStart.heading")</h1>
         |
         |<p class="govuk-body">@messages("beforeYouStart.guidance")</p>
         |}
         |""".stripMargin
  }

  "ViewStub.renderForm" should "render a view stub for a String page" in {
    ViewStub.renderForm(
      Map.empty,
      journeyPage("serviceUrl", FieldType.STRING)
    ) shouldBe
      """@import viewmodels.InputWidth._
        |
        |@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukInput: GovukInput,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("serviceUrl.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukInput(
        |            InputViewModel(
        |                field = form("value"),
        |                label = LabelViewModel(messages("serviceUrl.heading")).asPageHeading()
        |            )
        |            .withWidth(Full)
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for a Boolean page" in {
    ViewStub.renderForm(
      Map.empty,
      journeyPage("areYouSendingSamples", FieldType.BOOLEAN)
    ) shouldBe
      """@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukRadios: GovukRadios,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("areYouSendingSamples.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukRadios(
        |            RadiosViewModel.yesNo(
        |                field = form("value"),
        |                legend = LegendViewModel(messages("areYouSendingSamples.heading")).asPageHeading(),
        |            )
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for an Int page" in {
    ViewStub.renderForm(
      Map.empty,
      journeyPage("howManySamples", FieldType.INT)
    ) shouldBe
      """@import viewmodels.InputWidth._
        |
        |@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukInput: GovukInput,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("howManySamples.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukInput(
        |            InputViewModel(
        |                field = form("value"),
        |                label = LabelViewModel(messages("howManySamples.heading")).asPageHeading()
        |            )
        |            .asNumeric()
        |            .withWidth(Fixed10)
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a form provider for a BigDecimal page" in {
    ViewStub.renderForm(
      Map.empty,
      journeyPage("whatIsTheValuation", ClassType(classOf[BigDecimal]))
    ) shouldBe
      """@import viewmodels.InputWidth._
        |
        |@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukInput: GovukInput,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("whatIsTheValuation.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukInput(
        |            InputViewModel(
        |                field = form("value"),
        |                label = LabelViewModel(messages("whatIsTheValuation.heading")).asPageHeading()
        |            )
        |            .withPrefix(PrefixOrSuffix(content = "£"))
        |            .withWidth(Fixed10)
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for a LocalDate page" in {
    ViewStub.renderForm(
      Map.empty,
      journeyPage("whenDidYouSendSamples", ClassType(classOf[LocalDate]))
    ) shouldBe
      """@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukDateInput: GovukDateInput,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("whenDidYouSendSamples.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukDateInput(
        |            DateViewModel(
        |                field  = form("value"),
        |                legend = LegendViewModel(messages("whenDidYouSendSamples.heading")).asPageHeading()
        |            )
        |            .withHint(HintViewModel(messages("whenDidYouSendSamples.hint")))
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for an optional LocalDate page" in {
    ViewStub.renderForm(
      Map.empty,
      journeyPage("whenDidYouSendSamples", OptionType(ClassType(classOf[LocalDate])))
    ) shouldBe
      """@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukDateInput: GovukDateInput,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("whenDidYouSendSamples.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukDateInput(
        |            DateViewModel(
        |                field  = form("value"),
        |                legend = LegendViewModel(messages("whenDidYouSendSamples.heading")).asPageHeading()
        |            )
        |            .withHint(HintViewModel(messages("whenDidYouSendSamples.hint")))
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for an unsupported type but provide no default inputs" in {
    ViewStub.renderForm(
      Map.empty,
      journeyPage("whichDayOfWeek", ClassType(classOf[DayOfWeek]))
    ) shouldBe
      """@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("whichDayOfWeek.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        ||        @* TODO: Add an input for "value" - there is no default input for DayOfWeek fields *@
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for an enum model" in {
    ViewStub.renderForm(
      Map("Choice" -> EnumModel("Choice", List("Yes", "No"))),
      journeyPage("areYouSendingSamples", ClassType(basePackage / "Choice"))
    ) shouldBe
      """@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukRadios: GovukRadios,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("areYouSendingSamples.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukRadios(
        |            RadiosViewModel(
        |                field = form("value"),
        |                legend = LegendViewModel(messages("areYouSendingSamples.heading")).asPageHeading(),
        |                items = List(
        |                    RadioItem(
        |                        id    = Some("value-yes"),
        |                        value = Some("Yes"),
        |                        content = Text(messages("site.yes"))
        |                    ),
        |                    RadioItem(
        |                        id    = Some("value-no"),
        |                        value = Some("No"),
        |                        content = Text(messages("site.no"))
        |                    )
        |                )
        |            )
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for a case class model" in {
    ViewStub.renderForm(
      Map(
        "AuditEvent" -> CaseClassModel(
          "AuditEvent",
          List(
            "auditType"                   -> FieldType.STRING,
            "description"                 -> FieldType.STRING,
            "expectedGoLiveDate"          -> ClassType(classOf[LocalDate]),
            "expectedDecommissioningDate" -> OptionType(ClassType(classOf[LocalDate]))
          )
        )
      ),
      journeyPage("auditEvent", ClassType(basePackage / "AuditEvent"))
    ) shouldBe
      """@import viewmodels.InputWidth._
        |
        |@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukInput: GovukInput,
        |    govukDateInput: GovukDateInput,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("auditEvent.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        <h1 class="govuk-heading-xl">@messages("auditEvent.heading")</h1>
        |        @govukInput(
        |            InputViewModel(
        |                field = form("value.auditType"),
        |                label = LabelViewModel(messages("auditEvent.auditType"))
        |            )
        |            .withWidth(Full)
        |        )
        |
        |        @govukInput(
        |            InputViewModel(
        |                field = form("value.description"),
        |                label = LabelViewModel(messages("auditEvent.description"))
        |            )
        |            .withWidth(Full)
        |        )
        |
        |        @govukDateInput(
        |            DateViewModel(
        |                field  = form("value.expectedGoLiveDate"),
        |                legend = LegendViewModel(messages("auditEvent.expectedGoLiveDate"))
        |            )
        |            .withHint(HintViewModel(messages("auditEvent.expectedGoLiveDate.hint")))
        |        )
        |
        |        @govukDateInput(
        |            DateViewModel(
        |                field  = form("value.expectedDecommissioningDate"),
        |                legend = LegendViewModel(messages("auditEvent.expectedDecommissioningDate"))
        |            )
        |            .withHint(HintViewModel(messages("auditEvent.expectedDecommissioningDate.hint")))
        |        )
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }

  it should "render a view stub for a case class model with an unsupported field type" in {
    ViewStub.renderForm(
      Map(
        "AuditEvent" -> CaseClassModel(
          "AuditEvent",
          List(
            "auditType"               -> FieldType.STRING,
            "description"             -> FieldType.STRING,
            "expectedGoLiveDate"      -> ClassType(classOf[LocalDate]),
            "expectedGoLiveDayOfWeek" -> ClassType(classOf[DayOfWeek])
          )
        )
      ),
      journeyPage("auditEvent", ClassType(basePackage / "AuditEvent"))
    ) shouldBe
      """@import viewmodels.InputWidth._
        |
        |@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukInput: GovukInput,
        |    govukDateInput: GovukDateInput,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("auditEvent.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        <h1 class="govuk-heading-xl">@messages("auditEvent.heading")</h1>
        |        @govukInput(
        |            InputViewModel(
        |                field = form("value.auditType"),
        |                label = LabelViewModel(messages("auditEvent.auditType"))
        |            )
        |            .withWidth(Full)
        |        )
        |
        |        @govukInput(
        |            InputViewModel(
        |                field = form("value.description"),
        |                label = LabelViewModel(messages("auditEvent.description"))
        |            )
        |            .withWidth(Full)
        |        )
        |
        |        @govukDateInput(
        |            DateViewModel(
        |                field  = form("value.expectedGoLiveDate"),
        |                legend = LegendViewModel(messages("auditEvent.expectedGoLiveDate"))
        |            )
        |            .withHint(HintViewModel(messages("auditEvent.expectedGoLiveDate.hint")))
        |        )
        |
        |        @* TODO: Add an input for "value.expectedGoLiveDayOfWeek" - there is no default input for DayOfWeek fields *@
        |
        |        @govukButton(
        |            ButtonViewModel(messages("site.continue"))
        |        )
        |    }
        |}
        |""".stripMargin
  }
}
