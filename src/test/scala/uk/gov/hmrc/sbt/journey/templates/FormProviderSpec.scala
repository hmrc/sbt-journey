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

class FormProviderSpec extends AnyFlatSpec with Matchers {

  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

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

  "FormProvider.render" should "render a form provider for a String page" in {
    FormProvider.render(
      basePackage,
      Map.empty,
      journeyPage("serviceUrl", FieldType.STRING)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.mapping
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |
        |
        |trait ServiceUrlBaseFormProvider {
        |  def apply(): Form[String]
        |}
        |
        |class DefaultServiceUrlFormProvider
        |  extends ServiceUrlBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[String] = Form(
        |    "value" -> text("serviceUrl.error.required")
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for a Boolean page" in {
    FormProvider.render(
      basePackage,
      Map.empty,
      journeyPage("areYouSendingSamples", FieldType.BOOLEAN)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.mapping
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |
        |
        |trait AreYouSendingSamplesBaseFormProvider {
        |  def apply(): Form[Boolean]
        |}
        |
        |class DefaultAreYouSendingSamplesFormProvider
        |  extends AreYouSendingSamplesBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[Boolean] = Form(
        |    "value" -> boolean(
        |      requiredKey = "areYouSendingSamples.error.required",
        |      invalidKey = "areYouSendingSamples.error.boolean",
        |    )
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for an Int page" in {
    FormProvider.render(
      basePackage,
      Map.empty,
      journeyPage("howManySamples", FieldType.INT)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.mapping
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |
        |
        |trait HowManySamplesBaseFormProvider {
        |  def apply(): Form[Int]
        |}
        |
        |class DefaultHowManySamplesFormProvider
        |  extends HowManySamplesBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[Int] = Form(
        |    "value" -> int(
        |      requiredKey = "howManySamples.error.required",
        |      wholeNumberKey = "howManySamples.error.wholeNumber",
        |      nonNumericKey = "howManySamples.error.nonNumeric",
        |    )
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for a LocalDate page" in {
    FormProvider.render(
      basePackage,
      Map.empty,
      journeyPage("whenDidYouSendSamples", ClassType(classOf[LocalDate]))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.mapping
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import java.time.LocalDate
        |import play.api.i18n.Messages
        |
        |trait WhenDidYouSendSamplesBaseFormProvider {
        |  def apply()(using messages: Messages): Form[LocalDate]
        |}
        |
        |class DefaultWhenDidYouSendSamplesFormProvider
        |  extends WhenDidYouSendSamplesBaseFormProvider
        |  with Mappings {
        |
        |  def apply()(using messages: Messages): Form[LocalDate] = Form(
        |    "value" -> localDate(
        |      invalidKey = "whenDidYouSendSamples.error.invalid",
        |      allRequiredKey = "whenDidYouSendSamples.error.required.all",
        |      twoRequiredKey = "whenDidYouSendSamples.error.required.two",
        |      requiredKey = "whenDidYouSendSamples.error.required",
        |    )
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for an unsupported type but provide no default implementation" in {
    FormProvider.render(
      basePackage,
      Map.empty,
      journeyPage("whichDayOfWeek", ClassType(classOf[DayOfWeek]))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.mapping
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import java.time.DayOfWeek
        |
        |trait WhichDayOfWeekBaseFormProvider {
        |  def apply(): Form[DayOfWeek]
        |}
        |""".stripMargin
  }

  it should "render a form provider for an enum model" in {
    FormProvider.render(
      basePackage,
      Map("Choice" -> EnumModel("Choice", List("Yes", "No"))),
      journeyPage("areYouSendingSamples", ClassType(basePackage / "Choice"))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.mapping
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import uk.gov.hmrc.sbtjourneytest.Choice
        |
        |trait AreYouSendingSamplesBaseFormProvider {
        |  def apply(): Form[Choice]
        |}
        |
        |class DefaultAreYouSendingSamplesFormProvider
        |  extends AreYouSendingSamplesBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[Choice] = Form(
        |    "value" -> enumerable[Choice](
        |      requiredKey = "areYouSendingSamples.error.required",
        |      invalidKey = "areYouSendingSamples.error.invalid",
        |    )
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for a case class model" in {
    FormProvider.render(
      basePackage,
      Map(
        "AuditEvent" -> CaseClassModel(
          "AuditEvent",
          List(
            "auditType"          -> FieldType.STRING,
            "description"        -> FieldType.STRING,
            "expectedGoLiveDate" -> ClassType(classOf[LocalDate])
          )
        )
      ),
      journeyPage("auditEvent", ClassType(basePackage / "AuditEvent"))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.mapping
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import uk.gov.hmrc.sbtjourneytest.AuditEvent
        |import play.api.i18n.Messages
        |
        |trait AuditEventBaseFormProvider {
        |  def apply()(using messages: Messages): Form[AuditEvent]
        |}
        |
        |class DefaultAuditEventFormProvider
        |  extends AuditEventBaseFormProvider
        |  with Mappings {
        |
        |  def apply()(using messages: Messages): Form[AuditEvent] = Form(
        |    "value" -> mapping(
        |      "auditType" -> text("auditEvent.error.auditType.required"),
        |      "description" -> text("auditEvent.error.description.required"),
        |      "expectedGoLiveDate" -> localDate(
        |        invalidKey = "auditEvent.error.expectedGoLiveDate.invalid",
        |        allRequiredKey = "auditEvent.error.expectedGoLiveDate.required.all",
        |        twoRequiredKey = "auditEvent.error.expectedGoLiveDate.required.two",
        |        requiredKey = "auditEvent.error.expectedGoLiveDate.required",
        |      )
        |    )(AuditEvent.apply)(o => Some(Tuple.fromProductTyped(o)))
        |  )
        |}
        |""".stripMargin
  }
}
