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

import uk.gov.hmrc.sbt.journey.models.{FieldType, QualifiedName}
import uk.gov.hmrc.sbt.journey.templates.Imports.PlayJsonPrefix
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.camelCase

object JourneyModel {
  private def switchCase(caseName: String, fields: List[(String, FieldType)]) = {
    if (fields.isEmpty)
      s"  case $caseName"
    else
      s"""|  case $caseName(
          |  ${fields
           .map((ModelFields.field _).tupled)
           .mkString("," + System.lineSeparator() + (" " * 2))}
          |  )""".stripMargin
  }

  private def switchCaseReads(caseName: String, fields: List[(String, FieldType)]): String = {
    val nm = camelCase(caseName)
    if (fields.isEmpty) ""
    else
      s"""  val ${nm}Reads: Reads[$caseName] = Json.reads[$caseName]
         |  val nested${caseName}Reads: Reads[$caseName] = Reads.at(JsPath \\ "$caseName")(${nm}Reads)
         |""".stripMargin
  }

  private def switchCaseWrites(caseName: String, fields: List[(String, FieldType)]): String = {
    if (fields.isEmpty) ""
    else s"  val ${camelCase(caseName)}Writes: Writes[$caseName] = Json.writes[$caseName]"
  }

  private def switchCaseRead(
    caseName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val nm = camelCase(caseName)
    if (fields.isEmpty)
      s"""|        case $nm if $nm == config.typeNaming("$caseName") =>
          |          JsSuccess($caseName)""".stripMargin
    else
      s"""|        case $nm if $nm == config.typeNaming("$caseName") =>
          |          nested${caseName}Reads.reads(obj)""".stripMargin
  }

  private def switchCaseWrite(
    caseName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val nm = camelCase(caseName)
    if (fields.isEmpty)
      s"""|    case $nm: $caseName =>
          |      Json.obj(config.discriminator -> config.typeNaming("$caseName"))""".stripMargin
    else
      s"""|    case $nm: $caseName =>
          |      Json.obj(config.discriminator -> config.typeNaming("$caseName"), "$caseName" -> ${nm}Writes.writes($nm))""".stripMargin
  }

  def forSwitchCase(
    modelsPackage: QualifiedName,
    modelName: String,
    modelCases: Map[String, List[(String, FieldType)]]
  ): String = {
    val playImports = Map(
      PlayJsonPrefix -> Set(
        "Json",
        "JsonConfiguration",
        "JsError",
        "JsObject",
        "JsPath",
        "JsValue",
        "Format",
        "Reads",
        "Writes"
      )
    )

    val importPrefixes = playImports ++ Imports.importedSymbols(modelCases.values.toList.flatten)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    val caseReads          = modelCases.map((switchCaseReads _).tupled)
    val caseWrites         = modelCases.map((switchCaseWrites _).tupled)
    val caseReadsAndWrites = (caseReads ++ caseWrites).toList.filterNot(_.isBlank)

    val subtypeReadsWrites =
      if (caseReadsAndWrites.isEmpty) ""
      else
        caseReadsAndWrites.mkString(
          System.lineSeparator(),
          System.lineSeparator(),
          System.lineSeparator()
        )

    s"""package $modelsPackage
       |
       |$imports
       |
       |enum $modelName {
       |${modelCases.map((switchCase _).tupled).mkString(System.lineSeparator())}
       |}
       |
       |object $modelName $extendsClause{$subtypeReadsWrites
       |  given reads(using config: JsonConfiguration): Reads[$modelName] = Reads {
       |    case obj: JsObject => obj.value.get(config.discriminator) match {
       |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
       |${modelCases
        .map((switchCaseRead _).tupled)
        .mkString(System.lineSeparator())}
       |        case _ =>
       |          JsError("error.invalid")
       |      }
       |      case _ => JsError(JsPath \\ config.discriminator, "error.missing.path")
       |    }
       |    case _ => JsError("error.expected.jsobject")
       |  }
       |
       |  given writes(using config: JsonConfiguration): Writes[$modelName] = Writes {
       |${modelCases
        .map((switchCaseWrite _).tupled)
        .mkString(System.lineSeparator())}
       |  }
       |
       |  given Format[$modelName] = Format(reads, writes)
       |}
       |""".stripMargin
  }

  def forIfThen(
    modelsPackage: QualifiedName,
    modelName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val playImports = Map(
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

    val importPrefixes = playImports ++ Imports.importedSymbols(fields)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    s"""package $modelsPackage
       |
       |$imports
       |
       |enum $modelName {
       |  case Yes(
       |  ${fields.map((ModelFields.field _).tupled).mkString("," + System.lineSeparator() + "  ")}
       |  )
       |  case No
       |
       |  def choice: Choice = this match {
       |    case Yes(_) => Choice.Yes
       |    case No     => Choice.No
       |  }
       |}
       |
       |object $modelName $extendsClause{
       |  val yesReads: Reads[Yes] = Json.reads[Yes]
       |  val yesWrites: Writes[Yes] = Json.writes[Yes]
       |  val nestedYesReads: Reads[Yes] = Reads.at(JsPath \\ "Yes")(yesReads)
       |
       |  given reads(using config: JsonConfiguration): Reads[$modelName] = Reads {
       |    case obj: JsObject => obj.value.get(config.discriminator) match {
       |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
       |        case yes if yes == config.typeNaming("Yes") =>
       |          nestedYesReads.reads(obj)
       |        case no  if no  == config.typeNaming("No")  =>
       |          JsSuccess(No)
       |        case _ =>
       |          JsError("error.invalid")
       |      }
       |      case _ => JsError(JsPath \\ config.discriminator, "error.missing.path")
       |    }
       |    case _ => JsError("error.expected.jsobject")
       |  }
       |
       |  given writes(using config: JsonConfiguration): Writes[$modelName] = Writes {
       |    case yes: Yes =>
       |      Json.obj(config.discriminator -> config.typeNaming("Yes"), "Yes" -> yesWrites.writes(yes))
       |    case No =>
       |      Json.obj(config.discriminator -> config.typeNaming("No"))
       |  }
       |
       |  given Format[$modelName] = Format(reads, writes)
       |}
       |""".stripMargin
  }

  def forDoWhile(
    modelsPackage: QualifiedName,
    modelName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val importPrefixes = Imports.importedSymbols(fields)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    s"""package $modelsPackage
       |
       |import play.api.libs.json.{Json, Reads}
       |$imports
       |
       |case class $modelName(
       |${fields.map((ModelFields.field _).tupled).mkString("," + System.lineSeparator())}
       |)
       |
       |object $modelName $extendsClause{
       |  given Reads[$modelName] = Json.reads[$modelName]
       |}
       |""".stripMargin
  }
}
