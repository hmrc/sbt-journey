package uk.gov.hmrc.fileuploadjourney.models

import play.api.libs.json.{JsPath,Reads}

enum AddConfidentialInformation {
  case Yes(
    provideConfidentialInformation: String
  )
  case No

  def choice: Choice = this match {
    case Yes(_) => Choice.Yes
    case No => Choice.No
  }
}

object AddConfidentialInformation extends EnumFormats {
  private val yesReads: Reads[AddConfidentialInformation] =
    (JsPath \ "provideConfidentialInformation").read[String].map(Yes.apply)
  private val nestedYesReads: Reads[AddConfidentialInformation] =
    (JsPath \ "Yes").read[AddConfidentialInformation](using yesReads)

  given reads: Reads[AddConfidentialInformation] = enumReads(
    "Yes" -> nestedYesReads,
    "No" -> Reads.pure(No)
  )
}
