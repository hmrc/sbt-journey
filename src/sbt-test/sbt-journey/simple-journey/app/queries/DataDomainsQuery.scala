package queries

import play.api.libs.json.JsPath
import uk.gov.hmrc.simplejourney.models.DataDomains

case object DataDomainsQuery extends Gettable[List[DataDomains]] {
  override def path: JsPath = JsPath \ "dataDomains"
}
