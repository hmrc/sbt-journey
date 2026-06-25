package uk.gov.hmrc.fileuploadjourney.models.upscan

import play.api.libs.json.{Format, Reads, Writes}
import play.api.mvc.{PathBindable, QueryStringBindable}

opaque type UpscanReference = String

object UpscanReference {
  def apply(reference: String): UpscanReference = reference

  extension (self: UpscanReference) {
    def reference: String = self
  }

  given PathBindable[UpscanReference] = PathBindable.bindableString

  given QueryStringBindable[UpscanReference] = QueryStringBindable.bindableString

  given Format[UpscanReference] = Format(Reads.StringReads, Writes.StringWrites)
}
