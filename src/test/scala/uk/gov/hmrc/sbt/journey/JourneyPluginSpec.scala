/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.sbt.journey

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.sbt.journey.models.*

class JourneyPluginSpec extends AnyFlatSpec with Matchers {

  "JourneyPlugin.deserialiseJourneyConfiguration" should "deserialise an example configuration file" in {
    val config = ConfigFactory.parseResources("example.conf").resolve()

    val actual = JourneyPlugin.deserialiseJourneyConfig(config)

    val expected = JourneyConfig(
      basePackage = "uk.gov.hmrc.pra",
      rootPages = Map(
        "index" -> RootPage(
          titleKey = "index.title",
          headingKey = "index.heading",
          viewRoute = "/index",
          controllerClass = "uk.gov.hmrc.pra.DefaultIndexController",
          withDefaultController = true,
          viewClass = "views.html.IndexView"
        )
      ),
      models = Map.empty,
      journeys = Map(
        "submission" -> Journey(
          pages = Map(
            "beforeYouStart" -> JourneyPage(
              pageKey = "beforeYouStart",
              titleKey = "beforeYouStart.title",
              headingKey = "beforeYouStart.heading",
              viewRoute = "/before-you-start",
              changeRoute = "/change-before-you-start",
              controllerClass = "uk.gov.hmrc.pra.DefaultBeforeYouStartController",
              formProviderClass = "uk.gov.hmrc.pra.forms.DefaultBeforeYouStartFormProvider",
              viewClass = "views.html.SubmissionStartView",
              withDefaultController = true,
              withDefaultFormProvider = true,
              answerType = PrimitiveType(classOf[Int])
            ),
            "serviceUrl" -> JourneyPage(
              pageKey = "serviceUrl",
              titleKey = "serviceUrl.title",
              headingKey = "serviceUrl.heading",
              viewRoute = "/service-url",
              changeRoute = "/edit-service-url",
              controllerClass = "uk.gov.hmrc.pra.ServiceUrlController",
              formProviderClass = "uk.gov.hmrc.pra.forms.DefaultServiceUrlFormProvider",
              viewClass = "views.html.ServiceUrlView",
              withDefaultController = true,
              withDefaultFormProvider = true,
              answerType = PrimitiveType(classOf[Int])
            )
          ),
          journey = List.empty
        )
      )
    )

    actual shouldBe expected
  }
}
