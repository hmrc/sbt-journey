package uk.gov.hmrc.simplejourney.models

import play.api.libs.json.{Format,JsError,JsObject,JsPath,JsSuccess,JsValue,Json,JsonConfiguration,Reads,Writes}

enum AddATaxRegime {
  case Yes(
    taxRegimes: List[TaxRegime]
  )
  case No

  def choice: Choice = this match {
    case Yes(_) => Choice.Yes
    case No     => Choice.No
  }
}

object AddATaxRegime {
  val yesReads: Reads[Yes] = Json.reads[Yes]
  val yesWrites: Writes[Yes] = Json.writes[Yes]
  val nestedYesReads: Reads[Yes] = Reads.at(JsPath \ "Yes")(yesReads)

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

  given writes(using config: JsonConfiguration): Writes[AddATaxRegime] = Writes {
    case yes: Yes =>
      Json.obj(config.discriminator -> config.typeNaming("Yes"), "Yes" -> yesWrites.writes(yes))
    case No =>
      Json.obj(config.discriminator -> config.typeNaming("No"))
  }

  given Format[AddATaxRegime] = Format(reads, writes)
}
