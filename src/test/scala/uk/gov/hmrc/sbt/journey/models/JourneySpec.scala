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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{kebabCase, pascalCase}

class JourneySpec extends AnyFlatSpec with Matchers {
  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

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

  val cipAssessmentTicket = journeyPage("cipAssessmentTicket", FieldType.STRING)
  val addATaxRegime       = journeyPage("addATaxRegime", FieldType.BOOLEAN)
  val addAnotherTaxRegime = journeyPage("addAnotherTaxRegime", FieldType.BOOLEAN)
  val taxRegime =
    journeyPage("taxRegime", ClassType(basePackage / "models" / "TaxRegime"))
  val whichTaxRegime =
    journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
  val contactDetailsPage =
    journeyPage("contactDetails", ClassType(basePackage / "models" / "ContactDetails"))
  val saInfo                = journeyPage("saInfo", FieldType.STRING)
  val vatInfo               = journeyPage("vatInfo", FieldType.STRING)
  val auditSource           = journeyPage("auditSource", FieldType.STRING)
  val auditEvent            = journeyPage("auditEvent", FieldType.STRING)
  val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)
  val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)

  val pages = List(
    cipAssessmentTicket,
    addATaxRegime,
    taxRegime,
    addAnotherTaxRegime,
    taxRegime,
    whichTaxRegime,
    contactDetailsPage,
    saInfo,
    vatInfo
  )

  val journey = Journey(
    pages.map(page => page.pageKey -> page).toMap,
    List(
      SinglePagePart(cipAssessmentTicket.pageKey, None),
      IfThenPart(
        addATaxRegime.pageKey,
        List(
          DoWhilePart(
            addAnotherTaxRegime.pageKey,
            List(SinglePagePart(taxRegime.pageKey, None)),
            "taxRegimes"
          )
        ),
        None
      ),
      SwitchCasePart(
        whichTaxRegime.pageKey,
        Map(
          "SA" -> List(
            SinglePagePart(contactDetailsPage.pageKey, None),
            SinglePagePart(saInfo.pageKey, None)
          ),
          "VAT" -> List(
            SinglePagePart(contactDetailsPage.pageKey, None),
            SinglePagePart(vatInfo.pageKey, None)
          )
        ),
        None
      )
    )
  )

  "Journey.startPage" should "return a single page when it is the first journey part" in {
    journey.startPage shouldBe cipAssessmentTicket.pageKey
  }

  it should "return the choice page of an if-then subjourney when it is the first journey part" in {
    val journey = Journey(
      Map.empty,
      List(
        IfThenPart(
          addATaxRegime.pageKey,
          List(
            DoWhilePart(
              addAnotherTaxRegime.pageKey,
              List(SinglePagePart(taxRegime.pageKey, None)),
              "taxRegimes"
            )
          ),
          None
        )
      )
    )

    journey.startPage shouldBe addATaxRegime.pageKey
  }

  it should "return the choice page of a switch-case subjourney when it is the first journey part" in {
    val journey = Journey(
      Map.empty,
      List(
        SwitchCasePart(
          whichTaxRegime.pageKey,
          Map(
            "SA" -> List(
              SinglePagePart(contactDetailsPage.pageKey, None),
              SinglePagePart(saInfo.pageKey, None)
            ),
            "VAT" -> List(
              SinglePagePart(contactDetailsPage.pageKey, None),
              SinglePagePart(vatInfo.pageKey, None)
            )
          ),
          None
        )
      )
    )

    journey.startPage shouldBe whichTaxRegime.pageKey
  }

  it should "return the first page of a do-while subjourney when it is the first journey part" in {
    val journey = Journey(
      Map.empty,
      List(
        DoWhilePart(
          addAnotherAuditSource.pageKey,
          List(
            SinglePagePart(auditSource.pageKey, None),
            DoWhilePart(
              addAnotherAuditEvent.pageKey,
              List(SinglePagePart(auditEvent.pageKey, None)),
              "auditEvents"
            )
          ),
          "auditSources"
        )
      )
    )

    journey.startPage shouldBe auditSource.pageKey
  }

  "Journey.pathsFor" should "return the answer path for a page that only appears once at the root level" in {
    val path = Root / StringPath(cipAssessmentTicket.pageKey)
    journey.pathsFor(cipAssessmentTicket.pageKey) shouldBe List(path)
  }

  it should "return the answer path for a page that is part of an optional add-to-list journey" in {
    val path = Root /
      ChoicePath(addATaxRegime.pageKey, "Yes") /
      IndexPath("taxRegimes") /
      StringPath(taxRegime.pageKey)

    journey.pathsFor(taxRegime.pageKey) shouldBe List(path)
  }

  it should "return the answer paths for a page that is in one case of a switch-case journey" in {
    val saPath = Root /
      ChoicePath(whichTaxRegime.pageKey, "SA") /
      StringPath(saInfo.pageKey)

    journey.pathsFor(saInfo.pageKey) shouldBe List(saPath)
  }

  it should "return the answer paths for a page that is in multiple cases of a switch-case journey" in {
    val saPath = Root /
      ChoicePath(whichTaxRegime.pageKey, "SA") /
      StringPath(contactDetailsPage.pageKey)

    val vatPath = Root /
      ChoicePath(whichTaxRegime.pageKey, "VAT") /
      StringPath(contactDetailsPage.pageKey)

    journey.pathsFor(contactDetailsPage.pageKey) shouldBe List(saPath, vatPath)
  }
}
