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

import uk.gov.hmrc.sbt.journey.models.{AnswerModel, CaseClassModel, EnumModel, QualifiedName}
import uk.gov.hmrc.sbt.journey.templates.Imports.PlayJsonPrefix
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.camelCase

object CustomModel {
  def render(basePackage: QualifiedName, model: AnswerModel): String = model match {
    case model: CaseClassModel => caseClassModel(basePackage, model)
    case model: EnumModel      => enumModel(basePackage, model)
  }

  private def enumRead(caseName: String): String = {
    val nm = camelCase(caseName)
    s"""|        case $nm if $nm == config.typeNaming("$caseName") =>
        |          JsSuccess($caseName)""".stripMargin
  }

  private def enumWrite(caseName: String): String = {
    s"""|    case $caseName =>
        |      Json.obj(config.discriminator -> config.typeNaming("$caseName"))""".stripMargin
  }

  private[templates] def enumModel(basePackage: QualifiedName, enumModel: EnumModel): String = {
    val modelsPackage = basePackage / "models"
    val name          = enumModel.name
    val cases         = enumModel.cases

    val imports = Imports.importsFor(
      modelsPackage,
      Map(
        PlayJsonPrefix -> Set(
          "Json",
          "JsonConfiguration",
          "JsError",
          "JsObject",
          "JsPath",
          "JsSuccess",
          "JsValue",
          "Format",
          "Reads",
          "Writes"
        )
      )
    )

    s"""package $modelsPackage
       |
       |import models.Enumerable // import ${basePackage / "models.Enumerable"}
       |$imports
       |
       |enum $name {
       |  case ${cases.mkString(", ")}
       |}
       |
       |object $name {
       |  given reads(using config: JsonConfiguration): Reads[$name] = Reads {
       |    case obj: JsObject => obj.value.get(config.discriminator) match {
       |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
       |${cases.map(enumRead).mkString(System.lineSeparator())}
       |        case _ =>
       |          JsError("error.invalid")
       |      }
       |      case _ => JsError(JsPath \\ config.discriminator, "error.missing.path")
       |    }
       |    case _ => JsError("error.expected.jsobject")
       |  }
       |
       |  given writes(using config: JsonConfiguration): Writes[$name] = Writes {
       |${cases.map(enumWrite).mkString(System.lineSeparator())}
       |  }
       |
       |  given Format[$name] = Format(reads, writes)
       |
       |  given Enumerable[$name] = (value: String) => fromString(value)
       |
       |  def fromString(value: String): Option[$name] =
       |    values.find(_.toString == value)
       |}
       |""".stripMargin
  }

  private[templates] def caseClassModel(
    basePackage: QualifiedName,
    caseClassModel: CaseClassModel
  ): String = {
    val modelsPackage = basePackage / "models"
    val name          = caseClassModel.name
    val fields        = caseClassModel.fields

    val importPrefixes = Imports.importedSymbols(fields)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    s"""package $modelsPackage
       |
       |import play.api.libs.json.{Json, Format}
       |$imports
       |
       |case class $name(
       |${fields.map((ModelFields.field _).tupled).mkString("," + System.lineSeparator())}
       |)
       |
       |object $name $extendsClause{
       |  given Format[$name] = Json.format[$name]
       |}
       |""".stripMargin
  }

}
