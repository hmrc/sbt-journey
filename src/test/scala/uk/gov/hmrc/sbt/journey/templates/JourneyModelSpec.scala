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
import uk.gov.hmrc.sbt.journey.models.{ClassType, FieldType, ListType, QualifiedName}

class JourneyModelSpec extends AnyFlatSpec with Matchers {
  "JourneyModel.forSwitchCase" should "render an enum model for a switch-case journey part with no subjourneys" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    JourneyModel.forSwitchCase(
      modelsPackage,
      "WhichTaxRegime",
      Map("SA" -> List.empty, "VAT" -> List.empty)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Format,JsError,JsObject,JsPath,JsValue,Json,JsonConfiguration,Reads,Writes}
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
        |
        |  given writes(using config: JsonConfiguration): Writes[WhichTaxRegime] = Writes {
        |    case sa: SA =>
        |      Json.obj(config.discriminator -> config.typeNaming("SA"))
        |    case vat: VAT =>
        |      Json.obj(config.discriminator -> config.typeNaming("VAT"))
        |  }
        |
        |  given Format[WhichTaxRegime] = Format(reads, writes)
        |}
        |""".stripMargin
  }

  it should "render an enum model for a switch-case journey part with subjourneys" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    JourneyModel.forSwitchCase(
      modelsPackage,
      "WhichTaxRegime",
      Map(
        "SA"  -> List("saInfo" -> FieldType.STRING),
        "VAT" -> List("vatInfo" -> FieldType.STRING)
      )
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Format,JsError,JsObject,JsPath,JsValue,Json,JsonConfiguration,Reads,Writes}
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
        |  val saReads: Reads[SA] = Json.reads[SA]
        |  val nestedSAReads: Reads[SA] = Reads.at(JsPath \ "SA")(saReads)
        |
        |  val vatReads: Reads[VAT] = Json.reads[VAT]
        |  val nestedVATReads: Reads[VAT] = Reads.at(JsPath \ "VAT")(vatReads)
        |
        |  val saWrites: Writes[SA] = Json.writes[SA]
        |  val vatWrites: Writes[VAT] = Json.writes[VAT]
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
        |
        |  given writes(using config: JsonConfiguration): Writes[WhichTaxRegime] = Writes {
        |    case sa: SA =>
        |      Json.obj(config.discriminator -> config.typeNaming("SA"), "SA" -> saWrites.writes(sa))
        |    case vat: VAT =>
        |      Json.obj(config.discriminator -> config.typeNaming("VAT"), "VAT" -> vatWrites.writes(vat))
        |  }
        |
        |  given Format[WhichTaxRegime] = Format(reads, writes)
        |}
        |""".stripMargin
  }

  "JourneyModel.forIfThen" should "render an enum model for an if-then journey part" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    JourneyModel.forIfThen(
      modelsPackage,
      "AddATaxRegime",
      List("taxRegimes" -> ListType(ClassType(modelsPackage / "TaxRegime")))
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Format,JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads,Writes}
        |
        |enum AddATaxRegime {
        |  case Yes(
        |    taxRegimes: List[TaxRegime]
        |  )
        |  case No
        |
        |  def choice: Choice = this match {
        |    case Yes(_) => Choice.Yes
        |    case No     => Choice.No
        |  }
        |}
        |
        |object AddATaxRegime {
        |  val yesReads: Reads[Yes] = Json.reads[Yes]
        |  val yesWrites: Writes[Yes] = Json.writes[Yes]
        |  val nestedYesReads: Reads[Yes] = Reads.at(JsPath \ "Yes")(yesReads)
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
        |
        |  given writes(using config: JsonConfiguration): Writes[AddATaxRegime] = Writes {
        |    case yes: Yes =>
        |      Json.obj(config.discriminator -> config.typeNaming("Yes"), "Yes" -> yesWrites.writes(yes))
        |    case No =>
        |      Json.obj(config.discriminator -> config.typeNaming("No"))
        |  }
        |
        |  given Format[AddATaxRegime] = Format(reads, writes)
        |}
        |""".stripMargin
  }

  "JourneyModel.forDoWhile" should "render a case class model for a do-while journey part" in {
    val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
    val modelsPackage = basePackage / "models"
    JourneyModel
      .forDoWhile(
        modelsPackage,
        "AuditSources",
        List(
          "auditSource" -> FieldType.STRING,
          "auditEvents" -> ListType(ClassType(modelsPackage / "AuditEvent"))
        )
      ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.json.{Json, Reads}
        |
        |
        |case class AuditSources(
        |  auditSource: String,
        |  auditEvents: List[AuditEvent]
        |)
        |
        |object AuditSources {
        |  given Reads[AuditSources] = Json.reads[AuditSources]
        |}
        |""".stripMargin
  }
}
