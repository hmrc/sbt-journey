package uk.gov.hmrc.simplejourney.models

import models.Enumerable // import uk.gov.hmrc.simplejourney.models.Enumerable
import play.api.libs.json.{Format,JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads,Writes}

enum Choice {
  case Yes, No
}

object Choice extends EnumFormats {
  given reads: Reads[Choice] = enumReads(
    "Yes" -> Reads.pure(Yes),
    "No" -> Reads.pure(No)
  )

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
