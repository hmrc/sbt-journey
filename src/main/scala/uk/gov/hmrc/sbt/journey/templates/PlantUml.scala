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

object PlantUml {
  private def forParts(parts: List[JourneyPart], indent: Int = 0): List[String] = {
    val p = " " * indent
    parts.map {
      case DoWhilePart(choicePage, subJourney, _) =>
        s"""|${p}repeat
            |${forParts(subJourney, indent + 2).mkString(System.lineSeparator())}
            |${p}repeat while ($choicePage) is (Yes) not (No)""".stripMargin
      case SwitchCasePart(choicePage, subJourneys, _) =>
        val cases = subJourneys.map { case (name, parts) =>
          s"""|${p}case ($name)
              |${forParts(parts, indent + 2)}""".stripMargin
        }
        s"""|${p}switch ($choicePage)
            |${cases.mkString(System.lineSeparator())}
            |${p}endswitch""".stripMargin
      case IfThenPart(choicePage, subJourney, _) =>
        s"""|${p}if ($choicePage) then (Yes)
            |${forParts(subJourney, indent + 2).mkString(System.lineSeparator())}
            |${p}endif""".stripMargin
      case SinglePagePart(pageKey, _) =>
        s"$p:$pageKey;"
    }
  }

  private def forJourney(journey: Journey): String = {
    s"""|@startuml
        |start
        |${forParts(journey.journey).mkString(System.lineSeparator())}
        |end
        |@enduml""".stripMargin
  }

  def forConfig(config: JourneyConfig): Map[String, String] = {
    config.journeys.map { case (name, journey) =>
      name -> forJourney(journey)
    }
  }
}
