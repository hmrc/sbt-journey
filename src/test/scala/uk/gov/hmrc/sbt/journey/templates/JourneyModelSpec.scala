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

class JourneyModelSpec extends AnyFlatSpec with Matchers {
  val basePackage   = QualifiedName("uk.gov.hmrc.sbtjourneytest")
  val modelsPackage = basePackage / "models"

  def journeyPage(pageKey: String, answerType: FieldType) = JourneyPage(
    pageKey,
    s"$pageKey.title",
    s"$pageKey.heading",
    s"/${kebabCase(pageKey)}",
    s"/change-${kebabCase(pageKey)}",
    (basePackage / "controllers" / s"${pascalCase(pageKey)}BaseController").toString,
    (basePackage / "forms" / s"${pascalCase(pageKey)}BaseFormProvider").toString,
    s"views.html.${pascalCase(pageKey)}View",
    withDefaultController = true,
    withDefaultFormProvider = true,
    answerType
  )

  val journeyModel = new JourneyModel(
    Map(
      "whichTaxRegime" -> journeyPage("whichTaxRegime", ClassType(modelsPackage / "TaxRegime")),
      "whereDomiciled" -> journeyPage("whereDomiciled", ClassType(modelsPackage / "Domicile"))
    ),
    Map(
      "Choice"    -> EnumModel("Choice", List("Yes", "No")),
      "TaxRegime" -> EnumModel("TaxRegime", List("SA", "VAT")),
      "Domicile" -> EnumModel("Domicile", List("ENGLAND_WALES", "SCOTLAND", "NORTHERN_IRELAND", "OTHER"))
    )
  )

