package uk.gov.hmrc.simplejourney.models

import play.api.libs.functional.syntax.*
import play.api.libs.json.{JsPath,Reads}

enum AddATaxRegime {
  case Yes(
    taxRegimes: List[TaxRegime]
  )
  case No

  def choice: Choice = this match {
    case Yes(_) => Choice.Yes
    case No => Choice.No
  }
}

object AddATaxRegime extends EnumFormats {
  private val yesReads: Reads[AddATaxRegime] = {
    val taxRegimes = Reads.list(Reads.at[TaxRegime](JsPath \ "taxRegime"))
    (JsPath \ "taxRegimes").read[List[TaxRegime]](using taxRegimes).map(Yes.apply)
  }
  private val nestedYesReads: Reads[AddATaxRegime] =
    (JsPath \ "Yes").read[AddATaxRegime](using yesReads)

  given reads: Reads[AddATaxRegime] = enumReads(
    "Yes" -> nestedYesReads,
    "No" -> Reads.pure(No)
  )
}
