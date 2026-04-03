package uk.gov.hmrc.simplejourney.models

import models.Enumerable // import uk.gov.hmrc.simplejourney.models.Enumerable
import play.api.libs.json.{Format,JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads,Writes}

enum TaxRegime {
  case SA, VAT
}

object TaxRegime {
  given reads(using config: JsonConfiguration): Reads[TaxRegime] = Reads {
    case obj: JsObject => obj.value.get(config.discriminator) match {
      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        case sa if sa == config.typeNaming("SA") =>
          JsSuccess(SA)
        case vat if vat == config.typeNaming("VAT") =>
          JsSuccess(VAT)
        case _ =>
          JsError("error.invalid")
      }
      case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
    }
    case _ => JsError("error.expected.jsobject")
  }

  given writes(using config: JsonConfiguration): Writes[TaxRegime] = Writes {
    case SA =>
      Json.obj(config.discriminator -> config.typeNaming("SA"))
    case VAT =>
      Json.obj(config.discriminator -> config.typeNaming("VAT"))
  }

  given Format[TaxRegime] = Format(reads, writes)

  given Enumerable[TaxRegime] = (value: String) => fromString(value)

  def fromString(value: String): Option[TaxRegime] =
    values.find(_.toString == value)
}
