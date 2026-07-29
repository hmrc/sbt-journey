package uk.gov.hmrc.fileuploadjourney.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse, HeaderNames as HmrcHeaderNames}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.fileuploadjourney.models.upscan.*

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import java.util.UUID

class UpscanConnectorSpec extends AsyncFlatSpec, Matchers, HttpClientV2Support, WireMockSupport {
  private val config = Configuration(
    "appName"                           -> "file-upload-journey",
    "microservice.services.upscan.host" -> wireMockHost,
    "microservice.services.upscan.port" -> wireMockPort
  )

  private val upscanConnector = new UpscanConnector(config, httpClientV2)

  private val sessionId = UUID.randomUUID().toString
  private val requestId = UUID.randomUUID().toString
  given request: RequestHeader = FakeRequest().withHeaders(
    HeaderNames.HOST           -> s"$wireMockHost:$wireMockPort",
    HmrcHeaderNames.xSessionId -> sessionId,
    HmrcHeaderNames.xRequestId -> requestId
  )
  given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

  private val fileReference  = UUID.randomUUID().toString
  private val responseTime   = Instant.now().atZone(ZoneOffset.UTC)
  private val formatter      = DateTimeFormatter.ISO_ZONED_DATE_TIME
  private val shortFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")

  private val initiateResponse = UpscanInitiateResponse(
    reference = UpscanReference(fileReference),
    uploadRequest = UpscanFormTemplate(
      href = "http://localhost:9570/upscan/upload-proxy",
      fields = Map(
        "success_action_redirect" -> s"http://localhost:$wireMockPort/file-upload-journey/check-your-answers?key=$fileReference",
        "x-amz-credential"                    -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
        "x-amz-meta-upscan-initiate-response" -> formatter.format(responseTime),
        "x-amz-meta-original-filename"        -> "${filename}",
        "x-amz-algorithm"                     -> "AWS4-HMAC-SHA256",
        "x-amz-signature"                     -> "xxxx",
        "error_action_redirect"   -> "http://localhost:$wireMockPort/file-upload-journey/journey-recovery",
        "x-amz-meta-session-id"   -> sessionId,
        "x-amz-meta-callback-url" -> "http://localhost:$wireMockPort/file-upload-journey/",
        "x-amz-date"              -> shortFormatter.format(responseTime),
        "x-amz-meta-upscan-initiate-received" -> formatter.format(responseTime),
        "x-amz-meta-request-id"               -> requestId,
        "key"                                 -> fileReference,
        "acl"                                 -> "private",
        "x-amz-meta-consuming-service"        -> "file-upload-journey",
        "policy" -> "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMCwxMDQ4NTc2MDBdXX0="
      )
    )
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    // This is usually set on startup when the routes are instantiated
    app.RoutesPrefix.setPrefix("/file-upload-journey")
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    // A valid request
    stubFor(
      post(urlPathEqualTo("/upscan/v2/initiate"))
        .atPriority(1)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
        .withHeader(HeaderNames.USER_AGENT, equalTo("file-upload-journey"))
        .withRequestBody(equalToJson(s"""{
        |  "callbackUrl": "$wireMockUrl/file-upload-journey",
        |  "successRedirect": "$wireMockUrl/file-upload-journey/check-your-answers",
        |  "errorRedirect": "$wireMockUrl/file-upload-journey/there-is-a-problem"
        |}""".stripMargin))
        .willReturn(
          okJson(
            Json.stringify(
              Json.obj(
                "reference" -> fileReference,
                "uploadRequest" -> Json.obj(
                  "href" -> "http://localhost:9570/upscan/upload-proxy",
                  "fields" -> Json.obj(
                    "success_action_redirect" -> s"http://localhost:$wireMockPort/file-upload-journey/check-your-answers?key=$fileReference",
                    "x-amz-credential" -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
                    "x-amz-meta-upscan-initiate-response" -> formatter.format(responseTime),
                    "x-amz-meta-original-filename"        -> "${filename}",
                    "x-amz-algorithm"                     -> "AWS4-HMAC-SHA256",
                    "x-amz-signature"                     -> "xxxx",
                    "error_action_redirect" -> "http://localhost:$wireMockPort/file-upload-journey/journey-recovery",
                    "x-amz-meta-session-id"   -> sessionId,
                    "x-amz-meta-callback-url" -> "http://localhost:$wireMockPort/file-upload-journey/",
                    "x-amz-date"              -> shortFormatter.format(responseTime),
                    "x-amz-meta-upscan-initiate-received" -> formatter.format(responseTime),
                    "x-amz-meta-request-id"               -> requestId,
                    "key"                                 -> fileReference,
                    "acl"                                 -> "private",
                    "x-amz-meta-consuming-service"        -> "file-upload-journey",
                    "policy" -> "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMCwxMDQ4NTc2MDBdXX0="
                  )
                )
              )
            )
          )
        )
    )

    // A request where the body is missing
    stubFor(
      post(urlPathEqualTo("/upscan/v2/initiate"))
        .atPriority(2)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
        .withHeader(HeaderNames.USER_AGENT, equalTo("file-upload-journey"))
        .willReturn(badRequest().withBody("""{
                                            |  "statusCode": 415,
                                            |  "message": "Expecting text/json or application/json body"
                                            |}""".stripMargin))
    )

    // A request where the user agent is missing
    stubFor(
      post(urlPathEqualTo("/upscan/v2/initiate"))
        .atPriority(3)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
        .willReturn(badRequest().withBody(s"Missing ${HeaderNames.USER_AGENT} Header"))
    )
  }