  "JourneyModel.forSwitchCase" should "render an enum model for a switch-case journey part with no subjourneys" in {
    val switchCasePart = SwitchCasePart(
      "whichTaxRegime",
      Map("SA" -> List.empty, "VAT" -> List.empty),
      None
    )
    journeyModel.forSwitchCase(
      modelsPackage,
      switchCasePart,
      "whichTaxRegime",
      "WhichTaxRegime",
      Map("SA" -> List.empty, "VAT" -> List.empty)
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.functional.syntax.*
        |import play.api.libs.json.{JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads}
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
    val switchCasePart = SwitchCasePart(
      "whichTaxRegime",
      Map(
        "SA"  -> List(SinglePagePart("saInfo", None)),
        "VAT" -> List(SinglePagePart("vatInfo", None))
      ),
      None
    )
    journeyModel.forSwitchCase(
      modelsPackage,
      switchCasePart,
      "whichTaxRegime",
      "WhichTaxRegime",
      Map(
        "SA"  -> List("saInfo" -> FieldType.STRING),
        "VAT" -> List("vatInfo" -> FieldType.STRING)
      )
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.functional.syntax.*
        |import play.api.libs.json.{JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads}
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
        |  private val nestedSaReads: Reads[WhichTaxRegime] =
        |    (JsPath \ "SA").read[WhichTaxRegime](using saReads)
        |  private val vatReads: Reads[WhichTaxRegime] =
        |    (JsPath \ "vatInfo").read[String].map(VAT.apply)
        |  private val nestedVatReads: Reads[WhichTaxRegime] =
        |    (JsPath \ "VAT").read[WhichTaxRegime](using vatReads)
        |
        |  given reads(using config: JsonConfiguration): Reads[WhichTaxRegime] = Reads {
        |    case obj: JsObject => obj.value.get(config.discriminator) match {
        |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        |        case sa if sa == config.typeNaming("SA") =>
        |          nestedSaReads.reads(obj)
        |        case vat if vat == config.typeNaming("VAT") =>
        |          nestedVatReads.reads(obj)
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

  it should "render an enum model for a switch-case journey part with subjourneys that doesn't cover every case" in {
    val switchCasePart = SwitchCasePart(
      "whereDomiciled",
      Map(
        "OTHER"  -> List(SinglePagePart("iht401", None)),
        "SCOTLAND" -> List(SinglePagePart("legitimFundDischarged", None))
      ),
      None
    )
    journeyModel.forSwitchCase(
      modelsPackage,
      switchCasePart,
      "whereDomiciled",
      "WhereDomiciled",
      Map(
        "OTHER"  -> List("iht401" -> FieldType.STRING),
        "SCOTLAND" -> List("legitimFundDischarged" -> FieldType.BOOLEAN)
      )
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.functional.syntax.*
        |import play.api.libs.json.{JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads}
        |
        |enum WhereDomiciled {
        |  case OTHER(
        |    iht401: String
        |  )
        |  case SCOTLAND(
        |    legitimFundDischarged: Boolean
        |  )
        |  case ENGLAND_WALES
        |  case NORTHERN_IRELAND
        |}
        |
        |object WhereDomiciled {
        |  private val otherReads: Reads[WhereDomiciled] =
        |    (JsPath \ "iht401").read[String].map(OTHER.apply)
        |  private val nestedOtherReads: Reads[WhereDomiciled] =
        |    (JsPath \ "OTHER").read[WhereDomiciled](using otherReads)
        |  private val scotlandReads: Reads[WhereDomiciled] =
        |    (JsPath \ "legitimFundDischarged").read[Boolean].map(SCOTLAND.apply)
        |  private val nestedScotlandReads: Reads[WhereDomiciled] =
        |    (JsPath \ "SCOTLAND").read[WhereDomiciled](using scotlandReads)
        |
        |  given reads(using config: JsonConfiguration): Reads[WhereDomiciled] = Reads {
        |    case obj: JsObject => obj.value.get(config.discriminator) match {
        |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        |        case other if other == config.typeNaming("OTHER") =>
        |          nestedOtherReads.reads(obj)
        |        case scotland if scotland == config.typeNaming("SCOTLAND") =>
        |          nestedScotlandReads.reads(obj)
        |        case englandWales if englandWales == config.typeNaming("ENGLAND_WALES") =>
        |          JsSuccess(ENGLAND_WALES)
        |        case northernIreland if northernIreland == config.typeNaming("NORTHERN_IRELAND") =>
        |          JsSuccess(NORTHERN_IRELAND)
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

  it should "render an enum model for a switch-case journey part with subjourneys that doesn't cover every case but has a default" in {
    val switchCasePart = SwitchCasePart(
      "whereDomiciled",
      Map(
        "OTHER"  -> List(SinglePagePart("iht401", None)),
        "SCOTLAND" -> List(SinglePagePart("legitimFundDischarged", None)),
        "default" -> List(SinglePagePart("longTermResident", None))
      ),
      None
    )
    journeyModel.forSwitchCase(
      modelsPackage,
      switchCasePart,
      "whereDomiciled",
      "WhereDomiciled",
      Map(
        "OTHER"  -> List("iht401" -> FieldType.STRING),
        "SCOTLAND" -> List("legitimFundDischarged" -> FieldType.BOOLEAN),
        "default" -> List("longTermResident" -> FieldType.BOOLEAN)
      )
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.models
        |
        |import play.api.libs.functional.syntax.*
        |import play.api.libs.json.{JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads}
        |
        |enum WhereDomiciled {
        |  case OTHER(
        |    iht401: String
        |  )
        |  case SCOTLAND(
        |    legitimFundDischarged: Boolean
        |  )
        |  case default(
        |    longTermResident: Boolean
        |  )
        |}
        |
        |object WhereDomiciled {
        |  private val otherReads: Reads[WhereDomiciled] =
        |    (JsPath \ "iht401").read[String].map(OTHER.apply)
        |  private val nestedOtherReads: Reads[WhereDomiciled] =
        |    (JsPath \ "OTHER").read[WhereDomiciled](using otherReads)
        |  private val scotlandReads: Reads[WhereDomiciled] =
        |    (JsPath \ "legitimFundDischarged").read[Boolean].map(SCOTLAND.apply)
        |  private val nestedScotlandReads: Reads[WhereDomiciled] =
        |    (JsPath \ "SCOTLAND").read[WhereDomiciled](using scotlandReads)
        |  private val defaultReads: Reads[WhereDomiciled] =
        |    (JsPath \ "longTermResident").read[Boolean].map(default.apply)
        |  private val nestedDefaultReads: Reads[WhereDomiciled] =
        |    (JsPath \ "default").read[WhereDomiciled](using defaultReads)
        |
        |  given reads(using config: JsonConfiguration): Reads[WhereDomiciled] = Reads {
        |    case obj: JsObject => obj.value.get(config.discriminator) match {
        |      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        |        case other if other == config.typeNaming("OTHER") =>
        |          nestedOtherReads.reads(obj)
        |        case scotland if scotland == config.typeNaming("SCOTLAND") =>
        |          nestedScotlandReads.reads(obj)
        |        case _ =>
        |          nestedDefaultReads.reads(obj)
        |      }
        |      case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
        |    }
        |    case _ => JsError("error.expected.jsobject")
        |  }
        |}
        |""".stripMargin
  }

  "JourneyModel.forIfThen" should "render an enum model for an if-then journey part" in {
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
    journeyModel.forIfThen(
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
    journeyModel
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
