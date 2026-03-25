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

/** A journey consisting of multiple [[JourneyPart]]s.
  *
  * @param pages
  *   the pages of the journey.
  * @param journey
  *   the parts of the journey.
  */
case class Journey(pages: Map[String, JourneyPage], journey: List[JourneyPart]) {

  /** The [[JourneyPage.pageKey]] of the first [[SinglePagePart]] in the [[journey]].
    */
  def startPage: String = {
    @tailrec def find(journeyPart: JourneyPart): String = journeyPart match {
      case DoWhilePart(_, subJourney, _)    => find(subJourney.head)
      case SwitchCasePart(choicePage, _, _) => choicePage
      case IfThenPart(choicePage, _, _)     => choicePage
      case SinglePagePart(pageKey, _)       => pageKey
    }

    find(journey.head)
  }

  /** * A list of [[JourneyPath]]s at which the given [[JourneyPage.pageKey]] appears.
    */
  def pathsFor(pageKey: String): List[JourneyPath] = {
    def go(
      pageKey: String,
      journeyPart: JourneyPart,
      acc: JourneyPath = Root
    ): List[JourneyPath] =
      journeyPart match {
        case SinglePagePart(key, as) =>
          val singlePageKey  = as.getOrElse(key)
          val singlePagePath = acc / StringPath(singlePageKey)
          if (singlePageKey == pageKey) List(singlePagePath) else List.empty
        case DoWhilePart(choicePage, subJourney, as) =>
          val doWhilePath = acc / IndexPath(as)
          val subPaths    = subJourney.flatMap(go(pageKey, _, doWhilePath))
          if (choicePage == pageKey) doWhilePath +: subPaths else subPaths
        case IfThenPart(choicePage, subJourney, as) =>
          val choicePageKey = as.getOrElse(choicePage)
          val ifThenPath    = acc / StringPath(choicePageKey)
          val subPaths      = subJourney.flatMap(go(pageKey, _, ifThenPath))
          if (choicePageKey == pageKey) ifThenPath +: subPaths else subPaths
        case SwitchCasePart(choicePage, subJourneys, as) =>
          val choicePageKey  = as.getOrElse(choicePage)
          val choicePagePath = acc / StringPath(choicePageKey)
          val subPaths = subJourneys.flatMap { case (choice, part) =>
            val choicePath = choicePagePath / ChoicePath(choicePageKey, choice)
            part.flatMap(go(pageKey, _, choicePath))
          }.toList
          if (choicePageKey == pageKey) choicePagePath +: subPaths else subPaths
      }

    journey.flatMap(go(pageKey, _))
  }
}
