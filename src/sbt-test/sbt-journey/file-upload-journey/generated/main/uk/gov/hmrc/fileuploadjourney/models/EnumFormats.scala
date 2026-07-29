package uk.gov.hmrc.fileuploadjourney.models

import play.api.libs.json.{JsonConfiguration, JsError, JsObject, JsPath, Reads}

trait EnumFormats {
  def enumReads[A <: Product](
    cases: (String, Reads[? <: A])*
  )(using config: JsonConfiguration): Reads[A] =
    enumReads(Reads.failed("error.invalid"), cases*)

  def enumReads[A <: Product](
    default: => Reads[? <: A],
    cases: (String, Reads[? <: A])*
  )(using config: JsonConfiguration): Reads[A] =
    discriminatedReads(
      config.discriminator,
      default,
      cases.map { case (dsc, fn) => (config.typeNaming(dsc), fn) }*
    )

  def discriminatedReads[A](
    discriminatorKey: String,
    cases: (String, Reads[? <: A])*
  )(using config: JsonConfiguration): Reads[A] =
    discriminatedReads(discriminatorKey, Reads.failed("error.invalid"), cases*)

  def discriminatedReads[A](
    discriminatorKey: String,
    default: => Reads[? <: A],
    cases: (String, Reads[? <: A])*
  )(using config: JsonConfiguration): Reads[A] = {
    val caseReads = cases.toMap
    Reads {
      case obj: JsObject =>
        obj.value.get(discriminatorKey) match {
          case Some(discriminatorValue) =>
            discriminatorValue.validate[String].flatMap { discriminator =>
              caseReads.getOrElse(discriminator, default).reads(obj)
            }
          case _ => JsError(JsPath \ discriminatorKey, "error.missing.path")
        }
      case _ => JsError("error.expected.jsobject")
    }
  }
}

object EnumFormats extends EnumFormats
