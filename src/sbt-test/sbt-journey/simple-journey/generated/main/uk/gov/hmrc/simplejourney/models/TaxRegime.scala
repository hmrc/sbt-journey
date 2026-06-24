package uk.gov.hmrc.simplejourney.models

import models.Enumerable // import uk.gov.hmrc.simplejourney.models.Enumerable
import play.api.libs.json.{Format,Json,JsonConfiguration,Reads,Writes}

enum TaxRegime {
  case SA, VAT
}

object TaxRegime extends EnumFormats {
  given reads: Reads[TaxRegime] = enumReads(
    "SA" -> Reads.pure(SA),
    "VAT" -> Reads.pure(VAT)
  )

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
