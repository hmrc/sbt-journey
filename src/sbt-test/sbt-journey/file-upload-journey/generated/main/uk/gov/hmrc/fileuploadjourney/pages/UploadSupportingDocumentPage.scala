package uk.gov.hmrc.fileuploadjourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{IdxPathNode,JsPath,KeyPathNode}
import uk.gov.hmrc.fileuploadjourney.models.{Choice,UploadId}

case class UploadSupportingDocumentPage private (override val path: JsPath) extends QuestionPage[UploadId] {
  override def toString: String = "uploadSupportingDocument"
}

object UploadSupportingDocumentPage {
  def apply(addSupportingDocuments: Choice, supportingDocumentsIndex: Int): UploadSupportingDocumentPage =
    new UploadSupportingDocumentPage(
      JsPath \ "addSupportingDocuments" \ addSupportingDocuments.toString \ "supportingDocuments" \ supportingDocumentsIndex \ "uploadSupportingDocument"
    )

  def unapply(page: UploadSupportingDocumentPage): Option[(Choice, Int)] =
    page.path.path match {
      case KeyPathNode("addSupportingDocuments")
        :: KeyPathNode(addSupportingDocuments)
        :: KeyPathNode("supportingDocuments")
        :: IdxPathNode(supportingDocumentsIndex)
        :: KeyPathNode("uploadSupportingDocument")
        :: Nil => Some((Choice.valueOf(addSupportingDocuments), supportingDocumentsIndex))
      case _ => None
    }
}
