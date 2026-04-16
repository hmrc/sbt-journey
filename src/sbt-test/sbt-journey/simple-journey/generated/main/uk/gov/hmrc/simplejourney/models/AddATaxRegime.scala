package uk.gov.hmrc.simplejourney.models

import play.api.libs.functional.syntax.*
import play.api.libs.json.{JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads}

enum AddATaxRegime {
  case Yes(
    taxRegimes: List[TaxRegime]
  )
  case No

  def choice: Choice = this match {
    case Yes(_) => Choice.Yes
    case No => Choice.No
  }
}

object AddATaxRegime {
  private val yesReads: Reads[AddATaxRegime] = {
    val taxRegimes = Reads.list(Reads.at[TaxRegime](JsPath \ "taxRegime"))
    (JsPath \ "taxRegimes").read[List[TaxRegime]](using taxRegimes).map(Yes.apply)
  }
  private val nestedYesReads: Reads[AddATaxRegime] =
    (JsPath \ "Yes").read[AddATaxRegime](using yesReads)

  given reads(using config: JsonConfiguration): Reads[AddATaxRegime] = Reads {
    case obj: JsObject => obj.value.get(config.discriminator) match {
      case Some(jsDiscriminator) => jsDiscriminator.validate[String].flatMap {
        case yes if yes == config.typeNaming("Yes") =>
          nestedYesReads.reads(obj)
        case no  if no  == config.typeNaming("No")  =>
          JsSuccess(No)
        case _ =>
          JsError("error.invalid")
      }
      case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
    }
    case _ => JsError("error.expected.jsobject")
  }
}
