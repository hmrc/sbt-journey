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
import uk.gov.hmrc.sbt.journey.templates.Imports.PlayJsonPrefix
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.camelCase

object JourneyModel extends Template {
  private def jsPathReads(fieldName: String, fieldType: FieldType): String = {
    val scalaType = ModelFields.fieldType(fieldType)
    s"""(JsPath \\ "$fieldName").read[$scalaType]"""
  }

  private def listReads(
    journeyPart: JourneyPart,
    fieldName: String,
    fieldType: FieldType
  ): String = {
    val p = " " * 4
    fieldType match {
      case ListType(elementType) =>
        val scalaType = ModelFields.fieldType(elementType)
        s"""${p}val $fieldName = Reads.list(Reads.at[$scalaType](JsPath \\ "${journeyPart.startPage}"))$NL""".stripMargin
      case _ =>
        ""
    }
  }

  private def nestedReads(
    subJourney: List[JourneyPart],
    modelName: String,
    caseName: String,
    fields: List[(String, FieldType)],
    modifier: String
  ): String = {
    val nm = camelCase(caseName)
    if (fields.length == 1) {
      val (fieldName, fieldType) = fields.head
      val readList               = listReads(subJourney.head, fieldName, fieldType)
      val usingReads             = if (readList.nonEmpty) s"(using $fieldName)" else ""
      val pathReads              = jsPathReads(fieldName, fieldType)
      if (readList.isEmpty)
        s"""  $modifier ${nm}Reads: Reads[$modelName] =
           |    $pathReads$usingReads.map($caseName.apply)""".stripMargin
      else
        s"""  $modifier ${nm}Reads: Reads[$modelName] = {
           |$readList    $pathReads$usingReads.map($caseName.apply)
           |  }""".stripMargin
    } else {
      val (readList, readPath) = subJourney
        .zip(fields)
        .map { case (journeyPart, (fieldName, fieldType)) =>
          val readList   = listReads(journeyPart, fieldName, fieldType)
          val usingReads = if (readList.nonEmpty) s"(using $fieldName)" else ""
          val readPath   = s"""      ${jsPathReads(fieldName, fieldType)}$usingReads"""
          (readList, readPath)
        }
        .unzip
      s"""  $modifier ${nm}Reads: Reads[$modelName] = {
         |${if (readList.isEmpty) "" else readList.distinct.mkString}    (
         |${readPath.mkString(" and" + NL)}
         |    )($caseName.apply)
         |  }""".stripMargin
    }
  }

  private def switchCase(caseName: String, fields: List[(String, FieldType)]) = {
    if (fields.isEmpty)
      s"  case $caseName"
    else
      s"""|  case $caseName(
          |  ${fields
           .map((ModelFields.field _).tupled)
           .mkString("," + NL + (" " * 2))}
          |  )""".stripMargin
  }

  private def switchCaseReads(
    subJourney: List[JourneyPart],
    modelName: String,
    caseName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val nm = camelCase(caseName)
    if (fields.isEmpty) ""
    else
      s"""${nestedReads(subJourney, modelName, caseName, fields, "private val")}
         |  private val nested${caseName}Reads: Reads[$modelName] =
         |    (JsPath \\ "$caseName").read[$modelName](using ${nm}Reads)""".stripMargin
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

  def forSwitchCase(
    modelsPackage: QualifiedName,
    switchCasePart: SwitchCasePart,
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
        "Reads"
      )
    )

    val importPrefixes = playImports ++ Imports.importedSymbols(modelCases.values.toList.flatten)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    val caseReads = modelCases
      .map { case (caseName, fields) =>
        switchCaseReads(switchCasePart.subJourneys(caseName), modelName, caseName, fields)
      }
      .toList
      .filterNot(_.isBlank)

    val subtypeReads =
      if (caseReads.isEmpty) ""
      else
        caseReads.mkString(NL, NL, NL)

    s"""package $modelsPackage
       |
       |import play.api.libs.functional.syntax.*
       |$imports
       |
       |enum $modelName {
       |${modelCases.map((switchCase _).tupled).mkString(NL)}
       |}
       |
       |object $modelName $extendsClause{$subtypeReads
       |  given reads(using config: JsonConfiguration): Reads[$modelName] = Reads {
       |    case obj: JsObject => obj.value.get(config.discriminator) match {
       |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
       |${modelCases
        .map((switchCaseRead _).tupled)
        .mkString(NL)}
       |        case _ =>
       |          JsError("error.invalid")
       |      }
       |      case _ => JsError(JsPath \\ config.discriminator, "error.missing.path")
       |    }
       |    case _ => JsError("error.expected.jsobject")
       |  }
       |}
       |""".stripMargin
  }

  def forIfThen(
    modelsPackage: QualifiedName,
    ifThenPart: IfThenPart,
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
        "Reads"
      )
    )

    val importPrefixes = playImports ++ Imports.importedSymbols(fields)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    s"""package $modelsPackage
       |
       |import play.api.libs.functional.syntax.*
       |$imports
       |
       |enum $modelName {
       |  case Yes(
       |  ${fields.map((ModelFields.field _).tupled).mkString("," + NL + "  ")}
       |  )
       |  case No
       |
       |  def choice: Choice = this match {
       |    case Yes${fields.map(_ => "_").mkString("(", ", ", ")")} => Choice.Yes
       |    case No => Choice.No
       |  }
       |}
       |
       |object $modelName $extendsClause{
       |${nestedReads(ifThenPart.subJourney, modelName, "Yes", fields, "private val")}
       |  private val nestedYesReads: Reads[$modelName] =
       |    (JsPath \\ "Yes").read[$modelName](using yesReads)
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
       |}
       |""".stripMargin
  }

  def forDoWhile(
    modelsPackage: QualifiedName,
    doWhilePart: DoWhilePart,
    modelName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val importPrefixes = Imports.importedSymbols(fields)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    s"""package $modelsPackage
       |
       |import play.api.libs.json.{Json, JsPath, Reads}
       |import play.api.libs.functional.syntax.*
       |$imports
       |
       |case class $modelName(
       |${fields.map((ModelFields.field _).tupled).mkString("," + NL)}
       |)
       |
       |object $modelName $extendsClause{
       |${nestedReads(doWhilePart.subJourney, modelName, modelName, fields, "given")}
       |}
       |""".stripMargin
  }
}
