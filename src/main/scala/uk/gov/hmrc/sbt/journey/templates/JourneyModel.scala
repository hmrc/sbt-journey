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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{camelCase, pascalCase}

class JourneyModel(pages: Map[String, JourneyPage], models: Map[String, AnswerModel])
  extends Template {
  val collector = new ImportCollector(models)

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
    val nm        = camelCase(caseName)
    val capitalNm = pascalCase(caseName)
    if (fields.isEmpty) ""
    else
      s"""${nestedReads(subJourney, modelName, caseName, fields, "private val")}
         |  private val nested${capitalNm}Reads: Reads[$modelName] =
         |    (JsPath \\ "$caseName").read[$modelName](using ${nm}Reads)""".stripMargin
  }

  private def readNamedCase(
    caseName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val capitalNm = pascalCase(caseName)
    if (fields.isEmpty)
      s"""|    "$caseName" -> Reads.pure($caseName)""".stripMargin
    else
      s"""|    "$caseName" -> nested${capitalNm}Reads""".stripMargin
  }

  private def readDefaultCase(fields: List[(String, FieldType)]): String = {
    if (fields.isEmpty)
      "    default = Reads.pure(default)"
    else
      "    default = nestedDefaultReads"
  }

  def forSwitchCase(
    modelsPackage: QualifiedName,
    switchCasePart: SwitchCasePart,
    choicePage: String,
    modelName: String,
    modelCases: Map[String, List[(String, FieldType)]]
  ): String = {
    val playImports = Map(PlayJsonPrefix -> Set("JsSuccess", "JsPath", "Reads"))

    val importPrefixes =
      playImports ++ collector.importedSymbols(modelCases.values.toList.flatten, recursive = false)
    val imports       = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause = FormatTraits.extendsClause(importPrefixes, modelsPackage / "EnumFormats")

    val answerType = pages(choicePage).answerType

    val Some(model @ EnumModel(_, _)) = answerType.typeName.flatMap(models.get)

    val nonDefault = modelCases.filterKeys(_ != "default")
    val uncovered  = model.uncoveredCases(modelCases.keySet)
    val default    = modelCases.get("default")

    val normalCases    = nonDefault.map((switchCase _).tupled).toList
    val uncoveredCases = uncovered.map(switchCase(_, List.empty)).toList
    val defaultCase = default
      .map(fields => List(switchCase("default", fields)))
      .getOrElse(uncoveredCases)

    val normalCaseReads = nonDefault
      .filter { case (_, fields) => fields.nonEmpty }
      .map { case (caseName, fields) =>
        switchCaseReads(switchCasePart.subJourneys(caseName), modelName, caseName, fields)
      }
      .toList

    val defaultCaseReads = default
      .filterNot(_.isEmpty)
      .map { fields =>
        switchCaseReads(switchCasePart.subJourneys("default"), modelName, "default", fields)
      }
      .toList

    val allCaseReads = normalCaseReads ++ defaultCaseReads

    val subtypeReads =
      if (allCaseReads.isEmpty) ""
      else
        allCaseReads.mkString(NL, NL, NL)

    val readNamedCases =
      nonDefault.map((readNamedCase _).tupled).toList
    val readUncoveredCases =
      uncovered.map(readNamedCase(_, List.empty)).toList

    val readDefault = default
      .map(fields => List(readDefaultCase(fields)))
      .getOrElse(readUncoveredCases)

    s"""package $modelsPackage
       |
       |import play.api.libs.functional.syntax.*
       |$imports
       |
       |enum $modelName {
       |${(normalCases ++ defaultCase).mkString(NL)}
       |}
       |
       |object $modelName $extendsClause{$subtypeReads
       |  given reads: Reads[$modelName] = enumReads(
       |${(readDefault ++ readNamedCases).mkString("," + NL)}
       |  )
       |}
       |""".stripMargin
  }

  def forIfThen(
    modelsPackage: QualifiedName,
    ifThenPart: IfThenPart,
    modelName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val playImports    = Map(PlayJsonPrefix -> Set("JsPath", "Reads"))
    val importPrefixes = playImports ++ collector.importedSymbols(fields, recursive = false)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes, modelsPackage / "EnumFormats")

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
       |  given reads: Reads[$modelName] = enumReads(
       |    "Yes" -> nestedYesReads,
       |    "No" -> Reads.pure(No)
       |  )
       |}
       |""".stripMargin
  }

  def forDoWhile(
    modelsPackage: QualifiedName,
    doWhilePart: DoWhilePart,
    modelName: String,
    fields: List[(String, FieldType)]
  ): String = {
    val playImports    = Map(PlayJsonPrefix -> Set("JsPath", "Reads"))
    val importPrefixes = playImports ++ collector.importedSymbols(fields, recursive = false)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    s"""package $modelsPackage
       |
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
