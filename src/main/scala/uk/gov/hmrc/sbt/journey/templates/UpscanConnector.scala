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
}
