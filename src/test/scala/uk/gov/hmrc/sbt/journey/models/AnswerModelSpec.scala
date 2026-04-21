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

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class AnswerModelSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  implicit val arbCovered: Arbitrary[Set[String]] = Arbitrary {
    Gen.nonEmptyContainerOf[Set, String](Gen.identifier)
  }

  implicit val arbUncovered: Arbitrary[(List[String], Set[String])] = Arbitrary {
    for {
      cases   <- Gen.nonEmptyListOf(Gen.identifier)
      numKeys <- Gen.choose(0, cases.length - 1)
      keys    <- Gen.pick(numKeys, cases)
    } yield (cases, keys.toSet)
  }

  "EnumModel.isCoveredBy" should "be covered when all cases are matched" in forAll {
    (name: String, covered: Set[String]) =>
      EnumModel(name, covered.toList).isCoveredBy(covered) shouldBe true
  }

  it should "not be covered when cases are missing" in forAll {
    (name: String, uncovered: (List[String], Set[String])) =>
      val (cases, keys) = uncovered
      EnumModel(name, cases).isCoveredBy(keys) shouldBe false
  }

  it should "be covered if one of the keys is 'default'" in forAll {
    (name: String, uncovered: (List[String], Set[String])) =>
      val (cases, keys) = uncovered
      EnumModel(name, cases).isCoveredBy(keys + "default") shouldBe true
  }
}
