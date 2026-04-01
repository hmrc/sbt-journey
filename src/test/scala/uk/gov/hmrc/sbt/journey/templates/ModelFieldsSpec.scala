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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{kebabCase, pascalCase}

import java.time.LocalDate

class ModelFieldsSpec extends AnyFlatSpec with Matchers {
  val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
  val modelsPackage = basePackage / "models"

  def journeyPage(pageKey: String, answerType: FieldType) = JourneyPage(
    pageKey,
    s"$pageKey.title",
    s"$pageKey.heading",
    s"/${kebabCase(pageKey)}",
    s"/change-${kebabCase(pageKey)}",
    (basePackage / "controllers" / s"Default${pascalCase(pageKey)}Controller").toString,
    (basePackage / "forms" / s"Default${pascalCase(pageKey)}FormProvider").toString,
    s"views.html.${pascalCase(pageKey)}View",
    withDefaultController = true,
    withDefaultFormProvider = true,
    answerType
  )

  "ModelFields.field" should "return a field declaration for an Int field" in {
    ModelFields.field(
      "numberOfSamples",
      FieldType.INT
    ) shouldBe s"  numberOfSamples: Int"
  }

  it should "return a field declaration for a Boolean field" in {
    ModelFields.field(
      "areYouSendingSamples",
      FieldType.BOOLEAN
    ) shouldBe s"  areYouSendingSamples: Boolean"
  }

  it should "return a field declaration for a String field" in {
    ModelFields.field(
      "serviceName",
      FieldType.STRING
    ) shouldBe s"  serviceName: String"
  }

  it should "return a field declaration for a List field" in {
    ModelFields.field(
      "cipAssessmentTickets",
      ListType(FieldType.STRING)
    ) shouldBe s"  cipAssessmentTickets: List[String]"
  }

  it should "return a field declaration for an Option field" in {
    ModelFields.field(
      "expectedDecommissioningDate",
      OptionType(ClassType(classOf[LocalDate]))
    ) shouldBe s"  expectedDecommissioningDate: Option[LocalDate]"
  }

  it should "return a field declaration for a synthetic class field" in {
    ModelFields.field(
      "whichTaxRegime",
      SyntheticClassType(modelsPackage.toString, "WhichTaxRegime")
    ) shouldBe s"  whichTaxRegime: WhichTaxRegime"
  }

  "ModelFields.forPart" should "return the answer type for a single page part" in {
    val cipAssessmentTicket =
      journeyPage("cipAssessmentTicket", FieldType.STRING)
    val cipAssessmentPage =
      journeyPage("cipAssessmentPage", FieldType.STRING)

    val cipAssessmentPagePart = SinglePagePart("cipAssessmentPage", None)

    val journey = Journey(
      pages = Map(
        "cipAssessmentTicket" -> cipAssessmentTicket,
        "cipAssessmentPage"   -> cipAssessmentPage
      ),
      journey = List.empty
    )

    ModelFields.forPart(
      modelsPackage,
      journey,
      cipAssessmentPagePart
    ) shouldBe List("cipAssessmentPage" -> FieldType.STRING)
  }

  it should "use the underlying answer type for a do-while journey with only one question per entry" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val doWhilePart = DoWhilePart(
      "addAnotherAuditEvent",
      List(SinglePagePart("auditEvent", None)),
      "auditEvents"
    )

    val journey = Journey(
      pages = Map(
        "auditEvent"           -> auditEvent,
        "addAnotherAuditEvent" -> addAnotherAuditEvent
      ),
      journey = List.empty
    )

    ModelFields.forPart(
      modelsPackage,
      journey,
      doWhilePart
    ) shouldBe List("auditEvents" -> ListType(ClassType(classOf[String])))
  }

  it should "use a synthetic class answer type for a do-while journey with multiple questions per entry" in {
    val auditSource           = journeyPage("auditSource", FieldType.STRING)
    val auditEvent            = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)
    val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val innerDoWhilePart = DoWhilePart(
      "addAnotherAuditEvent",
      List(SinglePagePart("auditEvent", None)),
      "auditEvents"
    )

    val outerDoWhilePart = DoWhilePart(
      "addAnotherAuditSource",
      List(SinglePagePart("auditSource", None), innerDoWhilePart),
      "auditSources"
    )

    val journey = Journey(
      pages = Map(
        "auditSource"           -> auditSource,
        "addAnotherAuditSource" -> addAnotherAuditSource,
        "auditEvent"            -> auditEvent,
        "addAnotherAuditEvent"  -> addAnotherAuditEvent
      ),
      journey = List.empty
    )

    ModelFields.forPart(
      modelsPackage,
      journey,
      outerDoWhilePart
    ) shouldBe List(
      "auditSources" -> ListType(SyntheticClassType(modelsPackage.toString, "AuditSources"))
    )
  }

  it should "use a synthetic class answer type for an if-then journey" in {
    val addATaxRegime =
      journeyPage("addATaxRegime", ClassType(basePackage / "Choice"))
    val taxRegime =
      journeyPage("taxRegime", FieldType.STRING)

    val ifThenPart = IfThenPart(
      addATaxRegime.pageKey,
      List(SinglePagePart(taxRegime.pageKey, None)),
      None
    )

    val journey = Journey(
      pages = Map(
        "addATaxRegime" -> addATaxRegime,
        "taxRegime"     -> taxRegime
      ),
      journey = List.empty
    )

    ModelFields.forPart(
      modelsPackage,
      journey,
      ifThenPart
    ) shouldBe List("addATaxRegime" -> SyntheticClassType(modelsPackage.toString, "AddATaxRegime"))
  }

  it should "use a synthetic class answer type for a switch-case journey" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val switchCasePart = SwitchCasePart(
      whichTaxRegime.pageKey,
      Map(
        "SA"  -> List(SinglePagePart(saInfo.pageKey, None)),
        "VAT" -> List(SinglePagePart(vatInfo.pageKey, None))
      ),
      None
    )

    val journey = Journey(
      pages = Map(
        "whichTaxRegime" -> whichTaxRegime,
        "saInfo"         -> saInfo,
        "vatInfo"        -> vatInfo
      ),
      journey = List.empty
    )

    ModelFields.forPart(
      modelsPackage,
      journey,
      switchCasePart
    ) shouldBe List(
      "whichTaxRegime" -> SyntheticClassType(modelsPackage.toString, "WhichTaxRegime")
    )
  }

  "ModelFields.forParts" should "return all of the fields for a given list of journey parts" in {
    val addATaxRegime =
      journeyPage("addATaxRegime", ClassType(basePackage / "Choice"))
    val taxRegime =
      journeyPage("taxRegime", FieldType.STRING)
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val journeyPages = List(addATaxRegime, taxRegime, whichTaxRegime, vatInfo, saInfo)

    val journeyParts =
      List(
        IfThenPart("addATaxRegime", List(SinglePagePart("taxRegime", None)), None),
        SwitchCasePart(
          "whichTaxRegime",
          Map(
            "SA"  -> List(SinglePagePart("saInfo", None)),
            "VAT" -> List(SinglePagePart("vatInfo", None))
          ),
          None
        )
      )

    val journey = Journey(
      pages = journeyPages.map(p => p.pageKey -> p).toMap,
      journey = List.empty
    )

    ModelFields.forParts(modelsPackage, journey, journeyParts) shouldBe List(
      "addATaxRegime"  -> SyntheticClassType(modelsPackage.toString, "AddATaxRegime"),
      "whichTaxRegime" -> SyntheticClassType(modelsPackage.toString, "WhichTaxRegime")
    )
  }
}
