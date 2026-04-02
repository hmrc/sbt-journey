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
import uk.gov.hmrc.sbt.journey.templates.Imports.PlayI18nPrefix
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.pascalCase

import java.time.LocalDate

object FormProvider extends Template {
  private def hasMappingsFor(
    models: Map[String, AnswerModel],
    fieldType: FieldType
  ): Boolean = {
    fieldType.typeName.flatMap(models.get) match {
      case Some(CaseClassModel(_, fields)) =>
        fields.forall { case (_, typ) => hasMappingsFor(models, typ) }
      case Some(EnumModel(_, _)) => true
      case _ =>
        fieldType match {
          case OptionType(fieldType) =>
            hasMappingsFor(models, fieldType)
          case PrimitiveType(clazz) =>
            Set[Class[? <: AnyVal]](classOf[Int], classOf[Boolean]).contains(clazz)
          case ClassType(clazz) =>
            Set(classOf[LocalDate], classOf[BigDecimal], classOf[String])
              .map(_.getName)
              .contains(clazz)
          case _ => false
        }
    }
  }

  private def hasLocalDateField(
    models: Map[String, AnswerModel],
    fieldType: FieldType
  ): Boolean = {
    fieldType match {
      // The fieldType itself is LocalDate
      case ClassType(clazz) if clazz == classOf[LocalDate].getName =>
        true
      // It's an optional LocalDate
      case OptionType(fieldType) =>
        hasLocalDateField(models, fieldType)
      case _ =>
        // It's a model which has a LocalDate field
        models
          .get(ModelFields.fieldType(fieldType))
          .collect { case CaseClassModel(_, fields) =>
            fields.exists { case (_, field) => hasLocalDateField(models, field) }
          }
          .getOrElse(false)
    }
  }

  private def mappingsFor(
    models: Map[String, AnswerModel],
    pageName: String,
    indent: Int,
    enclosing: String,
    fieldName: String,
    subFields: List[(String, FieldType)]
  ): String = subFields
    .map { case (subFieldName, subFieldType) =>
      val p = " " * indent
      val newEnclosing =
        if (fieldName == "value") ""
        else if (enclosing.isEmpty) fieldName
        else s"$enclosing.$fieldName"
      s"""$p"$subFieldName" -> ${mappingsFor(
          models,
          pageName,
          indent,
          newEnclosing,
          subFieldName,
          subFieldType
        )}"""
    }
    .mkString("," + NL)

  private def mappingsFor(
    models: Map[String, AnswerModel],
    pageName: String,
    indent: Int,
    enclosing: String,
    fieldName: String,
    fieldType: FieldType
  ): String = {
    val p           = " " * indent
    val parentField = if (enclosing.isEmpty) "" else s"$enclosing."
    val subField    = if (fieldName == "value") "" else s"$fieldName."
    fieldType.typeName.flatMap(models.get) match {
      case Some(EnumModel(modelName, _)) =>
        s"""|enumerable[$modelName](
            |$p  requiredKey = "$pageName.error.$parentField${subField}required",
            |$p  invalidKey = "$pageName.error.$parentField${subField}invalid",
            |$p)""".stripMargin
      case Some(CaseClassModel(modelName, fields)) =>
        s"""|mapping(
            |${mappingsFor(models, pageName, indent + 2, enclosing, fieldName, fields)}
            |$p)($modelName.apply)(o => Some(Tuple.fromProductTyped(o)))""".stripMargin
      case _ =>
        fieldType match {
          case PrimitiveType(clazz) if clazz == classOf[Boolean] =>
            s"""|boolean(
                |$p  requiredKey = "$pageName.error.${parentField}${subField}required",
                |$p  invalidKey = "$pageName.error.${parentField}${subField}boolean",
                |$p)""".stripMargin
          case PrimitiveType(clazz) if clazz == classOf[Int] =>
            s"""|int(
                |$p  requiredKey = "$pageName.error.required",
                |$p  wholeNumberKey = "$pageName.error.${parentField}${subField}wholeNumber",
                |$p  nonNumericKey = "$pageName.error.${parentField}${subField}nonNumeric",
                |$p)""".stripMargin
          case ClassType(clazz) if clazz == classOf[LocalDate].getName =>
            s"""|localDate(
                |$p  invalidKey = "$pageName.error.${parentField}${subField}invalid",
                |$p  allRequiredKey = "$pageName.error.${parentField}${subField}required.all",
                |$p  twoRequiredKey = "$pageName.error.${parentField}${subField}required.two",
                |$p  requiredKey = "$pageName.error.${parentField}${subField}required",
                |$p)""".stripMargin
          case ClassType(clazz) if clazz == classOf[BigDecimal].getName =>
            s"""|currency(
                |$p  requiredKey = "$pageName.error.required",
                |$p  invalidNumericKey = "$pageName.error.${parentField}${subField}invalidNumeric",
                |$p  nonNumericKey = "$pageName.error.${parentField}${subField}nonNumeric",
                |$p)""".stripMargin
          case ClassType(clazz) if clazz == classOf[String].getName =>
            s"""text("$pageName.error.${parentField}${subField}required")""".stripMargin
          case OptionType(fieldType) =>
            s"optional(${mappingsFor(models, pageName, indent, enclosing, fieldName, fieldType)})"
          case _ =>
            s"??? /* TODO: There are no default mappings for ${ModelFields.fieldType(fieldType)} */"
        }
    }
  }

