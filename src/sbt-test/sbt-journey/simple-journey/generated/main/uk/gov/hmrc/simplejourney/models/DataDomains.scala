package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{Json, JsPath, Reads}
import play.api.libs.functional.syntax.*


case class DataDomains(
  dataDomain: String
)

object DataDomains {
  given dataDomainsReads: Reads[DataDomains] =
    (JsPath \ "dataDomain").read[String].map(DataDomains.apply)
}
