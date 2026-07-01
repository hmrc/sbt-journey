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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.sbt.journey.models.{
  CaseClassModel,
  ClassType,
  EnumModel,
  FieldType,
  OptionType,
  QualifiedName
}

class JourneyGeneratorsSpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  "JourneyGenerators.render" should "generate ScalaCheck generators for enum models" in {
    new JourneyGenerators(Map("Choice" -> EnumModel("Choice", List("Yes", "No"))))
      .render(basePackage) shouldBe
      s"""package uk.gov.hmrc.sbtjourneytest.generators
         |
         |import _root_.generators.Generators // TODO: Remove this once we have a better template
         |import org.scalacheck.{Arbitrary, Gen}
         |import org.scalacheck.Arbitrary.arbitrary
         |import uk.gov.hmrc.sbtjourneytest.models.*
         |
         |trait JourneyGenerators {
         |
         |  given Arbitrary[Choice] = Arbitrary(Gen.oneOf(Choice.values.toIndexedSeq))
         |}
         |""".stripMargin
  }

  it should "generate ScalaCheck generators for case class models" in {
    new JourneyGenerators(
      Map(
        "AuditEvent" -> CaseClassModel(
          "AuditEvent",
          List(
            "auditType"                   -> FieldType.STRING,
            "description"                 -> FieldType.STRING,
            "expectedGoLiveDate"          -> FieldType.LOCALDATE,
            "expectedDecommissioningDate" -> OptionType(FieldType.LOCALDATE)
          )
        )
      )
    ).render(basePackage) shouldBe
      s"""package uk.gov.hmrc.sbtjourneytest.generators
         |
         |import _root_.generators.Generators // TODO: Remove this once we have a better template
         |import java.time.LocalDate
         |import org.scalacheck.{Arbitrary, Gen}
         |import org.scalacheck.Arbitrary.arbitrary
         |import uk.gov.hmrc.sbtjourneytest.models.*
         |
         |trait JourneyGenerators {
         |
         |  given Arbitrary[AuditEvent] = Arbitrary {
         |    for {
         |      auditType <- arbitrary[String]
         |      description <- arbitrary[String]
         |      expectedGoLiveDate <- arbitrary[LocalDate]
         |      expectedDecommissioningDate <- Gen.option(arbitrary[LocalDate])
         |    } yield AuditEvent(auditType, description, expectedGoLiveDate, expectedDecommissioningDate)
         |  }
         |}
         |""".stripMargin
  }

  it should "generate ScalaCheck generators for case class models that use enum models" in {
    new JourneyGenerators(
      Map(
        "RateOfRelief" -> EnumModel("RateOfRelief", List("FIFTY_PERCENT", "HUNDRED_PERCENT")),
        "Exemption" -> CaseClassModel(
          "Exemption",
          List(
            "description"    -> FieldType.STRING,
            "rateOfRelief"   -> ClassType(basePackage / "models" / "RateOfRelief"),
            "amountDeducted" -> FieldType.BIGDECIMAL
          )
        )
      )
    ).render(basePackage) shouldBe
      s"""package uk.gov.hmrc.sbtjourneytest.generators
         |
         |import _root_.generators.Generators // TODO: Remove this once we have a better template
         |import org.scalacheck.{Arbitrary, Gen}
         |import org.scalacheck.Arbitrary.arbitrary
         |import uk.gov.hmrc.sbtjourneytest.models.*
         |
         |trait JourneyGenerators {
         |
         |  given Arbitrary[RateOfRelief] = Arbitrary(Gen.oneOf(RateOfRelief.values.toIndexedSeq))
         |
         |  given Arbitrary[Exemption] = Arbitrary {
         |    for {
         |      description <- arbitrary[String]
         |      rateOfRelief <- arbitrary[RateOfRelief]
         |      amountDeducted <- arbitrary[BigDecimal]
         |    } yield Exemption(description, rateOfRelief, amountDeducted)
         |  }
         |}
         |""".stripMargin
  }

  it should "generate a ScalaCheck generator for UploadId if the journey has a file upload" in {
    new JourneyGenerators(Map.empty).render(basePackage, hasFileUpload = true) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.generators
        |
        |import _root_.generators.Generators // TODO: Remove this once we have a better template
        |import org.scalacheck.{Arbitrary, Gen}
        |import org.scalacheck.Arbitrary.arbitrary
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |
        |trait JourneyGenerators {
        |
        |  given Arbitrary[UploadId] = Arbitrary(Gen.uuid.map(UploadId.apply))
        |}
        |""".stripMargin
  }
}
