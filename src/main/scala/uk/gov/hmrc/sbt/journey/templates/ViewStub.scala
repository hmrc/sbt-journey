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

import uk.gov.hmrc.sbt.journey.models.*
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.packageCase

import java.time.LocalDate

object ViewStub extends Template {
  def renderNoForm(pageName: String): String = {
    s"""@this(
       |    layout: templates.Layout
       |)
       |
       |@()(implicit request: Request[_], messages: Messages)
       |
       |@layout(
       |    pageTitle = titleNoForm(messages("$pageName.title")),
       |    showBackLink = false
       |) {
       |
       |<h1 class="govuk-heading-xl">@messages("$pageName.heading")</h1>
       |
       |<p class="govuk-body">@messages("$pageName.guidance")</p>
       |}
       |""".stripMargin
  }

  def legendFor(pageName: String, fieldName: String): String =
    if (fieldName == "value")
      s"""LegendViewModel(messages("$pageName.heading")).asPageHeading()"""
    else
      s"""LegendViewModel(messages("$pageName.$fieldName"))"""

  def labelFor(pageName: String, fieldName: String): String =
    if (fieldName == "value")
      s"""LabelViewModel(messages("$pageName.heading")).asPageHeading()"""
    else
      s"""LabelViewModel(messages("$pageName.$fieldName"))"""

  def importsFor(
    models: Map[String, AnswerModel],
    fieldType: FieldType
  ): List[String] = {
    fieldType.typeName.flatMap(models.get) match {
      case Some(EnumModel(_, _)) =>
        List.empty
      case Some(CaseClassModel(_, fields)) =>
        fields.flatMap { case (_, fieldType) =>
          importsFor(models, fieldType)
        }
      case _ =>
        fieldType match {
          case PrimitiveType(clazz) if clazz == classOf[Int] =>
            List("@import viewmodels.InputWidth._")
          case ClassType(clazz) if clazz == classOf[BigDecimal].getName =>
            List("@import viewmodels.InputWidth._")
          case ClassType(clazz) if clazz == classOf[String].getName =>
            List("@import viewmodels.InputWidth._")
          case OptionType(fieldType) =>
            importsFor(models, fieldType)
          case _ =>
            List.empty
        }
    }
  }

  def inputsFor(
    models: Map[String, AnswerModel],
    fieldType: FieldType
  ): List[String] = {
    val p = " " * 4
    fieldType.typeName.flatMap(models.get) match {
      case Some(EnumModel(_, _)) =>
        List(s"${p}govukRadios: GovukRadios,")
      case Some(CaseClassModel(_, fields)) =>
        fields.flatMap { case (_, fieldType) =>
          inputsFor(models, fieldType)
        }
      case _ =>
        fieldType match {
          case PrimitiveType(clazz) if clazz == classOf[Boolean] =>
            List(s"${p}govukRadios: GovukRadios,")
          case PrimitiveType(clazz) if clazz == classOf[Int] =>
            List(s"${p}govukInput: GovukInput,")
          case ClassType(clazz) if clazz == classOf[LocalDate].getName =>
            List(s"${p}govukDateInput: GovukDateInput,")
          case ClassType(clazz) if clazz == classOf[BigDecimal].getName =>
            List(s"${p}govukInput: GovukInput,")
          case ClassType(clazz) if clazz == classOf[String].getName =>
            List(s"${p}govukInput: GovukInput,")
          case OptionType(fieldType) =>
            inputsFor(models, fieldType)
          case _ =>
            List.empty
        }
    }
  }

