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

/** A journey consisting of one or more pages.
  */
sealed trait JourneyPart extends Product with Serializable {
  def startPage: String = {
    @tailrec def find(journeyPart: JourneyPart): String = journeyPart match {
      case DoWhilePart(_, subJourney, _)    => find(subJourney.head)
      case SwitchCasePart(choicePage, _, _) => choicePage
      case IfThenPart(choicePage, _, _)     => choicePage
      case SinglePagePart(pageKey, _)       => pageKey
    }

    find(this)
  }

  def startPageIndexes: List[IndexPath] = {
    @tailrec def find(journeyPart: JourneyPart, path: List[IndexPath] = Nil): List[IndexPath] =
      journeyPart match {
        case DoWhilePart(_, subJourney, as) => find(subJourney.head, IndexPath(as) :: path)
        case _                              => path.reverse
      }

    find(this)
  }
}

/** A looping journey which accumulates a list of answers as long as the user answers the
  * [[choicePage]] affirmatively.
  * @param choicePage
  *   the page key of a boolean choice page.
  * @param subJourney
  *   the parts of the looping journey.
  * @param as
  *   the key to use to store the page's data in the user answers.
  */
case class DoWhilePart(choicePage: String, subJourney: List[JourneyPart], as: String)
  extends JourneyPart

/** A journey which splits into several different journeys based upon the value of a user's answer
  * on the [[choicePage]].
  *
  * @param choicePage
  *   the page key of a boolean choice page.
  * @param subJourneys
  *   a map of enum choices to the [[JourneyPart]]s that make up each subjourney.
  * @param as
  *   the key to use to store the page's data in the user answers. If absent, [[choicePage]] is
  *   used.
  */
case class SwitchCasePart(
  choicePage: String,
  subJourneys: Map[String, List[JourneyPart]],
  as: Option[String]
) extends JourneyPart

/** An optional [[subJourney]] which is only used if the user answers the [[choicePage]]
  * affirmatively.
  *
  * @param choicePage
  *   the page key of a Boolean choice page which determines whether to navigate the user to the
  *   optional subjourney.
  * @param subJourney
  *   a list of [[JourneyPart]]s that make up the optional subjourney.
  * @param as
  *   the key to use to store the page's data in the user answers. If absent, [[choicePage]] is
  *   used.
  */
case class IfThenPart(
  choicePage: String,
  subJourney: List[JourneyPart],
  as: Option[String]
) extends JourneyPart

/** An individual page of a journey.
  *
  * @param pageKey
  *   the key of the page in the [[JourneyConfig.journeys]]
  * @param as
  *   the key to use to store the page's data in the user answers. If absent, [[pageKey]] is used.
  */
case class SinglePagePart(pageKey: String, as: Option[String]) extends JourneyPart
