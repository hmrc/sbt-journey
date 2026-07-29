package uk.gov.hmrc.fileuploadjourney.models.upscan

enum UploadError(val errorCode: String, val messageKey: String) {
  // Relevant entries from https://docs.aws.amazon.com/AmazonS3/latest/developerguide/ErrorResponses.html#ErrorCodeList
  case EntityTooSmall extends UploadError("EntityTooSmall", "upload.error.fileTooSmall")
  case EntityTooLarge extends UploadError("EntityTooLarge", "upload.error.fileTooLarge")

  case Other(code: String) extends UploadError(code, "upload.error.other")
}

object UploadError {
  private val knownErrors: Set[UploadError] = Set(EntityTooSmall, EntityTooLarge)

  def fromErrorCode(errorCode: String): UploadError =
    knownErrors
      .find(_.errorCode == errorCode)
      .getOrElse(UploadError.Other(errorCode))
}
