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

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import sbt.*
import sbt.Keys.*

import scala.jdk.CollectionConverters.*

object GenJourneyPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = noTrigger

  object autoImport {
    val parsedJourneyConfiguration = taskKey[Config](
      "The journey.conf configuration file content as a Lightbend Config instance."
    )

    val resolvedJourneyConfiguration = taskKey[Config](
      "The journey.conf configuration file content as a Lightbend Config instance with any config substitutions resolved."
    )

    val journeyConfiguration = taskKey[JourneyConfig](
      "The journey.conf configuration file content as a JourneyConfig case class."
    )

    val generateJourney = taskKey[Seq[File]](
      "Generate Play Framework controller interfaces from a journey.conf file."
    )

    val generateJourneyTests = taskKey[Seq[File]](
      "Generate Play Framework controller tests from a journey.conf file."
    )

    case class Page(
      titleKey: String,
      headingKey: String,
      template: Option[String],
      nextPage: Option[String]
    )

    case class Journey(
      name: String,
      startPage: String,
      pages: Map[String, Page]
    )

    case class JourneyConfig(
      indexPage: String,
      journeys: Map[String, Journey]
    )
  }

  import autoImport.*

  override def projectSettings: Seq[Setting[?]] = Def.settings(
    Compile / sourceGenerators += generateJourney.taskValue,
    Test / sourceGenerators += generateJourneyTests.taskValue,
    Zero / parsedJourneyConfiguration := {
      val configFile = resourceDirectory.value / "journey.conf"
      ConfigFactory.parseFile(configFile)
    },
    Zero / resolvedJourneyConfiguration := {
      parsedJourneyConfiguration.value.resolve()
    },
    Zero / journeyConfiguration := {
      val config = resolvedJourneyConfiguration.value
      deserialiseJourneyConfig(config)
    },
    Compile / generateJourney := {
      val baseDir = (Compile / sourceManaged).value
      generateJourneyFiles(baseDir, journeyConfiguration.value)
    },
    Test / generateJourneyTests := {
      val baseDir = (Compile / sourceManaged).value
      generateJourneyTestFiles(baseDir, journeyConfiguration.value)
    }
  )

  def deserialisePage(
    journey: String,
    key: String,
    value: ConfigValue
  ): (String, Page) = {
    value match {
      case obj: ConfigObject =>
        val config     = obj.toConfig()
        val titleKey   = config.getString("name")
        val headingKey = config.getString("startPage")
        val template   = Option(config.getString("template"))
        val nextPage   = Option(config.getString("nextPage"))
        key -> Page(titleKey, headingKey, template, nextPage)
      case other: ConfigValue =>
        val origin = value.origin()
        sys.error(
          s"${origin.description()} Expected a configuration object at journeys.${journey}.${key}"
        )
    }
  }

  def deserialiseJourney(key: String, value: ConfigValue): (String, Journey) = {
    value match {
      case obj: ConfigObject =>
        val config    = obj.toConfig()
        val name      = config.getString("name")
        val startPage = config.getString("startPage")

        val pages = config
          .getObject("pages")
          .asScala
          .toMap

        key -> Journey(
          name,
          startPage,
          pages.map((deserialisePage(key, _, _)).tupled)
        )

      case other: ConfigValue =>
        val origin = value.origin()
        sys.error(
          s"${origin.description()} Expected a configuration object at journeys.${key}"
        )
    }
  }

  def deserialiseJourneyConfig(config: Config): JourneyConfig = {
    val indexPage = config.getString("indexPage")

    val journeys = config
      .getObject("journeys")
      .asScala
      .toMap

    JourneyConfig(
      indexPage,
      journeys.map((deserialiseJourney _).tupled)
    )
  }

  def generateJourneyFiles(
    baseDirectory: File,
    config: JourneyConfig
  ): Seq[File] = {
    Seq.empty
  }

  def generateJourneyTestFiles(
    baseDirectory: File,
    config: JourneyConfig
  ): Seq[File] = {
    Seq.empty
  }
}
