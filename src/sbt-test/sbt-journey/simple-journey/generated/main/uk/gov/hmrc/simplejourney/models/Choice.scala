package uk.gov.hmrc.simplejourney.models

import models.Enumerable // import uk.gov.hmrc.simplejourney.models.Enumerable
import play.api.libs.json.{Format,JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads,Writes}

enum Choice {
  case Yes, No
}

object Choice {
  given reads(using config: JsonConfiguration): Reads[Choice] = Reads {
    case obj: JsObject => obj.value.get(config.discriminator) match {
      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        case yes if yes == config.typeNaming("Yes") =>
          JsSuccess(Yes)
        case no if no == config.typeNaming("No") =>
          JsSuccess(No)
        case _ =>
          JsError("error.invalid")
      }
      case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
    }
    case _ => JsError("error.expected.jsobject")
  }

  given writes(using config: JsonConfiguration): Writes[Choice] = Writes {
    case Yes =>
      Json.obj(config.discriminator -> config.typeNaming("Yes"))
    case No =>
      Json.obj(config.discriminator -> config.typeNaming("No"))
  }

  given Format[Choice] = Format(reads, writes)

  given Enumerable[Choice] = (value: String) => fromString(value)

  def fromString(value: String): Option[Choice] =
    values.find(_.toString == value)
}
