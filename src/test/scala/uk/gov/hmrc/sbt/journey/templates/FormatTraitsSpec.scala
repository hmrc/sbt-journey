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
import uk.gov.hmrc.sbt.journey.models.QualifiedName

class FormatTraitsSpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  "FormatTraits.extendsClause" should "produce an empty String when the imported symbols do not include any hmrc-mongo Java time types" in {
    // LocalTime does not have a special hmrc-mongo Format instance
    FormatTraits.extendsClause(Map(Imports.JavaTimePrefix -> Set("LocalTime"))) shouldBe ""
  }

  it should "extend hmrc-mongo Java time format traits when the imported symbols do contain supported Java time types" in {
    FormatTraits.extendsClause(
      Map(Imports.JavaTimePrefix -> Set("LocalDate"))
    ) shouldBe "extends MongoJavatimeFormats.Implicits "
  }

  "FormatTraits.enumFormats" should "render a template for EnumFormats relative to the base package" in {
    FormatTraits.enumFormats(basePackage) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{JsonConfiguration, JsError, JsObject, JsPath, Reads}
        |
        |trait EnumFormats {
        |  def enumReads[A <: Product](
        |    cases: (String, Reads[? <: A])*
        |  )(using config: JsonConfiguration): Reads[A] =
        |    enumReads(Reads.failed("error.invalid"), cases*)
        |
        |  def enumReads[A <: Product](
        |    default: => Reads[? <: A],
        |    cases: (String, Reads[? <: A])*
        |  )(using config: JsonConfiguration): Reads[A] =
        |    discriminatedReads(
        |      config.discriminator,
        |      default,
        |      cases.map { case (dsc, fn) => (config.typeNaming(dsc), fn) }*
        |    )
        |
        |  def discriminatedReads[A](
        |    discriminatorKey: String,
        |    cases: (String, Reads[? <: A])*
        |  )(using config: JsonConfiguration): Reads[A] =
        |    discriminatedReads(discriminatorKey, Reads.failed("error.invalid"), cases*)
        |
        |  def discriminatedReads[A](
        |    discriminatorKey: String,
        |    default: => Reads[? <: A],
        |    cases: (String, Reads[? <: A])*
        |  )(using config: JsonConfiguration): Reads[A] = {
        |    val caseReads = cases.toMap
        |    Reads {
        |      case obj: JsObject =>
        |        obj.value.get(discriminatorKey) match {
        |          case Some(discriminatorValue) =>
        |            discriminatorValue.validate[String].flatMap { discriminator =>
        |              caseReads.getOrElse(discriminator, default).reads(obj)
        |            }
        |          case _ => JsError(JsPath \ discriminatorKey, "error.missing.path")
        |        }
        |      case _ => JsError("error.expected.jsobject")
        |    }
        |  }
        |}
        |
        |object EnumFormats extends EnumFormats
        |""".stripMargin
  }
}
