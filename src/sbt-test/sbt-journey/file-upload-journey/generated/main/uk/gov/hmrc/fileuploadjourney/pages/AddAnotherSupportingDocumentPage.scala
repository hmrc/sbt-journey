package uk.gov.hmrc.fileuploadjourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.{IdxPathNode,JsPath,KeyPathNode}
import uk.gov.hmrc.fileuploadjourney.models.Choice

case class AddAnotherSupportingDocumentPage private (override val path: JsPath) extends QuestionPage[Choice] {
  override def toString: String = "addAnotherSupportingDocument"
}

object AddAnotherSupportingDocumentPage {
  def apply(addSupportingDocuments: Choice, supportingDocumentsIndex: Int): AddAnotherSupportingDocumentPage =
    new AddAnotherSupportingDocumentPage(
      JsPath \ "addSupportingDocuments" \ addSupportingDocuments.toString \ "supportingDocuments" \ supportingDocumentsIndex
    )

  def unapply(page: AddAnotherSupportingDocumentPage): Option[(Choice, Int)] =
    page.path.path match {
      case KeyPathNode("addSupportingDocuments")
        :: KeyPathNode(addSupportingDocuments)
        :: KeyPathNode("supportingDocuments")
        :: IdxPathNode(supportingDocumentsIndex)
        :: Nil => Some((Choice.valueOf(addSupportingDocuments), supportingDocumentsIndex))
      case _ => None
    }
}
