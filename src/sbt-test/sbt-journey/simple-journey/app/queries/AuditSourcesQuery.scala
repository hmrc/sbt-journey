package queries

import play.api.libs.json.JsPath
import uk.gov.hmrc.simplejourney.models.AuditSources

case object AuditSourcesQuery extends Gettable[List[AuditSources]] {
  override def path: JsPath = JsPath \ "auditSources"
}
