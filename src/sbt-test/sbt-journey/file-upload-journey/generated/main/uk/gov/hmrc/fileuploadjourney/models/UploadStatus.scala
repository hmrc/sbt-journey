package uk.gov.hmrc.fileuploadjourney.models

import play.api.libs.json.Format

enum UploadStatus extends Enum[UploadStatus] {
  case Initiated, Processing, Failed, Ready
}

object UploadStatus {
  given format: Format[UploadStatus] = Format.of[String]
    .bimap(UploadStatus.valueOf, _.productPrefix)
}
