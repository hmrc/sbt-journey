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
import uk.gov.hmrc.sbt.journey.models.*
import uk.gov.hmrc.sbt.journey.templates.Imports.{JavaLangPrefix, JavaTimePrefix}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

class ImportsSpec extends AnyFlatSpec with Matchers {
  "Imports.usesHmrcMongoJavaTime" should "detect usage of supported hmrc-mongo Java time types" in {
    Imports.usesHmrcMongoJavaTime(Map(JavaTimePrefix -> Set("LocalDate"))) shouldBe true
    Imports.usesHmrcMongoJavaTime(Map(JavaTimePrefix -> Set("Instant"))) shouldBe true
  }

  it should "not report usage of unsupported types" in {
    Imports.usesHmrcMongoJavaTime(Map(JavaTimePrefix -> Set("LocalTime"))) shouldBe false
  }

  "Imports.importsFor" should "add imports for class types from other packages" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    val pagesPackage  = basePackage / "pages"
    Imports.importsFor(
      pagesPackage,
      Map(modelsPackage.parts -> Set("TestCaseClass"), pagesPackage.parts -> Set("TaxRegimePage"))
    ) shouldBe "import uk.gov.hmrc.sbtjourneytest.models.TestCaseClass"
  }

  it should "not add imports from the current package" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    val pagesPackage  = basePackage / "pages"
    Imports.importsFor(
      modelsPackage,
      Map(modelsPackage.parts -> Set("TestCaseClass"), pagesPackage.parts -> Set("TaxRegimePage"))
    ) shouldBe "import uk.gov.hmrc.sbtjourneytest.pages.TaxRegimePage"
  }

  it should "not add imports for types from java.lang" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    Imports.importsFor(
      modelsPackage,
      Map(JavaLangPrefix -> Set("Integer"))
    ) shouldBe ""
  }

  it should "add imports for hmrc-mongo Java time instances when the corresponding types are used" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    Imports.importsFor(
      modelsPackage,
      Map(JavaTimePrefix -> Set("LocalDate"))
    ) shouldBe
      """import java.time.LocalDate
        |import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats""".stripMargin
  }

  it should "not add imports for hmrc-mongo Java time instances when format imports are disabled" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    Imports.importsFor(
      modelsPackage,
      Map(JavaTimePrefix -> Set("LocalDate")),
      addFormatImports = false
    ) shouldBe "import java.time.LocalDate"
  }

  it should "support importing multiple symbols" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    Imports.importsFor(
      modelsPackage,
      Map(JavaTimePrefix -> Set("LocalDate", "Instant")),
      addFormatImports = false
    ) shouldBe "import java.time.{Instant,LocalDate}"
  }

  "Imports.importedSymbols" should "collect imported symbols from field types" in {
    Imports.importedSymbols(
      MapType(FieldType.STRING, ClassType(classOf[DateTimeFormatter]))
    ) shouldBe Map(
      JavaLangPrefix                 -> Set("String"),
      List("java", "time", "format") -> Set("DateTimeFormatter")
    )
  }

  "Imports.importedSymbols" should "collect imported symbols from lists of field declarations" in {
    Imports.importedSymbols(
      List(
        "startDateTime" -> OptionType(ClassType(classOf[Instant])),
        "endDate"       -> OptionType(ClassType(classOf[LocalDate])),
        "formatters" -> MapType(
          FieldType.STRING,
          ClassType(classOf[DateTimeFormatter])
        )
      )
    ) shouldBe Map(
      JavaLangPrefix                 -> Set("String"),
      JavaTimePrefix                 -> Set("LocalDate", "Instant"),
      List("java", "time", "format") -> Set("DateTimeFormatter")
    )
  }
}
