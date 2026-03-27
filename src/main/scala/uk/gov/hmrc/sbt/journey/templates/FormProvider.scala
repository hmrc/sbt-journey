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

object FormProvider {
  private def hasMappingsFor(models: Map[String, AnswerModel], fieldType: FieldType): Boolean = {
    fieldType.typeName.flatMap(models.get) match {
      case Some(CaseClassModel(_, fields)) =>
        fields.forall { case (_, typ) => hasMappingsFor(models, typ) }
      case Some(EnumModel(_, _)) => true
      case _ =>
        fieldType match {
          case PrimitiveType(clazz) =>
            Set[Class[? <: AnyVal]](classOf[Int], classOf[Boolean]).contains(clazz)
          case ClassType(clazz) =>
            Set(classOf[LocalDate].getName, classOf[String].getName).contains(clazz)
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
    fieldType: FieldType
  ): String = {
    val p        = " " * indent
    val subField = if (enclosing.isEmpty) "" else s"$enclosing."
    fieldType.typeName.flatMap(models.get) match {
      case Some(EnumModel(modelName, _)) =>
        s"""|$p"$fieldName" -> enumerable[$modelName](
            |$p  requiredKey = "$pageName.error.${subField}required",
            |$p  invalidKey = "$pageName.error.${subField}invalid",
            |$p)""".stripMargin
      case Some(CaseClassModel(modelName, fields)) =>
        s"""|${p}mapping(
            |${fields
             .map { case (fieldName, fieldType) =>
               val subField = if (enclosing.isEmpty) fieldName else s"$enclosing.$fieldName"
               mappingsFor(models, pageName, indent + 2, subField, fieldName, fieldType)
             }
             .mkString("," + System.lineSeparator())}
            |$p)($modelName.apply)(o => Some(Tuple.fromProductTyped(o)))""".stripMargin
      case _ =>
        fieldType match {
          case PrimitiveType(clazz) if clazz == classOf[Boolean] =>
            s"""|$p"$fieldName" -> boolean(
                |$p  requiredKey = "$pageName.error.${subField}required",
                |$p  invalidKey = "$pageName.error.${subField}boolean",
                |$p)""".stripMargin
          case PrimitiveType(clazz) if clazz == classOf[Int] =>
            s"""|$p"$fieldName" -> int(
                |$p  requiredKey = "$pageName.error.required",
                |$p  wholeNumberKey = "$pageName.error.${subField}wholeNumber",
                |$p  nonNumericKey = "$pageName.error.${subField}nonNumeric",
                |$p)""".stripMargin
          case ClassType(clazz) if clazz == classOf[LocalDate].getName =>
            s"""|$p"$fieldName" -> localDate(
                |$p  invalidKey = "$pageName.error.${subField}invalid",
                |$p  allRequiredKey = "$pageName.error.${subField}required.all",
                |$p  twoRequiredKey = "$pageName.error.${subField}required.two",
                |$p  requiredKey = "$pageName.error.${subField}required",
                |$p)""".stripMargin
          case ClassType(clazz) if clazz == classOf[String].getName =>
            s"""$p"$fieldName" -> text("$pageName.error.${subField}required")""".stripMargin
        }
    }

  }

  def render(
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
       |import play.api.data.Forms.mapping
       |import _root_.forms.mappings.Mappings // ${basePackage / "forms.mappings.Mappings"}
       |$imports
       |
       |trait ${pascalCase(pageName)}BaseFormProvider {
       |  def apply()$applyParams: Form[$fieldType]
       |}${
        if (!hasMappingsFor(models, answerType)) ""
        else
          s"""|
              |
              |class Default${pascalCase(pageName)}FormProvider
              |  extends ${pascalCase(pageName)}BaseFormProvider
              |  with Mappings {
              |
              |  def apply()$applyParams: Form[$fieldType] = Form(
              |${mappingsFor(models, pageName, indent = 4, "", "value", answerType)}
              |  )
              |}""".stripMargin
      }
       |""".stripMargin
  }
}
