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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{capitalise, pascalCase}

object ModelFields {
  def forPart(
    modelsPackage: QualifiedName,
    journey: Journey,
    part: JourneyPart
  ): List[(String, FieldType)] = part match {
    case DoWhilePart(_, subJourney, as) =>
      val subJourneyFields = subJourney.flatMap(forPart(modelsPackage, journey, _))
      if (subJourneyFields.length == 1) List(as -> ListType(subJourneyFields.head._2))
      else List(as -> ListType(SyntheticClassType(modelsPackage.toString, pascalCase(as))))
    case SwitchCasePart(choicePage, _, as) =>
      val fieldName = as.getOrElse(choicePage)
      val fieldType = SyntheticClassType(modelsPackage.toString, pascalCase(fieldName))
      List(fieldName -> fieldType)
    case IfThenPart(choicePage, _, as) =>
      val fieldName = as.getOrElse(choicePage)
      val fieldType = SyntheticClassType(modelsPackage.toString, pascalCase(fieldName))
      List(fieldName -> fieldType)
    case SinglePagePart(pageKey, as) =>
      List(as.getOrElse(pageKey) -> journey.pages(pageKey).answerType)
  }

  def forParts(
    modelsPackage: QualifiedName,
    journey: Journey,
    parts: List[JourneyPart]
  ): List[(String, FieldType)] =
    parts.flatMap(forPart(modelsPackage, journey, _))

  def fieldType(typ: FieldType): String = typ match {
    case ListType(elements)          => s"List[${fieldType(elements)}]"
    case OptionType(elements)        => s"Option[${fieldType(elements)}]"
    case PrimitiveType(clazz)        => capitalise(clazz.getSimpleName)
    case ClassType(clazz)            => clazz.split("\\.").last
    case SyntheticClassType(_, name) => name
  }

  def field(fieldName: String, typ: FieldType) =
    s"  $fieldName: ${fieldType(typ)}"
}
