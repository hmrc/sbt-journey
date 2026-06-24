/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.sbt.journey.templates

import uk.gov.hmrc.sbt.journey.models.QualifiedName

object UpscanModel extends Template {
  def upscanReference(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |import play.api.libs.json.{Format, Reads, Writes}
       |import play.api.mvc.{PathBindable, QueryStringBindable}
       |
       |opaque type UpscanReference = String
       |
       |object UpscanReference {
       |  def apply(reference: String): UpscanReference = reference
       |
       |  extension (self: UpscanReference) {
       |    def reference: String = self
       |  }
       |
       |  given PathBindable[UpscanReference] = PathBindable.bindableString
       |
       |  given QueryStringBindable[UpscanReference] = QueryStringBindable.bindableString
       |
       |  given Format[UpscanReference] = Format(Reads.StringReads, Writes.StringWrites)
       |}
       |""".stripMargin
  }

  def upscanInitiateRequest(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |import play.api.libs.json.{Json, OWrites}
       |
       |case class UpscanInitiateRequest(
       |  callbackUrl: String,
       |  successRedirect: Option[String],
       |  errorRedirect: Option[String],
       |  minimumFileSize: Option[Long] = None,
       |  maximumFileSize: Option[Long] = None
       |)
       |
       |object UpscanInitiateRequest {
       |  given OWrites[UpscanInitiateRequest] = Json.writes[UpscanInitiateRequest]
       |}
       |""".stripMargin
  }

  def upscanFormTemplate(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |import play.api.libs.json.{Json, Reads}
       |
       |case class UpscanFormTemplate(
       |  href: String,
       |  fields: Map[String, String]
       |)
       |
       |object UpscanFormTemplate {
       |  given Reads[UpscanFormTemplate] = Json.reads[UpscanFormTemplate]
       |}
       |""".stripMargin
  }

  def upscanInitiateResponse(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |import play.api.libs.json.{Json, Reads}
       |
       |case class UpscanInitiateResponse(
       |  reference: UpscanReference,
       |  uploadRequest: UpscanFormTemplate
       |)
       |
       |object UpscanInitiateResponse {
       |  given Reads[UpscanInitiateResponse] = Json.reads[UpscanInitiateResponse]
       |}
       |""".stripMargin
  }

  def uploadError(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |enum UploadError(val errorCode: String, val messageKey: String) {
       |  // Relevant entries from https://docs.aws.amazon.com/AmazonS3/latest/developerguide/ErrorResponses.html#ErrorCodeList
       |  case EntityTooSmall extends UploadError("EntityTooSmall", "upload.error.fileTooSmall")
       |  case EntityTooLarge extends UploadError("EntityTooLarge", "upload.error.fileTooLarge")
       |
       |  case Other(code: String) extends UploadError(code, "upload.error.other")
       |}
       |
       |object UploadError {
       |  private val knownErrors: Set[UploadError] = Set(EntityTooSmall, EntityTooLarge)
       |
       |  def fromErrorCode(errorCode: String): UploadError =
       |    knownErrors
       |      .find(_.errorCode == errorCode)
       |      .getOrElse(UploadError.Other(errorCode))
       |}
       |""".stripMargin
  }

  def uploadDetails(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |import java.time.Instant
       |import play.api.libs.json.{Json, OFormat}
       |
       |case class UploadDetails(
       |  fileName: String,
       |  fileMimeType: String,
       |  uploadTimestamp: Instant,
       |  checksum: String,
       |  size: Long
       |)
       |
       |object UploadDetails {
       |  given format: OFormat[UploadDetails] = Json.format[UploadDetails]
       |}
       |""".stripMargin
  }

  def failureDetails(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |import play.api.libs.json.{Json, OFormat}
       |
       |case class FailureDetails(
       |  failureReason: String,
       |  message: String
       |)
       |
       |object FailureDetails {
       |  given format: OFormat[FailureDetails] = Json.format[FailureDetails]
       |}
       |""".stripMargin
  }

  def upscanNotification(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models" / "upscan"}
       |
       |import java.net.URI
       |import play.api.libs.json.{Json, Reads}
       |import ${basePackage / "models" / "EnumFormats"}
       |
       |enum UpscanNotification {
       |  def reference: UpscanReference
       |  case Ready(reference: UpscanReference, downloadUrl: URI, uploadDetails: UploadDetails)
       |  case Failed(reference: UpscanReference, failureDetails: FailureDetails)
       |}
       |
       |object UpscanNotification extends EnumFormats {
       |  private val readyReads: Reads[Ready] = Json.reads[Ready]
       |  private val failedReads: Reads[Failed] = Json.reads[Failed]
       |
       |  given Reads[UpscanNotification] = discriminatedReads(
       |    "fileStatus",
       |    "READY" -> readyReads,
       |    "FAILED" -> failedReads
       |  )
       |}
       |""".stripMargin
  }

