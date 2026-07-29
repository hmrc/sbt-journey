package uk.gov.hmrc.fileuploadjourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{IdxPathNode,JsPath,KeyPathNode}
import uk.gov.hmrc.fileuploadjourney.models.Choice

case class IsDocumentConfidentialPage private (override val path: JsPath) extends QuestionPage[Boolean] {
  override def toString: String = "isDocumentConfidential"
}

object IsDocumentConfidentialPage {
  def apply(addSupportingDocuments: Choice, supportingDocumentsIndex: Int): IsDocumentConfidentialPage =
    new IsDocumentConfidentialPage(
      JsPath \ "addSupportingDocuments" \ addSupportingDocuments.toString \ "supportingDocuments" \ supportingDocumentsIndex \ "isDocumentConfidential"
    )

  def unapply(page: IsDocumentConfidentialPage): Option[(Choice, Int)] =
    page.path.path match {
      case KeyPathNode("addSupportingDocuments")
        :: KeyPathNode(addSupportingDocuments)
        :: KeyPathNode("supportingDocuments")
        :: IdxPathNode(supportingDocumentsIndex)
        :: KeyPathNode("isDocumentConfidential")
        :: Nil => Some((Choice.valueOf(addSupportingDocuments), supportingDocumentsIndex))
      case _ => None
    }
}
