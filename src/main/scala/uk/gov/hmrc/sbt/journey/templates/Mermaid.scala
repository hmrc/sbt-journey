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

object Mermaid extends Template {
  private def forPart(part: JourneyPart, nextPart: JourneyPart): List[String] = {
    val p = " " * 2
    part match {
      case SinglePagePart(pageKey, _) =>
        List(s"$p$pageKey-->${nextPart.startPage}")
      case SwitchCasePart(choicePage, subJourneys, _) =>
        s"$choicePage{{$choicePage}}" :: subJourneys.toList.map { case (name, parts) =>
          s"""|$p$choicePage-- $name -->${parts.head.startPage}
              |${forParts(parts, nextPart)}""".stripMargin
        }
      case IfThenPart(choicePage, subJourney, _) =>
        val subJourneyParts = forParts(subJourney, nextPart)
        val choicePagePart =
          s"""|$p$choicePage{{$choicePage}}
              |$p$choicePage-- Yes -->${subJourney.head.startPage}
              |$p$choicePage-- No -->${nextPart.startPage}""".stripMargin
        choicePagePart :: subJourneyParts
      case DoWhilePart(choicePage, subJourney, _) =>
        val subJourneyParts = forParts(subJourney, SinglePagePart(choicePage, None))
        val choicePagePart =
          s"""|$p$choicePage{{$choicePage}}
              |$p$choicePage-- Yes -->${subJourney.head.startPage}
              |$p$choicePage-- No -->${nextPart.startPage}""".stripMargin
        choicePagePart :: subJourneyParts
    }
  }

  private def forParts(parts: List[JourneyPart], nextPart: JourneyPart): List[String] = {
    val sequentialParts = parts.sliding(2).toList.flatMap {
      case firstPart :: nextPart :: Nil =>
        forPart(firstPart, nextPart)
      case _ => Nil
    }
    val continuationParts = parts.lastOption.toList.flatMap(forPart(_, nextPart))
    sequentialParts ++ continuationParts
  }

  private def forJourney(journey: Journey): String = {
    val parts = journey.journey.sliding(2).toList.flatMap {
      case firstPart :: nextPart :: Nil =>
        forPart(firstPart, nextPart)
      case _ => Nil
    }
    s"""|```mermaid
        |flowchart TD
        |${parts.mkString(NL)}
        |```""".stripMargin
  }

  def forConfig(config: JourneyConfig): Map[String, String] = {
    config.journeys.map { case (name, journey) =>
      name -> forJourney(journey)
    }
  }
}
