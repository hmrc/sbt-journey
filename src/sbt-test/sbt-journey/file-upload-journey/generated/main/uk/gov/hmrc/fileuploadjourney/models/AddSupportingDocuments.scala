package uk.gov.hmrc.fileuploadjourney.models

import play.api.libs.json.{JsPath,Reads}

enum AddSupportingDocuments {
  case Yes(
    supportingDocuments: List[SupportingDocuments]
  )
  case No

  def choice: Choice = this match {
    case Yes(_) => Choice.Yes
    case No => Choice.No
  }
}

object AddSupportingDocuments extends EnumFormats {
  private val yesReads: Reads[AddSupportingDocuments] = {
    val supportingDocuments = Reads.list(Reads.at[SupportingDocuments](JsPath \ "uploadSupportingDocument"))
    (JsPath \ "supportingDocuments").read[List[SupportingDocuments]](using supportingDocuments).map(Yes.apply)
  }
  private val nestedYesReads: Reads[AddSupportingDocuments] =
    (JsPath \ "Yes").read[AddSupportingDocuments](using yesReads)

  given reads: Reads[AddSupportingDocuments] = enumReads(
    "Yes" -> nestedYesReads,
    "No" -> Reads.pure(No)
  )
}
