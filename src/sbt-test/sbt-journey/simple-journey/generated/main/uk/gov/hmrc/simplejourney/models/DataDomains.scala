package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{JsPath,Reads}

case class DataDomains(
  dataDomain: String
)

object DataDomains {
  given dataDomainsReads: Reads[DataDomains] =
    (JsPath \ "dataDomain").read[String].map(DataDomains.apply)
}
