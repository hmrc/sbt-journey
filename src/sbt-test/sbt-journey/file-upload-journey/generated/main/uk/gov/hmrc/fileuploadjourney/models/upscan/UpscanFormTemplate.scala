package uk.gov.hmrc.fileuploadjourney.models.upscan

import play.api.libs.json.{Json, Reads}

case class UpscanFormTemplate(
  href: String,
  fields: Map[String, String]
)

object UpscanFormTemplate {
  given Reads[UpscanFormTemplate] = Json.reads[UpscanFormTemplate]
}