  def baseProvider(
    basePackage: QualifiedName,
    models: Map[String, AnswerModel],
    journeyPage: JourneyPage
  ): String = {
    val formsPackage = basePackage / "forms"
    val pageName     = journeyPage.pageKey
    val answerType   = journeyPage.answerType
    val withDefault  = journeyPage.withDefaultFormProvider
    val fieldType    = ModelFields.fieldType(answerType)

    val answerImports   = Imports.importedSymbols(answerType)
    val usesLocalDate   = hasLocalDateField(models, answerType)
    val messagesImports = if (usesLocalDate) Map(PlayI18nPrefix -> Set("Messages")) else Map.empty
    val imports =
      Imports.importsFor(formsPackage, answerImports ++ messagesImports, addFormatImports = false)

    // The localDate form Mapping requires Messages
    val applyParams = if (usesLocalDate) "(using messages: Messages)" else ""

    val defaultImpl =
      if (!withDefault || !hasMappingsFor(models, answerType)) ""
      else
        s"""
           |class Default${pascalCase(pageName)}FormProvider
           |  extends ${pascalCase(pageName)}BaseFormProvider
           |  with Mappings {
           |
           |  def apply()$applyParams: Form[$fieldType] = Form(
           |    "value" -> ${mappingsFor(models, pageName, indent = 4, "", "value", answerType)}
           |  )
           |}
           |""".stripMargin

    s"""package $formsPackage
       |
       |import play.api.data.Form
       |import play.api.data.Forms.{mapping,optional}
       |import _root_.forms.mappings.Mappings // ${basePackage / "forms.mappings.Mappings"}
       |$imports
       |
       |trait ${pascalCase(pageName)}BaseFormProvider {
       |  def apply()$applyParams: Form[$fieldType]
       |}
       |$defaultImpl""".stripMargin
  }

  def providerStub(
    basePackage: QualifiedName,
    models: Map[String, AnswerModel],
    journeyPage: JourneyPage
  ): String = {
    val formsPackage = basePackage / "forms"
    val pageName     = journeyPage.pageKey
    val answerType   = journeyPage.answerType
    val fieldType    = ModelFields.fieldType(answerType)

    val answerImports   = Imports.importedSymbols(answerType)
    val usesLocalDate   = hasLocalDateField(models, answerType)
    val messagesImports = if (usesLocalDate) Map(PlayI18nPrefix -> Set("Messages")) else Map.empty
    val imports =
      Imports.importsFor(formsPackage, answerImports ++ messagesImports, addFormatImports = false)

    // The localDate form Mapping requires Messages
    val applyParams = if (usesLocalDate) "(using messages: Messages)" else ""

    s"""package $formsPackage
       |
       |import play.api.data.Form
       |import play.api.data.Forms.{mapping,optional}
       |import _root_.forms.mappings.Mappings // ${basePackage / "forms.mappings.Mappings"}
       |$imports
       |
       |class ${pascalCase(pageName)}FormProvider
       |  extends ${pascalCase(pageName)}BaseFormProvider
       |  with Mappings {
       |
       |  def apply()$applyParams: Form[$fieldType] = Form(
       |    "value" -> ${mappingsFor(models, pageName, indent = 4, "", "value", answerType)}
       |  )
       |}
       |""".stripMargin
  }

  def module(config: JourneyConfig): String = {
    val basePackage   = QualifiedName(config.basePackage)
    val configPackage = basePackage / "config"
    val indent        = " " * 4

    val bindings = config.journeys.flatMap { case (_, journey) =>
      journey.pages.flatMap { case (pageName, page) =>
        val baseProvider    = s"${pascalCase(pageName)}BaseFormProvider"
        val defaultProvider = s"Default${pascalCase(pageName)}FormProvider"
        val hasMappings     = FormProvider.hasMappingsFor(config.models, page.answerType)
        if (page.withDefaultFormProvider && hasMappings)
          List(s"${indent}bind(classOf[$baseProvider]).to(classOf[$defaultProvider])")
        else
          List.empty
      }
    }

    s"""package $configPackage
       |
       |import com.google.inject.AbstractModule
       |import ${basePackage / "forms.*"}
       |
       |class DefaultFormProvidersModule extends AbstractModule {
       |  override def configure(): Unit = {
       |${bindings.mkString(NL)}
       |  }
       |}
       |""".stripMargin
  }
}
