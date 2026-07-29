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

import java.time.DayOfWeek

class ViewStubSpec extends AnyFlatSpec with Matchers {

  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  def journeyPage(pageKey: String, answerType: FieldType) =
    JourneyPage(
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
      basePackage,
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
      basePackage,
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
      basePackage,
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
      basePackage,
      Map.empty,
      journeyPage("whatIsTheValuation", FieldType.BIGDECIMAL)
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
      basePackage,
      Map.empty,
      journeyPage("whenDidYouSendSamples", FieldType.LOCALDATE)
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
      basePackage,
      Map.empty,
      journeyPage("whenDidYouSendSamples", OptionType(FieldType.LOCALDATE))
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
      basePackage,
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
      basePackage,
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

  it should "render a view stub for a set of enum model" in {
    ViewStub.renderForm(
      basePackage,
      Map(
        "SurvivedBy" -> EnumModel(
          "SurvivedBy",
          List("SPOUSE", "SIBLING", "PARENT", "CHILDREN", "GRANDCHILDREN")
        )
      ),
      journeyPage("whoSurvivesDeceased", SetType(ClassType(basePackage / "models" / "SurvivedBy")))
    ) shouldBe
      """@this(
        |    layout: templates.Layout,
        |    formHelper: FormWithCSRF,
        |    govukErrorSummary: GovukErrorSummary,
        |    govukCheckboxes: GovukCheckboxes,
        |    govukButton: GovukButton
        |)
        |
        |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
        |
        |@layout(pageTitle = title(form, messages("whoSurvivesDeceased.title"))) {
        |
        |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
        |        @if(form.errors.nonEmpty) {
        |            @govukErrorSummary(ErrorSummaryViewModel(form))
        |        }
        |
        |        @govukCheckboxes(
        |            CheckboxesViewModel(
        |                form = form,
        |                name = "value",
        |                legend = LegendViewModel(messages("whoSurvivesDeceased.heading")).asPageHeading(),
        |                items = List(
        |                    CheckboxItemViewModel(
        |                        content = Text(messages("whoSurvivesDeceased.value.spouse")),
        |                        fieldId = "value",
        |                        index = 0,
        |                        value = "SPOUSE"
        |                    ),
        |                    CheckboxItemViewModel(
        |                        content = Text(messages("whoSurvivesDeceased.value.sibling")),
        |                        fieldId = "value",
        |                        index = 1,
        |                        value = "SIBLING"
        |                    ),
        |                    CheckboxItemViewModel(
        |                        content = Text(messages("whoSurvivesDeceased.value.parent")),
        |                        fieldId = "value",
        |                        index = 2,
        |                        value = "PARENT"
        |                    ),
        |                    CheckboxItemViewModel(
        |                        content = Text(messages("whoSurvivesDeceased.value.children")),
        |                        fieldId = "value",
        |                        index = 3,
        |                        value = "CHILDREN"
        |                    ),
        |                    CheckboxItemViewModel(
        |                        content = Text(messages("whoSurvivesDeceased.value.grandchildren")),
        |                        fieldId = "value",
        |                        index = 4,
        |                        value = "GRANDCHILDREN"
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
      basePackage,
      Map(
        "AuditEvent" -> CaseClassModel(
          "AuditEvent",
          List(
            "auditType"                   -> FieldType.STRING,
            "description"                 -> FieldType.STRING,
            "expectedGoLiveDate"          -> FieldType.LOCALDATE,
            "expectedDecommissioningDate" -> OptionType(FieldType.LOCALDATE)
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
        |
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
      basePackage,
      Map(
        "AuditEvent" -> CaseClassModel(
          "AuditEvent",
          List(
            "auditType"               -> FieldType.STRING,
            "description"             -> FieldType.STRING,
            "expectedGoLiveDate"      -> FieldType.LOCALDATE,
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
        |
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

  it should "render a view stub for file upload via Upscan" in {
    ViewStub.renderForm(
      basePackage,
      Map.empty,
      journeyPage("uploadWillAndCodicils", ClassType(basePackage / "UploadId"))
    ) shouldBe
      s"""@import uk.gov.hmrc.sbtjourneytest.models.upscan.UpscanFormTemplate
         |
         |@this(
         |    layout: templates.Layout,
         |    govukErrorSummary: GovukErrorSummary,
         |    govukFileUpload: GovukFileUpload,
         |    govukButton: GovukButton
         |)
         |
         |@(form: Form[_], formTemplate: UpscanFormTemplate, mode: Mode)(implicit request: Request[_], messages: Messages)
         |
         |@layout(pageTitle = title(form, messages("uploadWillAndCodicils.title"))) {
         |
         |    <form method="POST" action="@formTemplate.href" enctype="multipart/form-data" novalidate autocomplete="off">
         |        @if(form.errors.nonEmpty) {
         |            @govukErrorSummary(ErrorSummaryViewModel(form))
         |        }
         |
         |        @for((name, value) <- formTemplate.fields) {
         |          <input type="hidden" name="@name" value="@value" />
         |        }
         |
         |        @govukFileUpload(FileUpload(
         |          name = "file",
         |          label = LabelViewModel(messages("uploadWillAndCodicils.heading")).asPageHeading(),
         |          javascript = Some(true)
         |        ))
         |
         |        @govukButton(
         |            ButtonViewModel(messages("site.continue"))
         |        )
         |    </form>
         |}
         |""".stripMargin
  }
}
