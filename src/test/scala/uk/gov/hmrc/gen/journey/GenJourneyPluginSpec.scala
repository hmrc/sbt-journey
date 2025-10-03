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

package uk.gov.hmrc.gen.journey

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.typesafe.config.ConfigFactory
import GenJourneyPlugin.autoImport.*

class GenJourneyPluginSpec extends AnyFlatSpec with Matchers {

  "GenJourneyPlugin.deserialiseJourneyConfiguration" should "deserialise an example configuration file" in {
    val config = ConfigFactory.parseResources("example.conf").resolve()

    val actual = GenJourneyPlugin.deserialiseJourneyConfig(config)

    val expected = JourneyConfig(
      indexPage = "beforeYouStart",
      Map(
        "beforeYouStart" -> Journey(
          name = "Submission",
          startPage = "beforeYouStart",
          pages = Map(
            "beforeYouStart" -> Page(
              titleKey = "Before You Start",
              headingKey = "Before You Start",
              template = Some("uk.gov.hmrc.gen.journey.GenJourneyPlugin"),
              nextPage = Some("")
            )
          )
        )
      )
    )

    actual shouldBe expected
  }
}
