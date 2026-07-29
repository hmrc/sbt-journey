package queries

import play.api.libs.json.JsPath
import uk.gov.hmrc.simplejourney.models.AddATaxRegime

case object AddATaxRegimeQuery extends Gettable[AddATaxRegime] {
  override def path: JsPath = JsPath \ "addATaxRegime"
}
