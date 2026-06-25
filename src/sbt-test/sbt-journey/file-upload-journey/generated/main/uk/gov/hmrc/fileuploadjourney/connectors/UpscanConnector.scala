package uk.gov.hmrc.fileuploadjourney.connectors

import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.fileuploadjourney.models.upscan.{UpscanInitiateRequest, UpscanInitiateResponse}

import javax.inject.{Inject,Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(using
  ExecutionContext
) extends ServicesConfig(config) {
  private val upscanInitiateUrl = url"${baseUrl("upscan")}/upscan/v2/initiate"

  def initiate(callbackUrl: Call, successRedirect: Call, errorRedirect: Call)(using
    RequestHeader,
    HeaderCarrier
  ): Future[UpscanInitiateResponse] = {
    val request = UpscanInitiateRequest(
      callbackUrl.absoluteURL(),
      successRedirect = Some(successRedirect.absoluteURL()),
      errorRedirect = Some(errorRedirect.absoluteURL())
    )

    httpClient
      .post(upscanInitiateUrl)
      .withBody(Json.toJson(request))
      .execute[UpscanInitiateResponse]
  }
}
