package uk.gov.hmrc.simplejourney.pages

import models.Mode // uk.gov.hmrc.simplejourney.models.Mode
import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
import play.api.mvc.Call
import uk.gov.hmrc.simplejourney.controllers.routes


case class AuditSourcePage private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[String] {
  override def submitRoute(mode: Mode): Call = makeRoute(mode)
  override def toString: String = "auditSource"
}

object AuditSourcePage {
  def apply(auditSourcesIndex: Int): AuditSourcePage =
    new AuditSourcePage(
      JsPath \ "auditSources" \ auditSourcesIndex \ "auditSource",
      mode => routes.AuditSourceBaseController.onSubmit(auditSourcesIndex, mode)
    )

  def unapply(page: AuditSourcePage): Option[Int] =
    page.path.path match {
      case KeyPathNode("auditSources")
        :: IdxPathNode(auditSourcesIndex)
        :: KeyPathNode("auditSource")
        :: Nil => Some(auditSourcesIndex)
      case _ => None
    }
}
