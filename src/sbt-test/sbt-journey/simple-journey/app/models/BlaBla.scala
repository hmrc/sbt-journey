package models

import play.api.libs.json.{Format, JsError, JsObject, JsPath, JsSuccess, Json, JsonConfiguration, Reads, Writes}

enum BlaBla {
  case Yes(mmm: String, aaaa: Boolean)
  case No
}

object BlaBla {
  given Reads[Yes] = Json.reads[Yes]

  given Writes[Yes] = Json.writes[Yes]

  given reads(using config: JsonConfiguration): Reads[BlaBla] = Reads {
    case obj: JsObject =>
      obj.value.get(config.discriminator) match {
        case Some(jsDiscriminator) =>
          lazy val input = obj.value.getOrElse("_value", obj)
          jsDiscriminator.validate[String].flatMap {
            case yes if yes == config.typeNaming("models.BlaBla.Yes") =>
              summon[Reads[Yes]].reads(input)
            case no if no == config.typeNaming("models.BlaBla.No") =>
              JsSuccess(No)
            case _ =>
              JsError("error.invalid")
          }
        case _ => JsError(JsPath \ config.discriminator, "error.missing.path")
      }
    case _ => JsError("error.expected.jsobject")
  }

  given writes(using config: JsonConfiguration): Writes[BlaBla] = Writes {
    case yes: Yes =>
      val output = summon[Writes[Yes]].writes(yes) match {
        case obj: JsObject => obj
        case wrapped => Json.obj("_value" -> wrapped)
      }

      output ++ Json.obj(config.discriminator -> config.typeNaming("models.BlaBla.Yes"))

    case No =>
      Json.obj(config.discriminator -> config.typeNaming("models.BlaBla.No"))
  }

  given Format[BlaBla] = Format(reads, writes)
}