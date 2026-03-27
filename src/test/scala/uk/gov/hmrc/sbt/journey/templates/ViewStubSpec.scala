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

class ViewStubSpec extends AnyFlatSpec with Matchers {
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

  "ViewStub.renderForm" should "render a simple stub for a view with a form" in {
    ViewStub.renderForm("areYouSendingSamples") shouldBe
      s"""@this(
         |    layout: templates.Layout,
         |    formHelper: FormWithCSRF,
         |    govukErrorSummary: GovukErrorSummary,
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
         |        // TODO: Add your form fields here
         |
         |        @govukButton(
         |            ButtonViewModel(messages("site.continue"))
         |        )
         |    }
         |}
         |""".stripMargin
  }
}
