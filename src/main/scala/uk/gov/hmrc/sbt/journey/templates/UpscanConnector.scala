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

object UpscanConnector {
  def render(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "connectors"}
       |
       |import play.api.Configuration
       |import play.api.libs.json.Json
       |import play.api.libs.ws.writeableOf_JsValue
       |import play.api.mvc.{Call, RequestHeader}
       |import uk.gov.hmrc.http.HttpReads.Implicits.*
       |import uk.gov.hmrc.http.client.HttpClientV2
       |import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
       |import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
       |import ${basePackage / "models" / "upscan"}.{UpscanInitiateRequest, UpscanInitiateResponse}
       |
       |import javax.inject.{Inject,Singleton}
       |import scala.concurrent.{ExecutionContext, Future}
       |
       |@Singleton
       |class UpscanConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(using
       |  ExecutionContext
       |) extends ServicesConfig(config) {
       |  private val upscanInitiateUrl = url"$${baseUrl("upscan")}/upscan/v2/initiate"
       |
       |  def initiate(callbackUrl: Call, successRedirect: Call, errorRedirect: Call)(using
       |    RequestHeader,
       |    HeaderCarrier
       |  ): Future[UpscanInitiateResponse] = {
       |    val request = UpscanInitiateRequest(
       |      callbackUrl.absoluteURL(),
       |      successRedirect = Some(successRedirect.absoluteURL()),
       |      errorRedirect = Some(errorRedirect.absoluteURL())
       |    )
       |
       |    httpClient
       |      .post(upscanInitiateUrl)
       |      .withBody(Json.toJson(request))
       |      .execute[UpscanInitiateResponse]
       |  }
       |}
       |""".stripMargin
  }

  def renderSpec(serviceName: String, basePackage: QualifiedName): String = {
    s"""package ${basePackage / "connectors"}
       |
       |import com.github.tomakehurst.wiremock.client.WireMock
       |import com.github.tomakehurst.wiremock.client.WireMock.*
       |import org.scalatest.flatspec.AsyncFlatSpec
       |import org.scalatest.matchers.should.Matchers
       |import play.api.Configuration
       |import play.api.http.{ContentTypes, HeaderNames, Status}
       |import play.api.libs.json.Json
       |import play.api.mvc.RequestHeader
       |import play.api.test.FakeRequest
       |import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
       |import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse, HeaderNames as HmrcHeaderNames}
       |import uk.gov.hmrc.play.http.HeaderCarrierConverter
       |import ${basePackage / "models" / "upscan" / "*"}
       |
       |import java.time.format.DateTimeFormatter
       |import java.time.{Instant, ZoneOffset}
       |import java.util.UUID
       |
       |class UpscanConnectorSpec extends AsyncFlatSpec, Matchers, HttpClientV2Support, WireMockSupport {
       |  private val config = Configuration(
       |    "appName"                           -> "${serviceName}",
       |    "microservice.services.upscan.host" -> wireMockHost,
       |    "microservice.services.upscan.port" -> wireMockPort
       |  )
       |
       |  private val upscanConnector = new UpscanConnector(config, httpClientV2)
       |
       |  private val sessionId = UUID.randomUUID().toString
       |  private val requestId = UUID.randomUUID().toString
       |  given request: RequestHeader = FakeRequest().withHeaders(
       |    HeaderNames.HOST           -> s"$$wireMockHost:$$wireMockPort",
       |    HmrcHeaderNames.xSessionId -> sessionId,
       |    HmrcHeaderNames.xRequestId -> requestId
       |  )
       |  given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
       |
       |  private val fileReference  = UUID.randomUUID().toString
       |  private val responseTime   = Instant.now().atZone(ZoneOffset.UTC)
       |  private val formatter      = DateTimeFormatter.ISO_ZONED_DATE_TIME
       |  private val shortFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
       |
       |  private val initiateResponse = UpscanInitiateResponse(
       |    reference = UpscanReference(fileReference),
       |    uploadRequest = UpscanFormTemplate(
       |      href = "http://localhost:9570/upscan/upload-proxy",
       |      fields = Map(
       |        "success_action_redirect" -> s"http://localhost:$$wireMockPort/${serviceName}/check-your-answers?key=$$fileReference",
       |        "x-amz-credential"                    -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
       |        "x-amz-meta-upscan-initiate-response" -> formatter.format(responseTime),
       |        "x-amz-meta-original-filename"        -> "$${filename}",
       |        "x-amz-algorithm"                     -> "AWS4-HMAC-SHA256",
       |        "x-amz-signature"                     -> "xxxx",
       |        "error_action_redirect"   -> "http://localhost:$$wireMockPort/${serviceName}/journey-recovery",
       |        "x-amz-meta-session-id"   -> sessionId,
       |        "x-amz-meta-callback-url" -> "http://localhost:$$wireMockPort/${serviceName}/",
       |        "x-amz-date"              -> shortFormatter.format(responseTime),
       |        "x-amz-meta-upscan-initiate-received" -> formatter.format(responseTime),
       |        "x-amz-meta-request-id"               -> requestId,
       |        "key"                                 -> fileReference,
       |        "acl"                                 -> "private",
       |        "x-amz-meta-consuming-service"        -> "${serviceName}",
       |        "policy" -> "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMCwxMDQ4NTc2MDBdXX0="
       |      )
       |    )
       |  )
       |
       |  override def beforeAll(): Unit = {
       |    super.beforeAll()
       |    // This is usually set on startup when the routes are instantiated
       |    app.RoutesPrefix.setPrefix("/${serviceName}")
       |  }
       |
       |  override def beforeEach(): Unit = {
       |    super.beforeEach()
       |
       |    // A valid request
       |    stubFor(
       |      post(urlPathEqualTo("/upscan/v2/initiate"))
       |        .atPriority(1)
       |        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
       |        .withHeader(HeaderNames.USER_AGENT, equalTo("${serviceName}"))
       |        .withRequestBody(equalToJson(s\"\"\"{
       |        |  "callbackUrl": "$$wireMockUrl/${serviceName}",
       |        |  "successRedirect": "$$wireMockUrl/${serviceName}/check-your-answers",
       |        |  "errorRedirect": "$$wireMockUrl/${serviceName}/there-is-a-problem"
       |        |}\"\"\".stripMargin))
       |        .willReturn(
       |          okJson(
       |            Json.stringify(
       |              Json.obj(
       |                "reference" -> fileReference,
       |                "uploadRequest" -> Json.obj(
       |                  "href" -> "http://localhost:9570/upscan/upload-proxy",
       |                  "fields" -> Json.obj(
       |                    "success_action_redirect" -> s"http://localhost:$$wireMockPort/${serviceName}/check-your-answers?key=$$fileReference",
       |                    "x-amz-credential" -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
       |                    "x-amz-meta-upscan-initiate-response" -> formatter.format(responseTime),
       |                    "x-amz-meta-original-filename"        -> "$${filename}",
       |                    "x-amz-algorithm"                     -> "AWS4-HMAC-SHA256",
       |                    "x-amz-signature"                     -> "xxxx",
       |                    "error_action_redirect" -> "http://localhost:$$wireMockPort/${serviceName}/journey-recovery",
       |                    "x-amz-meta-session-id"   -> sessionId,
       |                    "x-amz-meta-callback-url" -> "http://localhost:$$wireMockPort/${serviceName}/",
       |                    "x-amz-date"              -> shortFormatter.format(responseTime),
       |                    "x-amz-meta-upscan-initiate-received" -> formatter.format(responseTime),
       |                    "x-amz-meta-request-id"               -> requestId,
       |                    "key"                                 -> fileReference,
       |                    "acl"                                 -> "private",
       |                    "x-amz-meta-consuming-service"        -> "${serviceName}",
       |                    "policy" -> "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMCwxMDQ4NTc2MDBdXX0="
       |                  )
       |                )
       |              )
       |            )
       |          )
       |        )
       |    )
       |
       |    // A request where the body is missing
       |    stubFor(
       |      post(urlPathEqualTo("/upscan/v2/initiate"))
       |        .atPriority(2)
       |        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
       |        .withHeader(HeaderNames.USER_AGENT, equalTo("${serviceName}"))
       |        .willReturn(badRequest().withBody(\"\"\"{
       |                                            |  "statusCode": 415,
       |                                            |  "message": "Expecting text/json or application/json body"
       |                                            |}\"\"\".stripMargin))
       |    )
       |
       |    // A request where the user agent is missing
       |    stubFor(
       |      post(urlPathEqualTo("/upscan/v2/initiate"))
       |        .atPriority(3)
       |        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
       |        .willReturn(badRequest().withBody(s"Missing $${HeaderNames.USER_AGENT} Header"))
       |    )
       |  }
       |
       |  "UpscanConnector" should "return the upscan-initiate response when the request is successful" in {
       |    upscanConnector
       |      .initiate(
       |        callbackUrl = controllers.routes.IndexController.onPageLoad(),
       |        successRedirect = controllers.routes.CheckYourAnswersController.onPageLoad(),
       |        errorRedirect = controllers.routes.JourneyRecoveryController.onPageLoad()
       |      )
       |      .map {
       |        _ shouldBe initiateResponse
       |      }
       |  }
       |
       |  it should "throw UpstreamErrorResponse if upscan-initiate returns a client error" in {
       |    stubFor(
       |      post(urlPathEqualTo("/upscan/v2/initiate"))
       |        .atPriority(1)
       |        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
       |        .withHeader(HeaderNames.USER_AGENT, equalTo("${serviceName}"))
       |        .willReturn(
       |          aResponse()
       |            .withStatus(Status.UNSUPPORTED_MEDIA_TYPE)
       |            .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
       |            .withBody(\"\"\"{
       |            |  "statusCode": 415,
       |            |  "message": "Expecting text/json or application/json body"
       |            |}\"\"\".stripMargin)
       |        )
       |    )
       |
       |    recoverToExceptionIf[UpstreamErrorResponse] {
       |      upscanConnector
       |        .initiate(
       |          callbackUrl = controllers.routes.IndexController.onPageLoad(),
       |          successRedirect = controllers.routes.CheckYourAnswersController.onPageLoad(),
       |          errorRedirect = controllers.routes.JourneyRecoveryController.onPageLoad()
       |        )
       |    }.map { error =>
       |      error.statusCode shouldBe Status.UNSUPPORTED_MEDIA_TYPE
       |      error.reportAs shouldBe Status.INTERNAL_SERVER_ERROR
       |    }
       |  }
       |
       |  it should "throw UpstreamErrorResponse if upscan-initiate returns a server error" in {
       |    stubFor(
       |      post(urlPathEqualTo("/upscan/v2/initiate"))
       |        .atPriority(1)
       |        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
       |        .withHeader(HeaderNames.USER_AGENT, equalTo("${serviceName}"))
       |        .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
       |    )
       |
       |    recoverToExceptionIf[UpstreamErrorResponse] {
       |      upscanConnector
       |        .initiate(
       |          callbackUrl = controllers.routes.IndexController.onPageLoad(),
       |          successRedirect = controllers.routes.CheckYourAnswersController.onPageLoad(),
       |          errorRedirect = controllers.routes.JourneyRecoveryController.onPageLoad()
       |        )
       |    }.map { error =>
       |      error.statusCode shouldBe Status.INTERNAL_SERVER_ERROR
       |      error.reportAs shouldBe Status.BAD_GATEWAY
       |    }
       |  }
       |}
       |""".stripMargin
  }
}