  def uploadId(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models"}
       |
       |import play.api.libs.json.{Format, Reads, Writes}
       |import play.api.mvc.{PathBindable, QueryStringBindable}
       |import uk.gov.hmrc.mongo.play.json.formats.MongoUuidFormats
       |
       |import java.util.UUID
       |
       |opaque type UploadId = UUID
       |
       |object UploadId {
       |  def apply(id: UUID): UploadId = id
       |
       |  def next(): UploadId = UUID.randomUUID()
       |
       |  extension (self: UploadId) {
       |    def id: UUID = self
       |  }
       |
       |  given PathBindable[UploadId] = PathBindable.bindableUUID
       |
       |  given QueryStringBindable[UploadId] = QueryStringBindable.bindableUUID
       |
       |  given Format[UploadId] = Format(Reads.uuidReads, Writes.UuidWrites)
       |
       |  trait MongoFormat {
       |    given Format[UploadId] = MongoUuidFormats.uuidFormat
       |  }
       |}
       |""".stripMargin
  }

  def uploadStatus(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models"}
       |
       |import play.api.libs.json.Format
       |
       |enum UploadStatus extends Enum[UploadStatus] {
       |  case Initiated, Processing, Failed, Ready
       |}
       |
       |object UploadStatus {
       |  given format: Format[UploadStatus] = Format.of[String]
       |    .bimap(UploadStatus.valueOf, _.productPrefix)
       |}
       |""".stripMargin
  }

  def fileUpload(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "models"}
       |
       |import play.api.libs.json.{Json, JsonConfiguration, OFormat, OWrites, Reads}
       |import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
       |import ${basePackage / "models" / "upscan"}.{UpscanReference, UploadDetails, FailureDetails}
       |
       |import java.net.URI
       |import java.time.Instant
       |
       |enum FileUpload {
       |  def id: UploadId
       |  def userId: String
       |  def reference: UpscanReference
       |  case Initiated(id: UploadId, userId: String, reference: UpscanReference, initiatedAt: Instant)
       |  case Processing(id: UploadId, userId: String, reference: UpscanReference, updatedAt: Instant)
       |  case Failed(id: UploadId, userId: String, reference: UpscanReference, failureDetails: FailureDetails, updatedAt: Instant)
       |  case Ready(id: UploadId, userId: String, reference: UpscanReference, downloadUrl: URI, uploadDetails: UploadDetails, updatedAt: Instant)
       |}
       |
       |object FileUpload extends EnumFormats, UploadId.MongoFormat, MongoJavatimeFormats.Implicits {
       |  private val initiatedFormat: OFormat[Initiated] = Json.format[Initiated]
       |  private val processingFormat: OFormat[Processing] = Json.format[Processing]
       |  private val failedFormat: OFormat[Failed] = Json.format[Failed]
       |  private val readyFormat: OFormat[Ready] = Json.format[Ready]
       |
       |  given reads: Reads[FileUpload] = discriminatedReads(
       |    "uploadStatus",
       |    "Initiated" -> initiatedFormat,
       |    "Processing" -> processingFormat,
       |    "Failed" -> failedFormat,
       |    "Ready" -> readyFormat
       |  )
       |
       |  given writes(using config: JsonConfiguration): OWrites[FileUpload] = OWrites {
       |    case initiated: Initiated =>
       |      Json.obj("uploadStatus" -> "Initiated") ++ initiatedFormat.writes(initiated)
       |    case processing: Processing =>
       |      Json.obj("uploadStatus" -> "Processing") ++ processingFormat.writes(processing)
       |    case failed: Failed =>
       |      Json.obj("uploadStatus" -> "Failed") ++ failedFormat.writes(failed)
       |    case ready: Ready =>
       |      Json.obj("uploadStatus" -> "Ready") ++ readyFormat.writes(ready)
       |  }
       |
       |  given format: OFormat[FileUpload] = OFormat(reads, writes)
       |}
       |""".stripMargin
  }
}
