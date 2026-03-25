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

package uk.gov.hmrc.sbt.journey.utils

/** Utilities for converting between cases. We do not try to robustly handle all possible scenarios.
  * We rely on the conventions that page keys from journey config are already in camel case format,
  * and the service name in the journey config is already in kebab-case format
  */
object StringCaseUtils {
  private[utils] def camelComponents(str: String): Array[String] = {
    // A lowercase character followed by an uppercase character
    // or an uppercase acronym followed by a lowercase character
    // or another common separator for identifiers.
    str.split("""(?<=\p{Lower})(?=\p{Upper})|(?<=\p{Upper})(?=\p{Upper}\p{Lower})|[_-]""")
  }

  def capitalise(str: String): String =
    if (str.nonEmpty) str.charAt(0).toUpper + str.substring(1) else str

  def decapitalise(str: String): String =
    if (str.nonEmpty) str.charAt(0).toLower + str.substring(1) else str

  def pascalCase(str: String): String =
    camelComponents(str) match {
      case Array(first, rest*) => capitalise(first) + rest.map(capitalise).mkString
    }

  def camelCase(str: String): String =
    camelComponents(str) match {
      case Array(first, rest*) => first.toLowerCase + rest.map(capitalise).mkString
    }

  def kebabCase(str: String): String =
    camelComponents(str).map(_.toLowerCase).mkString("-")

  def packageCase(str: String): String =
    str.toLowerCase().replaceAll("""([-_]|\\s)""", "")
}
