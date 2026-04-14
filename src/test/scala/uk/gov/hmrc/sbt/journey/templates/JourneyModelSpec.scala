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

class JourneyModelSpec extends AnyFlatSpec with Matchers {
  "JourneyModel.forSwitchCase" should "render an enum model for a switch-case journey part with no subjourneys" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    val switchCasePart = SwitchCasePart(
      "whichTaxRegime",
      Map("SA" -> List.empty, "VAT" -> List.empty),
      None
    )
    JourneyModel.forSwitchCase(
      modelsPackage,
      switchCasePart,
      "WhichTaxRegime",
      Map("SA" -> List.empty, "VAT" -> List.empty)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.functional.syntax.*
        |import play.api.libs.json.{JsError,JsObject,JsPath,JsValue,Json,JsonConfiguration,Reads}
        |
        |enum WhichTaxRegime {
        |  case SA
        |  case VAT
        |}
        |
        |object WhichTaxRegime {
        |  given reads(using config: JsonConfiguration): Reads[WhichTaxRegime] = Reads {
        |    case obj: JsObject => obj.value.get(config.discriminator) match {
        |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        |        case sa if sa == config.typeNaming("SA") =>
        |          JsSuccess(SA)
        |        case vat if vat == config.typeNaming("VAT") =>
        |          JsSuccess(VAT)
        |        case _ =>
        |          JsError("error.invalid")
        |      }
        |      case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
        |    }
        |    case _ => JsError("error.expected.jsobject")
        |  }
        |}
        |""".stripMargin
  }

  it should "render an enum model for a switch-case journey part with subjourneys" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    val switchCasePart = SwitchCasePart(
      "whichTaxRegime",
      Map(
        "SA"  -> List(SinglePagePart("saInfo", None)),
        "VAT" -> List(SinglePagePart("vatInfo", None))
      ),
      None
    )
    JourneyModel.forSwitchCase(
      modelsPackage,
      switchCasePart,
      "WhichTaxRegime",
      Map(
        "SA"  -> List("saInfo" -> FieldType.STRING),
        "VAT" -> List("vatInfo" -> FieldType.STRING)
      )
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.functional.syntax.*
        |import play.api.libs.json.{JsError,JsObject,JsPath,JsValue,Json,JsonConfiguration,Reads}
        |
        |enum WhichTaxRegime {
        |  case SA(
        |    saInfo: String
        |  )
        |  case VAT(
        |    vatInfo: String
        |  )
        |}
        |
        |object WhichTaxRegime {
        |  private val saReads: Reads[WhichTaxRegime] =
        |    (JsPath \ "saInfo").read[String].map(SA.apply)
        |  private val nestedSAReads: Reads[WhichTaxRegime] =
        |    (JsPath \ "SA").read[WhichTaxRegime](using saReads)
        |  private val vatReads: Reads[WhichTaxRegime] =
        |    (JsPath \ "vatInfo").read[String].map(VAT.apply)
        |  private val nestedVATReads: Reads[WhichTaxRegime] =
        |    (JsPath \ "VAT").read[WhichTaxRegime](using vatReads)
        |
        |  given reads(using config: JsonConfiguration): Reads[WhichTaxRegime] = Reads {
        |    case obj: JsObject => obj.value.get(config.discriminator) match {
        |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        |        case sa if sa == config.typeNaming("SA") =>
        |          nestedSAReads.reads(obj)
        |        case vat if vat == config.typeNaming("VAT") =>
        |          nestedVATReads.reads(obj)
        |        case _ =>
        |          JsError("error.invalid")
        |      }
        |      case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
        |    }
        |    case _ => JsError("error.expected.jsobject")
        |  }
        |}
        |""".stripMargin
  }

  "JourneyModel.forIfThen" should "render an enum model for an if-then journey part" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    val ifThenPart = IfThenPart(
      "addATaxRegime",
      List(
        DoWhilePart(
          "addAnotherTaxRegime",
          List(SinglePagePart("taxRegime", None)),
          "taxRegimes"
        )
      ),
      None
    )
    JourneyModel.forIfThen(
      modelsPackage,
      ifThenPart,
      "AddATaxRegime",
      List("taxRegimes" -> ListType(ClassType(modelsPackage / "TaxRegime")))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.functional.syntax.*
        |import play.api.libs.json.{JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads}
        |
        |enum AddATaxRegime {
        |  case Yes(
        |    taxRegimes: List[TaxRegime]
        |  )
        |  case No
        |
        |  def choice: Choice = this match {
        |    case Yes(_) => Choice.Yes
        |    case No => Choice.No
        |  }
        |}
        |
        |object AddATaxRegime {
        |  private val yesReads: Reads[AddATaxRegime] = {
        |    val taxRegimes = Reads.list(Reads.at[TaxRegime](JsPath \ "taxRegime"))
        |    (JsPath \ "taxRegimes").read[List[TaxRegime]](using taxRegimes).map(Yes.apply)
        |  }
        |  private val nestedYesReads: Reads[AddATaxRegime] =
        |    (JsPath \ "Yes").read[AddATaxRegime](using yesReads)
        |
        |  given reads(using config: JsonConfiguration): Reads[AddATaxRegime] = Reads {
        |    case obj: JsObject => obj.value.get(config.discriminator) match {
        |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        |        case yes if yes == config.typeNaming("Yes") =>
        |          nestedYesReads.reads(obj)
        |        case no  if no  == config.typeNaming("No")  =>
        |          JsSuccess(No)
        |        case _ =>
        |          JsError("error.invalid")
        |      }
        |      case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
        |    }
        |    case _ => JsError("error.expected.jsobject")
        |  }
        |}
        |""".stripMargin
  }

  "JourneyModel.forDoWhile" should "render a case class model for a do-while journey part" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    val doWhilePart = DoWhilePart(
      "addAnotherAuditSource",
      List(
        SinglePagePart("auditSource", None),
        DoWhilePart(
          "addAnotherAuditEvent",
          List(SinglePagePart("auditEvent", None)),
          "auditEvents"
        )
      ),
      "auditSources"
    )
    JourneyModel
      .forDoWhile(
        modelsPackage,
        doWhilePart,
        "AuditSources",
        List(
          "auditSource" -> FieldType.STRING,
          "auditEvents" -> ListType(ClassType(modelsPackage / "AuditEvent"))
        )
      ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Json, JsPath, Reads}
        |import play.api.libs.functional.syntax.*
        |
        |
        |case class AuditSources(
        |  auditSource: String,
        |  auditEvents: List[AuditEvent]
        |)
        |
        |object AuditSources {
        |  given auditSourcesReads: Reads[AuditSources] = {
        |    val auditEvents = Reads.list(Reads.at[AuditEvent](JsPath \ "auditEvent"))
        |    (
        |      (JsPath \ "auditSource").read[String] and
        |      (JsPath \ "auditEvents").read[List[AuditEvent]](using auditEvents)
        |    )(AuditSources.apply)
        |  }
        |}
        |""".stripMargin
  }
}
