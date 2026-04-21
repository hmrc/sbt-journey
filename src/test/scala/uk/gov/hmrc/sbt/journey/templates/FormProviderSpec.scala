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

class FormProviderSpec extends AnyFlatSpec with Matchers {

  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  def journeyConfig(journey: (String, Journey)) =
    JourneyConfig(
      basePackage.toString,
      Map.empty,
      Map.empty,
      Map(journey)
    )

  def journeyPage(pageKey: String, answerType: FieldType, withDefaultFormProvider: Boolean = true) =
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
      withDefaultFormProvider,
      answerType
    )

  "FormProvider.baseProvider" should "render a form provider for a String page" in {
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("serviceUrl", FieldType.STRING)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
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

  it should "render a form provider without a default implementation if requested" in {
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("areYouSendingSamples", FieldType.BOOLEAN, withDefaultFormProvider = false)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |
        |
        |trait AreYouSendingSamplesBaseFormProvider {
        |  def apply(): Form[Boolean]
        |}
        |""".stripMargin
  }

  it should "render a form provider for a Boolean page" in {
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("areYouSendingSamples", FieldType.BOOLEAN)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
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
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("howManySamples", FieldType.INT)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
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

  it should "render a form provider for a BigDecimal page" in {
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("whatIsTheValuation", FieldType.BIGDECIMAL)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import scala.math.BigDecimal
        |
        |trait WhatIsTheValuationBaseFormProvider {
        |  def apply(): Form[BigDecimal]
        |}
        |
        |class DefaultWhatIsTheValuationFormProvider
        |  extends WhatIsTheValuationBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[BigDecimal] = Form(
        |    "value" -> currency(
        |      requiredKey = "whatIsTheValuation.error.required",
        |      invalidNumeric = "whatIsTheValuation.error.invalidNumeric",
        |      nonNumericKey = "whatIsTheValuation.error.nonNumeric",
        |    )
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for a LocalDate page" in {
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("whenDidYouSendSamples", FieldType.LOCALDATE)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
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

  it should "render a form provider for an optional LocalDate page" in {
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("whenDidYouSendSamples", OptionType(FieldType.LOCALDATE))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import java.time.LocalDate
        |import play.api.i18n.Messages
        |
        |trait WhenDidYouSendSamplesBaseFormProvider {
        |  def apply()(using messages: Messages): Form[Option[LocalDate]]
        |}
        |
        |class DefaultWhenDidYouSendSamplesFormProvider
        |  extends WhenDidYouSendSamplesBaseFormProvider
        |  with Mappings {
        |
        |  def apply()(using messages: Messages): Form[Option[LocalDate]] = Form(
        |    "value" -> optional(localDate(
        |      invalidKey = "whenDidYouSendSamples.error.invalid",
        |      allRequiredKey = "whenDidYouSendSamples.error.required.all",
        |      twoRequiredKey = "whenDidYouSendSamples.error.required.two",
        |      requiredKey = "whenDidYouSendSamples.error.required",
        |    ))
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for an unsupported type but provide no default implementation" in {
    FormProvider.baseProvider(
      basePackage,
      Map.empty,
      journeyPage("whichDayOfWeek", ClassType(classOf[DayOfWeek]))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import java.time.DayOfWeek
        |
        |trait WhichDayOfWeekBaseFormProvider {
        |  def apply(): Form[DayOfWeek]
        |}
        |""".stripMargin
  }

  it should "render a form provider for an enum model" in {
    FormProvider.baseProvider(
      basePackage,
      Map("Choice" -> EnumModel("Choice", List("Yes", "No"))),
      journeyPage("areYouSendingSamples", ClassType(basePackage / "Choice"))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
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

  it should "render a form provider for a set of enum model" in {
    FormProvider.baseProvider(
      basePackage,
      Map(
        "SurvivedBy" -> EnumModel(
          "SurvivedBy",
          List("SPOUSE", "SIBLING", "PARENT", "CHILDREN", "GRANDCHILDREN")
        )
      ),
      journeyPage("whoSurvivesDeceased", SetType(ClassType(basePackage / "models" / "SurvivedBy")))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import uk.gov.hmrc.sbtjourneytest.models.SurvivedBy
        |
        |trait WhoSurvivesDeceasedBaseFormProvider {
        |  def apply(): Form[Set[SurvivedBy]]
        |}
        |
        |class DefaultWhoSurvivesDeceasedFormProvider
        |  extends WhoSurvivesDeceasedBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[Set[SurvivedBy]] = Form(
        |    "value" -> set(enumerable[SurvivedBy](
        |      requiredKey = "whoSurvivesDeceased.error.required",
        |      invalidKey = "whoSurvivesDeceased.error.invalid",
        |    ))
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for a case class model" in {
    FormProvider.baseProvider(
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
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
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
        |      ),
        |      "expectedDecommissioningDate" -> optional(localDate(
        |        invalidKey = "auditEvent.error.expectedDecommissioningDate.invalid",
        |        allRequiredKey = "auditEvent.error.expectedDecommissioningDate.required.all",
        |        twoRequiredKey = "auditEvent.error.expectedDecommissioningDate.required.two",
        |        requiredKey = "auditEvent.error.expectedDecommissioningDate.required",
        |      ))
        |    )(AuditEvent.apply)(o => Some(Tuple.fromProductTyped(o)))
        |  )
        |}
        |""".stripMargin
  }

  "FormProvider.providerStub" should "render a form provider for a String page" in {
    FormProvider.providerStub(
      basePackage,
      Map.empty,
      journeyPage("serviceUrl", FieldType.STRING)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |
        |
        |class ServiceUrlFormProvider
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
    FormProvider.providerStub(
      basePackage,
      Map.empty,
      journeyPage("areYouSendingSamples", FieldType.BOOLEAN)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |
        |
        |class AreYouSendingSamplesFormProvider
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
    FormProvider.providerStub(
      basePackage,
      Map.empty,
      journeyPage("howManySamples", FieldType.INT)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |
        |
        |class HowManySamplesFormProvider
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

  it should "render a form provider for a BigDecimal page" in {
    FormProvider.providerStub(
      basePackage,
      Map.empty,
      journeyPage("whatIsTheValuation", FieldType.BIGDECIMAL)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import scala.math.BigDecimal
        |
        |class WhatIsTheValuationFormProvider
        |  extends WhatIsTheValuationBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[BigDecimal] = Form(
        |    "value" -> currency(
        |      requiredKey = "whatIsTheValuation.error.required",
        |      invalidNumeric = "whatIsTheValuation.error.invalidNumeric",
        |      nonNumericKey = "whatIsTheValuation.error.nonNumeric",
        |    )
        |  )
        |}
        |""".stripMargin
  }
  it should "render a form provider for a LocalDate page" in {
    FormProvider.providerStub(
      basePackage,
      Map.empty,
      journeyPage("whenDidYouSendSamples", FieldType.LOCALDATE)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import java.time.LocalDate
        |import play.api.i18n.Messages
        |
        |class WhenDidYouSendSamplesFormProvider
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

  it should "render a form provider for an optional LocalDate page" in {
    FormProvider.providerStub(
      basePackage,
      Map.empty,
      journeyPage("whenDidYouSendSamples", OptionType(FieldType.LOCALDATE))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import java.time.LocalDate
        |import play.api.i18n.Messages
        |
        |class WhenDidYouSendSamplesFormProvider
        |  extends WhenDidYouSendSamplesBaseFormProvider
        |  with Mappings {
        |
        |  def apply()(using messages: Messages): Form[Option[LocalDate]] = Form(
        |    "value" -> optional(localDate(
        |      invalidKey = "whenDidYouSendSamples.error.invalid",
        |      allRequiredKey = "whenDidYouSendSamples.error.required.all",
        |      twoRequiredKey = "whenDidYouSendSamples.error.required.two",
        |      requiredKey = "whenDidYouSendSamples.error.required",
        |    ))
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for an unsupported type but provide no default mappings" in {
    FormProvider.providerStub(
      basePackage,
      Map.empty,
      journeyPage("whichDayOfWeek", ClassType(classOf[DayOfWeek]))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import java.time.DayOfWeek
        |
        |class WhichDayOfWeekFormProvider
        |  extends WhichDayOfWeekBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[DayOfWeek] = Form(
        |    "value" -> ??? /* TODO: There are no default mappings for DayOfWeek */
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for an enum model" in {
    FormProvider.providerStub(
      basePackage,
      Map("Choice" -> EnumModel("Choice", List("Yes", "No"))),
      journeyPage("areYouSendingSamples", ClassType(basePackage / "Choice"))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import uk.gov.hmrc.sbtjourneytest.Choice
        |
        |class AreYouSendingSamplesFormProvider
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

  it should "render a form provider for a set of enum model" in {
    FormProvider.providerStub(
      basePackage,
      Map(
        "SurvivedBy" -> EnumModel(
          "SurvivedBy",
          List("SPOUSE", "SIBLING", "PARENT", "CHILDREN", "GRANDCHILDREN")
        )
      ),
      journeyPage("whoSurvivesDeceased", SetType(ClassType(basePackage / "models" / "SurvivedBy")))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import uk.gov.hmrc.sbtjourneytest.models.SurvivedBy
        |
        |class WhoSurvivesDeceasedFormProvider
        |  extends WhoSurvivesDeceasedBaseFormProvider
        |  with Mappings {
        |
        |  def apply(): Form[Set[SurvivedBy]] = Form(
        |    "value" -> set(enumerable[SurvivedBy](
        |      requiredKey = "whoSurvivesDeceased.error.required",
        |      invalidKey = "whoSurvivesDeceased.error.invalid",
        |    ))
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for a case class model" in {
    FormProvider.providerStub(
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
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import uk.gov.hmrc.sbtjourneytest.AuditEvent
        |import play.api.i18n.Messages
        |
        |class AuditEventFormProvider
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
        |      ),
        |      "expectedDecommissioningDate" -> optional(localDate(
        |        invalidKey = "auditEvent.error.expectedDecommissioningDate.invalid",
        |        allRequiredKey = "auditEvent.error.expectedDecommissioningDate.required.all",
        |        twoRequiredKey = "auditEvent.error.expectedDecommissioningDate.required.two",
        |        requiredKey = "auditEvent.error.expectedDecommissioningDate.required",
        |      ))
        |    )(AuditEvent.apply)(o => Some(Tuple.fromProductTyped(o)))
        |  )
        |}
        |""".stripMargin
  }

  it should "render a form provider for a case class model with an unsupported field type" in {
    FormProvider.providerStub(
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
      """package uk.gov.hmrc.sbtjourneytest.forms
        |
        |import play.api.data.Form
        |import play.api.data.Forms.{mapping,optional,set}
        |import _root_.forms.mappings.Mappings // uk.gov.hmrc.sbtjourneytest.forms.mappings.Mappings
        |import uk.gov.hmrc.sbtjourneytest.AuditEvent
        |import play.api.i18n.Messages
        |
        |class AuditEventFormProvider
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
        |      ),
        |      "expectedGoLiveDayOfWeek" -> ??? /* TODO: There are no default mappings for DayOfWeek */
        |    )(AuditEvent.apply)(o => Some(Tuple.fromProductTyped(o)))
        |  )
        |}
        |""".stripMargin
  }

  "FormProvider.module" should "generate a Guice module that binds default form providers" in {
    val cipAssessmentTicket =
      journeyPage("cipAssessmentTicket", FieldType.STRING)
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    // Pages without default form provider implementations should be skipped
    val serviceUrlPage =
      journeyPage("serviceUrl", FieldType.STRING, withDefaultFormProvider = false)

    // Pages with unsupported answer types should be skipped
    val whichDayOfWeek =
      journeyPage("whichDayOfWeek", ClassType(classOf[DayOfWeek]))

    FormProvider.module(
      journeyConfig(
        "submission" -> Journey(
          pages = Map(
            "cipAssessmentTicket" -> cipAssessmentTicket,
            "cipAssessmentPage"   -> cipAssessmentPage,
            "serviceUrl"          -> serviceUrlPage,
            "whichDayOfWeek"      -> whichDayOfWeek
          ),
          journey = List.empty
        )
      )
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.config
        |
        |import com.google.inject.AbstractModule
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |
        |class DefaultFormProvidersModule extends AbstractModule {
        |  override def configure(): Unit = {
        |    bind(classOf[CipAssessmentTicketBaseFormProvider]).to(classOf[DefaultCipAssessmentTicketFormProvider])
        |    bind(classOf[CipAssessmentPageBaseFormProvider]).to(classOf[DefaultCipAssessmentPageFormProvider])
        |  }
        |}
        |""".stripMargin
  }
}
