package uk.gov.hmrc.fileuploadjourney.models

import play.api.libs.json.{Format, Reads, Writes}
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.mongo.play.json.formats.MongoUuidFormats

import java.util.UUID

opaque type UploadId = UUID

object UploadId {
  def apply(id: UUID): UploadId = id

  def next(): UploadId = UUID.randomUUID()

  extension (self: UploadId) {
    def id: UUID = self
  }

  given PathBindable[UploadId] = PathBindable.bindableUUID

  given QueryStringBindable[UploadId] = QueryStringBindable.bindableUUID

  given Format[UploadId] = Format(Reads.uuidReads, Writes.UuidWrites)

  trait MongoFormat {
    given Format[UploadId] = MongoUuidFormats.uuidFormat
  }
}
