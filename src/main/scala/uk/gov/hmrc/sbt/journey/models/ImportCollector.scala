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

package uk.gov.hmrc.sbt.journey.models

import scala.annotation.tailrec

class ImportCollector(models: Map[String, AnswerModel]) {
  def importedSymbols(
    fieldType: FieldType,
    recursive: Boolean
  ): Map[List[String], Set[String]] =
    importedSymbols(collectClassTypes(fieldType, recursive))

  def importedSymbols(
    fields: List[(String, FieldType)],
    recursive: Boolean
  ): Map[List[String], Set[String]] =
    importedSymbols(collectClassTypes(fields, recursive))

  private def importedSymbols(classTypes: Set[ClassType]): Map[List[String], Set[String]] = {
    classTypes
      .map(_.clazz.split("\\.").toList)
      .groupBy(_.dropRight(1))
      .view
      .map { case (prefix, types) => prefix -> types.map(_.last) }
      .toMap
  }

  private def collectClassTypes(
    fields: List[(String, FieldType)],
    recursive: Boolean
  ): Set[ClassType] = {
    fields
      .map { case (_, fieldType) =>
        collectClassTypes(fieldType, recursive)
      }
      .foldLeft(Set.empty[ClassType])(_ ++ _)
  }

  private def collectClassTypes(fieldType: FieldType, recursive: Boolean): Set[ClassType] = {
    @tailrec def find(typ: FieldType): Set[ClassType] = typ match {
      case ListType(elements)                     => find(elements)
      case SetType(elements)                      => find(elements)
      case OptionType(elements)                   => find(elements)
      case SyntheticClassType(_, _)               => Set.empty
      case PrimitiveType(_)                       => Set.empty
      case classType @ ClassType(_) if !recursive => Set(classType)
      case classType @ ClassType(_) =>
        classType.typeName.flatMap(models.get) match {
          case Some(CaseClassModel(_, fields)) => collectClassTypes(fields, recursive) + classType
          case _                               => Set(classType)
        }
    }

    find(fieldType)
  }
}
