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

sealed trait AnswerModel extends Product with Serializable

/** A custom model for a user's answer, represented by a Scala case class.
  * @param name
  *   The name to use for the generated case class.
  * @param fields
  *   The fields of the user's answer, represented as a list of field name and field type pairs.
  */
case class CaseClassModel(
  name: String,
  fields: List[(String, FieldType)]
) extends AnswerModel

/** A custom model for a user's answer, represented by a Scala enum.
  * @param name
  *   The name to use for the generated enum.
  * @param cases
  *   The names of the cases of the enum.
  */
case class EnumModel(
  name: String,
  cases: List[String]
) extends AnswerModel {
  def uncoveredCases(keys: Set[String]): Set[String] =
    cases.toSet.diff(keys)
  def isCoveredBy(keys: Set[String]): Boolean =
    keys.contains("default") || uncoveredCases(keys).isEmpty
}