  "UpscanConnector" should "return the upscan-initiate response when the request is successful" in {
    upscanConnector
      .initiate(
        callbackUrl = controllers.routes.IndexController.onPageLoad(),
        successRedirect = controllers.routes.CheckYourAnswersController.onPageLoad(),
        errorRedirect = controllers.routes.JourneyRecoveryController.onPageLoad()
      )
      .map {
        _ shouldBe initiateResponse
      }
  }

  it should "throw UpstreamErrorResponse if upscan-initiate returns a client error" in {
    stubFor(
      post(urlPathEqualTo("/upscan/v2/initiate"))
        .atPriority(1)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
        .withHeader(HeaderNames.USER_AGENT, equalTo("file-upload-journey"))
        .willReturn(
          aResponse()
            .withStatus(Status.UNSUPPORTED_MEDIA_TYPE)
            .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
            .withBody("""{
            |  "statusCode": 415,
            |  "message": "Expecting text/json or application/json body"
            |}""".stripMargin)
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      upscanConnector
        .initiate(
          callbackUrl = controllers.routes.IndexController.onPageLoad(),
          successRedirect = controllers.routes.CheckYourAnswersController.onPageLoad(),
          errorRedirect = controllers.routes.JourneyRecoveryController.onPageLoad()
        )
    }.map { error =>
      error.statusCode shouldBe Status.UNSUPPORTED_MEDIA_TYPE
      error.reportAs shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  it should "throw UpstreamErrorResponse if upscan-initiate returns a server error" in {
    stubFor(
      post(urlPathEqualTo("/upscan/v2/initiate"))
        .atPriority(1)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
        .withHeader(HeaderNames.USER_AGENT, equalTo("file-upload-journey"))
        .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      upscanConnector
        .initiate(
          callbackUrl = controllers.routes.IndexController.onPageLoad(),
          successRedirect = controllers.routes.CheckYourAnswersController.onPageLoad(),
          errorRedirect = controllers.routes.JourneyRecoveryController.onPageLoad()
        )
    }.map { error =>
      error.statusCode shouldBe Status.INTERNAL_SERVER_ERROR
      error.reportAs shouldBe Status.BAD_GATEWAY
    }
  }
}
