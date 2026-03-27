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

/** A representation of the path to an answer in UserAnswers for a given page key.
  */
sealed trait JourneyPath extends Product with Serializable {

  /** The [[PathAtom]]s that make up this [[JourneyPath]]. */
  def paths: List[PathAtom] = this match {
    case Compound(paths) => paths
    case atom: PathAtom  => List(atom)
  }

  /** The [[ChoicePath]]s within this [[JourneyPath]]. */
  def choicePaths: List[ChoicePath] =
    paths.collect { case choice @ ChoicePath(_, _) => choice }

  /** The [[IndexPath]]s within this [[JourneyPath]]. */
  def indexPaths: List[IndexPath] =
    paths.collect { case index @ IndexPath(_) => index }

  /** Whether this [[JourneyPath]] ends with an [[IndexPath]]. */
  def isIndex: Boolean = this match {
    case Compound(_ :+ IndexPath(_)) => true
    case IndexPath(_)                => true
    case _                           => false
  }

  /** Construct a [[Compound]] [[JourneyPath]] using <code>this</code> and the <code>next</code>
    * [[PathAtom]].
    */
  def /(next: PathAtom): JourneyPath = this match {
    case Compound(paths) => Compound(paths ++ List(next))
    case atom: PathAtom  => Compound(List(atom, next))
  }

  /** A path string that represents the [[JourneyPath]] in a jq-like format. */
  def pathString: String = {
    @tailrec def go(paths: List[PathAtom], acc: StringBuilder = new StringBuilder): StringBuilder =
      paths match {
        case Nil => acc
        case Root :: tail =>
          acc.append("$"); go(tail, acc)
        case StringPath(pageKey) :: tail =>
          if (acc.isEmpty) acc.append(pageKey) else acc.append(s".$pageKey"); go(tail, acc)
        case IndexPath(pageKey) :: tail =>
          if (acc.isEmpty) acc.append(s"$pageKey[]") else acc.append(s".$pageKey[]"); go(tail, acc)
        case ChoicePath(_, enumCase) :: tail =>
          if (acc.isEmpty) acc.append(enumCase) else acc.append(s".$enumCase"); go(tail, acc)
      }

    go(paths).toString
  }
}

/** A compound [[JourneyPath]] composed of multiple [[PathAtom]]s. */
case class Compound(override val paths: List[PathAtom]) extends JourneyPath

/** An atomic part of a [[JourneyPath]] */
sealed trait PathAtom extends JourneyPath

/** The root of a [[JourneyPath]], analogous to play-json's <code>JsPath</code>. */
case object Root extends PathAtom

/** An answer stored under a specific [[pageKey]]. This is analogous to play-json's
  * <code>KeyPathNode</code>.
  */
case class StringPath(pageKey: String) extends PathAtom

/** An answer stored under a specific [[pageKey]] and index. This is analogous to play-json's
  * <code>IdxPathNode</code>, but we don't yet know the specific index.
  */
case class IndexPath(pageKey: String) extends PathAtom

/** An answer stored under the [[pageKey]] and [[enumCase]] of a switch-case journey. */
case class ChoicePath(pageKey: String, enumCase: String) extends PathAtom
