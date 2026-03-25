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
  ListType,
  OptionType,
  PrimitiveType,
  QualifiedName
}

import java.time.LocalDate

class CustomModelSpec extends AnyFlatSpec with Matchers {
  "CustomModel.render" should "render an enum model" in {
    val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

    val enumModel = EnumModel("TestEnum", List("A", "B", "C"))

    CustomModel.render(basePackage, enumModel) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Json, JsonValidationError, Format, Reads, Writes}
        |
        |enum TestEnum {
        |  case A, B, C
        |}
        |
        |object TestEnum {
        |  private val labels = values.map(_.toString)
        |
        |  given reads: Reads[TestEnum] = Reads.of[String]
        |    .filter(JsonValidationError("error.invalid"))(labels.contains)
        |    .map(TestEnum.valueOf)
        |
        |  given writes: Writes[TestEnum] = Writes.of[String].contramap(_.toString)
        |
        |  given Format[TestEnum] = Format(reads, writes)
        |
        |  def fromString(value: String): Option[TestEnum] =
        |    values.find(_.toString == value)
        |}
        |""".stripMargin
  }

  it should "render a case class model that uses built-in Java and Scala types" in {
    val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

    val caseClassModel = CaseClassModel(
      "TestCaseClass",
      List(
        "anInt"             -> FieldType.INT,
        "anOptionOfBoolean" -> OptionType(FieldType.BOOLEAN),
        "aListOfString"     -> ListType(FieldType.STRING)
      )
    )

    CustomModel.render(basePackage, caseClassModel) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Json, Format}
        |
        |
        |case class TestCaseClass(
        |  anInt: Int,
        |  anOptionOfBoolean: Option[Boolean],
        |  aListOfString: List[String]
        |)
        |
        |object TestCaseClass {
        |  given Format[TestCaseClass] = Json.format[TestCaseClass]
        |}
        |""".stripMargin
  }

  it should "render a case class model that uses custom model types" in {
    val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

    val caseClassModel = CaseClassModel(
      "TestCaseClass",
      List(
        "anInt"               -> FieldType.INT,
        "anOptionOfTaxRegime" -> OptionType(ClassType(basePackage / "models" / "TaxRegime")),
        "aListOfString"       -> ListType(FieldType.STRING)
      )
    )

    CustomModel.render(basePackage, caseClassModel) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Json, Format}
        |
        |
        |case class TestCaseClass(
        |  anInt: Int,
        |  anOptionOfTaxRegime: Option[TaxRegime],
        |  aListOfString: List[String]
        |)
        |
        |object TestCaseClass {
        |  given Format[TestCaseClass] = Json.format[TestCaseClass]
        |}
        |""".stripMargin
  }

  it should "render a case class model that uses class types from another package" in {
    val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

    val caseClassModel = CaseClassModel(
      "TestCaseClass",
      List(
        "anInt"               -> FieldType.INT,
        "anOptionOfBsonDoc" -> OptionType(ClassType("org.mongodb.scala.bson.Document")),
        "aListOfString"       -> ListType(FieldType.STRING)
      )
    )

    CustomModel.render(basePackage, caseClassModel) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Json, Format}
        |import org.mongodb.scala.bson.Document
        |
        |case class TestCaseClass(
        |  anInt: Int,
        |  anOptionOfBsonDoc: Option[Document],
        |  aListOfString: List[String]
        |)
        |
        |object TestCaseClass {
        |  given Format[TestCaseClass] = Json.format[TestCaseClass]
        |}
        |""".stripMargin
  }

  it should "render a case class model that uses hmrc-mongo Java time instances" in {
    val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

    val caseClassModel = CaseClassModel(
      "TestCaseClass",
      List(
        "anInt"         -> FieldType.INT,
        "aDate"         -> ClassType(classOf[LocalDate].getName),
        "aListOfString" -> ListType(FieldType.STRING)
      )
    )

    CustomModel.render(basePackage, caseClassModel) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Json, Format}
        |import java.time.LocalDate
        |import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
        |
        |case class TestCaseClass(
        |  anInt: Int,
        |  aDate: LocalDate,
        |  aListOfString: List[String]
        |)
        |
        |object TestCaseClass extends MongoJavatimeFormats.Implicits {
        |  given Format[TestCaseClass] = Json.format[TestCaseClass]
        |}
        |""".stripMargin
  }
}
