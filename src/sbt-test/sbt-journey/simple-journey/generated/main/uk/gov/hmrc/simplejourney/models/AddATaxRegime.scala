package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{JsPath,Reads}

enum AddATaxRegime {
  case Yes(
    taxRegime: Set[TaxRegime]
  )
  case No

  def choice: Choice = this match {
    case Yes(_) => Choice.Yes
    case No => Choice.No
  }
}

object AddATaxRegime extends EnumFormats {
  private val yesReads: Reads[AddATaxRegime] =
    (JsPath \ "taxRegime").read[Set[TaxRegime]].map(Yes.apply)
  private val nestedYesReads: Reads[AddATaxRegime] =
    (JsPath \ "Yes").read[AddATaxRegime](using yesReads)

  given reads: Reads[AddATaxRegime] = enumReads(
    "Yes" -> nestedYesReads,
    "No" -> Reads.pure(No)
  )
}
