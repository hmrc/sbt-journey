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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.camelCase

import java.time.LocalDate

class JourneyGenerators(models: Map[String, AnswerModel]) extends Template {
  val importCollector = new ImportCollector(models)

  private def generatorNameFor(fieldType: FieldType): Option[String] = {
    fieldType.typeName.flatMap(models.get) match {
      case Some(CaseClassModel(name, _)) =>
        Some(s"${camelCase(name)}Gen")
      case Some(EnumModel(name, _)) =>
        Some(s"${camelCase(name)}Gen")
      case None =>
        fieldType match {
          case PrimitiveType(clazz) if clazz == classOf[Int] =>
            Some("arbitrary[Int]")
          case PrimitiveType(clazz) if clazz == classOf[Boolean] =>
            Some("arbitrary[Boolean]")
          case ClassType(clazz) if clazz == classOf[String].getName =>
            Some("arbitrary[String]")
          case ClassType(clazz) if clazz == classOf[BigDecimal].getName =>
            Some("arbitrary[BigDecimal]")
          case ClassType(clazz) if clazz == classOf[LocalDate].getName =>
            Some("arbitrary[LocalDate]")
          case _ => None
        }
    }
  }

  private def generatorFor(name: String, fieldType: FieldType): Option[String] = {
    val camelFieldName = camelCase(name)
    fieldType.typeName.flatMap(models.get) match {
      case Some(CaseClassModel(name, _)) =>
        Some(s"      $camelFieldName <- arbitrary[$name]")
      case Some(EnumModel(name, _)) =>
        Some(s"      $camelFieldName <- arbitrary[$name]")
      case None =>
        fieldType match {
          case OptionType(elements) =>
            generatorNameFor(elements).map { elementsGen =>
              s"      ${camelFieldName} <- Gen.option(${elementsGen})"
            }
          case ListType(elements) =>
            generatorNameFor(elements).map { elementsGen =>
              s"""     ${camelFieldName}GenSize <- Gen.choose(0, 10)
                 |     ${camelFieldName} <- Gen.listOfN(${camelFieldName}GenSize, ${elementsGen})""".stripMargin
            }
          case SetType(elements) =>
            generatorNameFor(elements).map { elementsGen =>
              s"""     ${camelFieldName}GenSize <- Gen.choose(0, 10)
                 |     ${camelFieldName} <- Gen.setOfN(${camelFieldName}GenSize, ${elementsGen})""".stripMargin
            }
          case PrimitiveType(clazz) if clazz == classOf[Int] =>
            Some(s"      ${camelFieldName} <- arbitrary[Int]")
          case PrimitiveType(clazz) if clazz == classOf[Boolean] =>
            Some(s"      ${camelFieldName} <- arbitrary[Boolean]")
          case ClassType(clazz) if clazz == classOf[String].getName =>
            Some(s"      ${camelFieldName} <- arbitrary[String]")
          case ClassType(clazz) if clazz == classOf[BigDecimal].getName =>
            Some(s"      ${camelFieldName} <- arbitrary[BigDecimal]")
          case ClassType(clazz) if clazz == classOf[LocalDate].getName =>
            Some(s"      ${camelFieldName} <- arbitrary[LocalDate]")
          case _ =>
            None
        }
    }
  }

  private def generatorsFor(models: Map[String, AnswerModel], hasFileUpload: Boolean): String = {
    val uploadIdGen =
      if (!hasFileUpload) List.empty
      else List(s"  given Arbitrary[UploadId] = Arbitrary(Gen.uuid.map(UploadId.apply))")

    val modelGenerators = models.flatMap {
      case (_, EnumModel(name, _)) =>
        List(s"  given Arbitrary[$name] = Arbitrary(Gen.oneOf($name.values.toIndexedSeq))")
      case (_, CaseClassModel(name, fields)) =>
        val fieldNames = fields.map { case (name, _) => name }
        // Only emit a generator for this model if we have a generator for every field
        val fieldGens = fields
          .map((generatorFor _).tupled)
          .foldLeft(Option(List.empty[String])) {
            case (Some(acc), Some(next)) => Some(next :: acc)
            case (None, _)               => None
            case (_, None)               => None
          }
          .map(_.reverse)
        fieldGens.map { gens =>
          s"""  given Arbitrary[$name] = Arbitrary {
             |    for {
             |${gens.mkString(NL)}
             |    } yield $name(${fieldNames.mkString(", ")})
             |  }""".stripMargin
        }.toList
    }

    val allGenerators = uploadIdGen ++ modelGenerators

    if (allGenerators.isEmpty) ""
    else allGenerators.mkString(NL * 2)
  }

  def render(basePackage: QualifiedName, hasFileUpload: Boolean = false): String = {
    s"""package ${basePackage / "generators"}
       |
       |import _root_.generators.Generators // TODO: Remove this once we have a better template
       |import org.scalacheck.{Arbitrary, Gen}
       |import org.scalacheck.Arbitrary.arbitrary
       |import ${basePackage / "models" / "*"}
       |
       |import java.time.{Instant,LocalDate,ZoneOffset}
       |
       |trait JourneyGenerators {
       |  // Empty strings are not valid for form binding
       |  given Arbitrary[String] = Arbitrary(Gen.nonEmptyBuildableOf[String, Char](Arbitrary.arbChar.arbitrary))
       |  // Instant.MIN and Instant.MAX can't be serialized by hmrc-mongo as they're out of range for Long
       |  given Arbitrary[Instant] = Arbitrary(Gen.choose(Instant.ofEpochMilli(Long.MinValue), Instant.ofEpochMilli(Long.MaxValue)))
       |  // LocalDate is converted to Instant before it's serialized by hmrc-mongo
       |  given Arbitrary[LocalDate] = Arbitrary(arbitrary[Instant].map(_.atZone(ZoneOffset.UTC).toLocalDate()))
       |
       |${generatorsFor(models, hasFileUpload)}
       |}
       |""".stripMargin
  }
}
