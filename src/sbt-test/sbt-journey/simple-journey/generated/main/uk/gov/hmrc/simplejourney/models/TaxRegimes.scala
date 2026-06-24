package uk.gov.hmrc.simplejourney.models

import play.api.libs.functional.syntax.*
import play.api.libs.json.{JsPath,Reads}

case class TaxRegimes(
  taxRegime: TaxRegime
)

object TaxRegimes {
  given taxRegimesReads: Reads[TaxRegimes] =
    (JsPath \ "taxRegime").read[TaxRegime].map(TaxRegimes.apply)
}
