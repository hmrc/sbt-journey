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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JourneyPathSpec extends AnyFlatSpec with Matchers {
  "JourneyPath.paths" should "return the atoms of a compound JourneyPath" in {
    val path = Root /
      StringPath("addATaxRegime") /
      IndexPath("taxRegimes") /
      StringPath("taxRegime")

    path.paths shouldBe List(
      Root,
      StringPath("addATaxRegime"),
      IndexPath("taxRegimes"),
      StringPath("taxRegime")
    )
  }

  it should "return the only atom of a root JourneyPath" in {
    Root.paths shouldBe List(Root)
  }

  it should "return the only atom of a StringPath" in {
    StringPath("addATaxRegime").paths shouldBe List(StringPath("addATaxRegime"))
  }

  it should "return the only atom of an IndexPath" in {
    IndexPath("taxRegimes").paths shouldBe List(IndexPath("taxRegimes"))
  }

  it should "return the only atom of a ChoicePath" in {
    ChoicePath("whichTaxRegime", "VAT").paths shouldBe List(ChoicePath("whichTaxRegime", "VAT"))
  }

  "JourneyPath.indexPaths" should "return the IndexPaths within a JourneyPath" in {
    val path = Root /
      IndexPath("auditSources") /
      IndexPath("auditEvents") /
      StringPath("auditEvent")

    path.indexPaths shouldBe List(IndexPath("auditSources"), IndexPath("auditEvents"))
  }

  it should "return an empty list when none are present" in {
    val path = Root /
      StringPath("whichTaxRegime") /
      ChoicePath("whichTaxRegime", "SA") /
      StringPath("saInfo")

    path.indexPaths shouldBe List.empty
  }

  "JourneyPath.isIndex" should "return true when the path ends with an IndexPath" in {
    val path = Root /
      IndexPath("auditSources") /
      IndexPath("auditEvents")

    assert(path.isIndex)
  }

  it should "return true for a lone IndexPath" in {
   assert(IndexPath("auditEvents").isIndex)
  }

  it should "return false for compound paths that don't end with an index" in {
    val path = Root /
      IndexPath("auditSources") /
      IndexPath("auditEvents") /
      StringPath("auditEvent")

    assert(!path.isIndex)
  }

  it should "return false for lone StringPaths" in {
    assert(!StringPath("auditEvent").isIndex)
  }

  it should "return false for lone ChoicePaths" in {
    assert(!ChoicePath("whichTaxRegime", "VAT").isIndex)
  }

  "JourneyPath.pathString" should "return a path string representing a compound JourneyPath with an index path" in {
    val path = Root /
      StringPath("addATaxRegime") /
      IndexPath("taxRegimes") /
      StringPath("taxRegime")

    path.pathString shouldBe "$.addATaxRegime.taxRegimes[].taxRegime"
  }

  it should "return a path string representing a compound JourneyPath with a choice path" in {
    val path = Root /
      StringPath("whichTaxRegime") /
      ChoicePath("whichTaxRegime", "SA") /
      StringPath("saInfo")

    path.pathString shouldBe "$.whichTaxRegime.SA.saInfo"
  }

  it should "return a path string representing a root JourneyPath" in {
    Root.pathString shouldBe "$"
  }

  it should "return a path string representing a StringPath" in {
    StringPath("addATaxRegime").pathString shouldBe "addATaxRegime"
  }

  it should "return a path string representing an IndexPath" in {
    IndexPath("taxRegimes").pathString shouldBe "taxRegimes[]"
  }

  it should "return a path string representing a ChoicePath" in {
    ChoicePath("whichTaxRegime", "VAT").pathString shouldBe "VAT"
  }

}
