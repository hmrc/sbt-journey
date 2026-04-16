package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes
import uk.gov.hmrc.simplejourney.models.Choice

case class AddAnotherAuditSourcePage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[Choice] {
  override def submitRoute(mode: Mode): Call = makeRoute(mode)
  override def toString: String = "addAnotherAuditSource"
}

object AddAnotherAuditSourcePage {
  def apply(auditSourcesIndex: Int): AddAnotherAuditSourcePage =
    new AddAnotherAuditSourcePage(
      JsPath \ "auditSources" \ auditSourcesIndex,
      mode => routes.AddAnotherAuditSourceBaseController.onSubmit(auditSourcesIndex, mode)
    )

  def unapply(page: AddAnotherAuditSourcePage): Option[Int] =
    page.path.path match {
      case KeyPathNode("auditSources")
        :: IdxPathNode(auditSourcesIndex)
        :: Nil => Some(auditSourcesIndex)
      case _ => None
    }
}
