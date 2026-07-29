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
import uk.gov.hmrc.sbt.journey.templates.Imports.{JavaLangPrefix, JavaTimePrefix}

import java.time.Instant
import java.time.format.DateTimeFormatter

class ImportCollectorSpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  "ImportCollector.importedSymbols" should "collect imported symbols from field types" in {
    val collector = new ImportCollector(Map.empty)
    collector.importedSymbols(
      ListType(ClassType(classOf[DateTimeFormatter])),
      recursive = false
    ) shouldBe Map(
      List("java", "time", "format") -> Set("DateTimeFormatter")
    )
  }

  it should "collect imported symbols from field types recursively if requested" in {
    val collector = new ImportCollector(
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
    )

    collector.importedSymbols(
      ClassType(basePackage / "models" / "Exemption"),
      recursive = true
    ) shouldBe Map(
      JavaLangPrefix                                        -> Set("String"),
      List("scala", "math")                                 -> Set("BigDecimal"),
      List("uk", "gov", "hmrc", "sbtjourneytest", "models") -> Set("Exemption", "RateOfRelief")
    )
  }

  it should "collect imported symbols from lists of field declarations" in {
    val collector = new ImportCollector(Map.empty)
    collector.importedSymbols(
      List(
        "startDateTime" -> OptionType(ClassType(classOf[Instant])),
        "endDate"       -> OptionType(FieldType.LOCALDATE),
        "formatters"    -> ListType(ClassType(classOf[DateTimeFormatter]))
      ),
      recursive = false
    ) shouldBe Map(
      JavaTimePrefix                 -> Set("LocalDate", "Instant"),
      List("java", "time", "format") -> Set("DateTimeFormatter")
    )
  }

  it should "collect imported symbols from lists of field declarations recursively if requested" in {
    val collector = new ImportCollector(
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
    )

    collector.importedSymbols(
      List("exemption" -> ClassType(basePackage / "models" / "Exemption")),
      recursive = true
    ) shouldBe Map(
      JavaLangPrefix                                        -> Set("String"),
      List("scala", "math")                                 -> Set("BigDecimal"),
      List("uk", "gov", "hmrc", "sbtjourneytest", "models") -> Set("Exemption", "RateOfRelief")
    )
  }
}
