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

import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{camelCase, camelComponents, capitalise, decapitalise, kebabCase, packageCase, pascalCase}

class StringCaseUtilsSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  "StringCaseUtils.capitalise" should "make the first character of its input uppercase" in forAll(
    Gen.nonEmptyStringOf(Gen.asciiPrintableChar)
  ) {
    input: String =>
      whenever(input.nonEmpty && input(0).isLetter) {
        val output = capitalise(input)
        output(0) should be(upperCase)
        output.drop(1) shouldBe input.drop(1)
      }
  }

  it should "leave the rest of the input unchanged" in forAll(
    Gen.nonEmptyStringOf(Gen.asciiPrintableChar)
  ) {
    input: String =>
      whenever(input.nonEmpty && input(0).isLetter) {
        val output = capitalise(input)
        output.drop(1) shouldBe input.drop(1)
      }
  }

  "StringCaseUtils.decapitalise" should "make the first character of its input lowercase" in forAll(
    Gen.nonEmptyStringOf(Gen.asciiPrintableChar)
  ) {
    input: String =>
      whenever(input.nonEmpty && input(0).isLetter) {
        val output = decapitalise(input)
        output(0) should be(lowerCase)
      }
  }

  it should "leave the rest of the input unchanged" in forAll(
    Gen.nonEmptyStringOf(Gen.asciiPrintableChar)
  ) {
    input: String =>
      whenever(input.nonEmpty && input(0).isLetter) {
        val output = decapitalise(input)
        output.drop(1) shouldBe input.drop(1)
      }
  }

  "StringCaseUtils.camelComponents" should "split StringCaseUtils into its camel case components" in {
    camelComponents("StringCaseUtils") shouldBe Array("String", "Case", "Utils")
  }

  it should "split priceIncludingVAT into its camel case components" in {
    camelComponents("priceIncludingVAT") shouldBe Array("price", "Including", "VAT")
  }

  it should "split VATInfo into its camel case components" in {
    camelComponents("VATInfo") shouldBe Array("VAT", "Info")
  }

  it should "split price-including-vat into its camel case components" in {
    camelComponents("price-including-vat") shouldBe Array("price", "including", "vat")
  }

  it should "split vat_info into its camel case components" in {
    camelComponents("vat_info") shouldBe Array("vat", "info")
  }

  "StringCaseUtils.pascalCase" should "convert stringCaseUtils to StringCaseUtils" in {
    pascalCase("stringCaseUtils") shouldBe "StringCaseUtils"
  }

  it should "convert vatInfo to VatInfo" in {
    pascalCase("vatInfo") shouldBe "VatInfo"
  }

  it should "convert priceIncludingVAT to PriceIncludingVat" in {
    pascalCase("priceIncludingVAT") shouldBe "PriceIncludingVat"
  }

  it should "convert price-including-vat to PriceIncludingVat" in {
    pascalCase("price-including-vat") shouldBe "PriceIncludingVat"
  }

  it should "leave PriceIncludingVat unchanged" in {
    pascalCase("PriceIncludingVat") shouldBe "PriceIncludingVat"
  }

  "StringCaseUtils.camelCase" should "convert StringCaseUtils to stringCaseUtils" in {
    camelCase("StringCaseUtils") shouldBe "stringCaseUtils"
  }

  it should "convert VATInfo to vatInfo" in {
    camelCase("VATInfo") shouldBe "vatInfo"
  }

  it should "convert PriceIncludingVat to priceIncludingVat" in {
    camelCase("PriceIncludingVat") shouldBe "priceIncludingVat"
  }

  it should "convert price-including-vat to priceIncludingVat" in {
    camelCase("price-including-vat") shouldBe "priceIncludingVat"
  }

  it should "leave priceIncludingVat unchanged" in {
    camelCase("priceIncludingVat") shouldBe "priceIncludingVat"
  }

  "StringCaseUtils.kebabCase" should "convert StringCaseUtils to string-case-utils" in {
    kebabCase("StringCaseUtils") shouldBe "string-case-utils"
  }

  it should "convert VATInfo to vat-info" in {
    kebabCase("VATInfo") shouldBe "vat-info"
  }

  it should "convert PriceIncludingVAT to price-including-vat" in {
    kebabCase("PriceIncludingVAT") shouldBe "price-including-vat"
  }

  it should "leave price-including-vat unchanged" in {
    kebabCase("price-including-vat") shouldBe "price-including-vat"
  }

  "StringCaseUtils.packageCase" should "convert string-case-utils to stringcaseutils" in {
    packageCase("string-case-utils") shouldBe "stringcaseutils"
  }

  it should "convert VATInfo to vatinfo" in {
    packageCase("VATInfo") shouldBe "vatinfo"
  }

  it should "convert PriceIncludingVat to priceincludingvat" in {
    packageCase("PriceIncludingVat") shouldBe "priceincludingvat"
  }

  it should "leave vatinfo unchanged" in {
    packageCase("vatinfo") shouldBe "vatinfo"
  }

  val upperCase = BePropertyMatcher[Char] { char =>
    BePropertyMatchResult(char.isUpper, "uppercase")
  }

  val lowerCase = BePropertyMatcher[Char] { char =>
    BePropertyMatchResult(char.isLower, "lowercase")
  }
}
