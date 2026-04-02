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

object Imports extends Template {
  private[templates] val JavaLangPrefix             = List("java", "lang")
  private[templates] val JavaTimePrefix             = List("java", "time")
  private[templates] val PlayI18nPrefix             = List("play", "api", "i18n")
  private[templates] val PlayJsonPrefix             = List("play", "api", "libs", "json")
  private[templates] val HmrcMongoJavaTimeInstances = Set("LocalDate", "Instant")

  private def needsImport(filePackage: QualifiedName, prefix: List[String]): Boolean =
    prefix != JavaLangPrefix && prefix != filePackage.parts

  def usesHmrcMongoJavaTime(importedSymbols: Map[List[String], Set[String]]): Boolean = {
    importedSymbols.contains(JavaTimePrefix) &&
    importedSymbols(JavaTimePrefix).intersect(HmrcMongoJavaTimeInstances).nonEmpty
  }

  def importsFor(
    currentPackage: QualifiedName,
    importedSymbols: Map[List[String], Set[String]],
    addFormatImports: Boolean = true
  ): String = {
    val fieldImports = importedSymbols
      .collect {
        case (prefix, types) if needsImport(currentPackage, prefix) =>
          val packagePrefix =
            if (prefix.isEmpty) "_root_."
            else prefix.mkString("", ".", ".")
          val symbols =
            if (types.size == 1) types.head
            else types.toList.sorted.mkString("{", ",", "}")
          s"import $packagePrefix$symbols"
      }

    val instanceImports =
      if (addFormatImports && usesHmrcMongoJavaTime(importedSymbols))
        List("import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats")
      else List.empty

    (fieldImports ++ instanceImports).mkString(NL)
  }

  def merge(
    left: Map[List[String], Set[String]],
    right: Map[List[String], Set[String]]
  ): Map[List[String], Set[String]] =
    left ++ right.map { case (k, v) => k -> (v ++ left.getOrElse(k, Set.empty)) }

  def importedSymbols(fieldType: FieldType): Map[List[String], Set[String]] =
    importedSymbols(collectClassTypes(fieldType))

  def importedSymbols(fields: List[(String, FieldType)]): Map[List[String], Set[String]] =
    importedSymbols(collectClassTypes(fields))

  private def importedSymbols(classTypes: Set[ClassType]): Map[List[String], Set[String]] = {
    classTypes
      .map(_.clazz.split("\\.").toList)
      .groupBy(_.dropRight(1))
      .view
      .map { case (prefix, types) => prefix -> types.map(_.last) }
      .toMap
  }

  private def collectClassTypes(fields: List[(String, FieldType)]): Set[ClassType] = {
    fields
      .map { case (_, fieldType) =>
        collectClassTypes(fieldType)
      }
      .foldLeft(Set.empty[ClassType])(_ ++ _)
  }

  private def collectClassTypes(fieldType: FieldType): Set[ClassType] = {
    def find(typ: FieldType): Set[ClassType] = typ match {
      case ListType(elements)       => find(elements)
      case OptionType(elements)     => find(elements)
      case classType @ ClassType(_) => Set(classType)
      case SyntheticClassType(_, _) => Set.empty
      case PrimitiveType(_)         => Set.empty
    }

    find(fieldType)
  }
}