  def fieldsFor(
    models: Map[String, AnswerModel],
    pageName: String,
    enclosing: String,
    fieldName: String,
    fieldType: FieldType
  ): List[String] = {
    val p           = " " * 8
    val parentField = if (enclosing.isEmpty) "" else s"$enclosing."
    fieldType.typeName.flatMap(models.get) match {
      case Some(EnumModel(enumName, choices)) =>
        val messagePrefix =
          if (enumName == "Choice") "site"
          else s"$pageName.$fieldName"

        val items = choices.map { choice =>
          val p = " " * 20
          s"""|${p}RadioItem(
              |${p}    id    = Some("value-${packageCase(choice)}"),
              |${p}    value = Some("$choice"),
              |${p}    content = Text(messages("$messagePrefix.${packageCase(choice)}"))
              |${p})""".stripMargin
        }

        val radios =
          s"""|$p@govukRadios(
              |$p    RadiosViewModel(
              |$p        field = form("$parentField$fieldName"),
              |$p        legend = ${legendFor(pageName, fieldName)},
              |$p        items = List(
              |${items.mkString("," + NL)}
              |$p        )
              |$p    )
              |$p)""".stripMargin

        List(radios)

      case Some(CaseClassModel(_, fields)) =>
        fields.flatMap { case (subFieldName, subFieldType) =>
          val newEnclosing =
            if (enclosing.isEmpty) fieldName
            else s"$enclosing.$fieldName"
          fieldsFor(models, pageName, newEnclosing, subFieldName, subFieldType)
        }
      case _ =>
        fieldType match {
          case OptionType(fieldType) =>
            fieldsFor(models, pageName, enclosing, fieldName, fieldType)

          case PrimitiveType(clazz) if clazz == classOf[Boolean] =>
            val radios =
              s"""|$p@govukRadios(
                  |$p    RadiosViewModel.yesNo(
                  |$p        field = form("$parentField$fieldName"),
                  |$p        legend = ${legendFor(pageName, fieldName)},
                  |$p    )
                  |$p)""".stripMargin

            List(radios)

          case PrimitiveType(clazz) if clazz == classOf[Int] =>
            val input =
              s"""|$p@govukInput(
                  |$p    InputViewModel(
                  |$p        field = form("$parentField$fieldName"),
                  |$p        label = ${labelFor(pageName, fieldName)}
                  |$p    )
                  |$p    .asNumeric()
                  |$p    .withWidth(Fixed10)
                  |$p)""".stripMargin

            List(input)

          case ClassType(clazz) if clazz == classOf[LocalDate].getName =>
            val hint =
              if (fieldName == "value") s"$pageName.hint"
              else s"$pageName.$fieldName.hint"

            val input =
              s"""|$p@govukDateInput(
                  |$p    DateViewModel(
                  |$p        field  = form("$parentField$fieldName"),
                  |$p        legend = ${legendFor(pageName, fieldName)}
                  |$p    )
                  |$p    .withHint(HintViewModel(messages("$hint")))
                  |$p)""".stripMargin

            List(input)

          case ClassType(clazz) if clazz == classOf[BigDecimal].getName =>
            val input =
              s"""|$p@govukInput(
                  |$p    InputViewModel(
                  |$p        field = form("$parentField$fieldName"),
                  |$p        label = ${labelFor(pageName, fieldName)}
                  |$p    )
                  |$p    .withPrefix(PrefixOrSuffix(content = "£"))
                  |$p    .withWidth(Fixed10)
                  |$p)""".stripMargin

            List(input)

          case ClassType(clazz) if clazz == classOf[String].getName =>
            val input =
              s"""|$p@govukInput(
                  |$p    InputViewModel(
                  |$p        field = form("$parentField$fieldName"),
                  |$p        label = ${labelFor(pageName, fieldName)}
                  |$p    )
                  |$p    .withWidth(Full)
                  |$p)""".stripMargin

            List(input)

          case _ =>
            val inputType = ModelFields.fieldType(fieldType)
            val input =
              s"""|$p@* TODO: Add an input for "$parentField$fieldName" - there is no default input for $inputType fields *@"""

            List(input)
        }
    }
  }

  def renderForm(models: Map[String, AnswerModel], page: JourneyPage): String = {
    val pageName   = page.pageKey
    val answerType = page.answerType

    val imports = {
      val imports = importsFor(models, answerType)
      if (imports.isEmpty)
        ""
      else
        imports.distinct.mkString("", NL, NL * 2)
    }

    val inputs = {
      val inputs = inputsFor(models, answerType)
      if (inputs.isEmpty)
        ""
      else
        inputs.distinct.mkString(NL, NL, "")
    }

    val fields = fieldsFor(models, pageName, "", "value", answerType)

    val heading =
      if (fields.length == 1) ""
      else
        s"""        <h1 class="govuk-heading-xl">@messages("$pageName.heading")</h1>${NL * 2}"""

      s"""$imports@this(
       |    layout: templates.Layout,
       |    formHelper: FormWithCSRF,
       |    govukErrorSummary: GovukErrorSummary,$inputs
       |    govukButton: GovukButton
       |)
       |
       |@(form: Form[_], action: Call, mode: Mode)(implicit request: Request[_], messages: Messages)
       |
       |@layout(pageTitle = title(form, messages("$pageName.title"))) {
       |
       |    @formHelper(action = action, Symbol("autoComplete") -> "off") {
       |        @if(form.errors.nonEmpty) {
       |            @govukErrorSummary(ErrorSummaryViewModel(form))
       |        }
       |
       |$heading${fields.mkString(NL * 2)}
       |
       |        @govukButton(
       |            ButtonViewModel(messages("site.continue"))
       |        )
       |    }
       |}
       |""".stripMargin
  }
}
