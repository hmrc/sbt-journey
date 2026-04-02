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

import com.typesafe.config.{ConfigException, ConfigFactory, ConfigValueFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inside, OptionValues}
import sbt.util.Logger
import uk.gov.hmrc.sbt.journey.models.*

import java.util.Collections
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

class JourneyPluginSpec extends AnyFlatSpec with Matchers with Inside with OptionValues {
  private val exampleConf = ConfigFactory.parseResources("example.conf").resolve()
  private val config      = JourneyPlugin.deserialiseJourneyConfig(Logger.Null, exampleConf)
  private val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  "JourneyPlugin.deserialiseJourneyConfiguration" should "deserialise the base package" in {
    config.basePackage shouldBe "uk.gov.hmrc.simplejourney"
  }

  it should "deserialise the root pages" in {
    val rootPages = config.rootPages

    rootPages.keySet shouldBe Set("beforeYouBegin", "checkYourAnswers", "index")

    rootPages.get("beforeYouBegin").orNull shouldBe RootPage(
      titleKey = "beforeYouBegin.title",
      headingKey = "beforeYouBegin.heading",
      viewRoute = "/before-you-begin",
      controllerClass = "uk.gov.hmrc.simplejourney.controllers.BeforeYouBeginBaseController",
      viewClass = "views.html.BeforeYouBeginView",
      withDefaultController = true
    )

    rootPages.get("checkYourAnswers").orNull shouldBe RootPage(
      titleKey = "checkYourAnswers.title",
      headingKey = "checkYourAnswers.heading",
      viewRoute = "/check-your-answers",
      controllerClass = "uk.gov.hmrc.simplejourney.controllers.CheckYourAnswersBaseController",
      viewClass = "views.html.CheckYourAnswersView",
      withDefaultController = false
    )

    rootPages.get("index").orNull shouldBe RootPage(
      titleKey = "index.title",
      headingKey = "index.heading",
      viewRoute = "/",
      controllerClass = "uk.gov.hmrc.simplejourney.controllers.IndexBaseController",
      viewClass = "views.html.IndexView",
      withDefaultController = true
    )
  }

  it should "deserialise the models" in {
    val models = config.models

    models.keySet shouldBe Set("Choice", "TaxRegime", "AuditEvent")

    models.get("Choice").orNull shouldBe EnumModel("Choice", List("Yes", "No"))

    models.get("TaxRegime").orNull shouldBe EnumModel("TaxRegime", List("SA", "VAT"))

    models.get("AuditEvent").orNull shouldBe CaseClassModel(
      "AuditEvent",
      List(
        "auditType"                   -> FieldType.STRING,
        "description"                 -> FieldType.STRING,
        "expectedGoLiveDate"          -> FieldType.LOCALDATE,
        "expectedDecommissioningDate" -> OptionType(FieldType.LOCALDATE)
      )
    )
  }

  it should "deserialise the submission journey's pages" in {
    val journeys = config.journeys

    journeys.keySet shouldBe Set("submission")

    val submissionJourney = journeys.get("submission").orNull

    inside(submissionJourney) { case Journey(pages, _) =>
      val pageKeys = Set(
        "cipAssessmentTicket",
        "cipAssessmentPage",
        "serviceName",
        "serviceDescription",
        "dataDomain",
        "addAnotherDataDomain",
        "addATaxRegime",
        "taxRegime",
        "addAnotherTaxRegime",
        "auditProvider",
        "auditSource",
        "auditEvent",
        "addAnotherAuditEvent",
        "addAnotherAuditSource"
      )

      pages.keySet shouldBe pageKeys

      pages.get("cipAssessmentTicket").orNull shouldBe JourneyPage(
        "cipAssessmentTicket",
        "cipAssessmentTicket.title",
        "cipAssessmentTicket.heading",
        "/cip-assessment-ticket",
        "/change-cip-assessment-ticket",
        "uk.gov.hmrc.simplejourney.controllers.CipAssessmentTicketBaseController",
        "uk.gov.hmrc.simplejourney.forms.CipAssessmentTicketBaseFormProvider",
        "views.html.CipAssessmentTicketView",
        withDefaultController = true,
        withDefaultFormProvider = true,
        FieldType.STRING
      )

      pages.get("addAnotherDataDomain").orNull shouldBe JourneyPage(
        "addAnotherDataDomain",
        "addAnotherDataDomain.title",
        "addAnotherDataDomain.heading",
        "/add-another-data-domain",
        "/change-add-another-data-domain",
        "uk.gov.hmrc.simplejourney.controllers.AddAnotherDataDomainBaseController",
        "uk.gov.hmrc.simplejourney.forms.AddAnotherDataDomainBaseFormProvider",
        "views.html.AddAnotherDataDomainView",
        withDefaultController = true,
        withDefaultFormProvider = true,
        ClassType("uk.gov.hmrc.simplejourney.models.Choice")
      )

      pages.get("auditEvent").orNull shouldBe JourneyPage(
        "auditEvent",
        "auditEvent.title",
        "auditEvent.heading",
        "/audit-event",
        "/change-audit-event",
        "uk.gov.hmrc.simplejourney.controllers.AuditEventBaseController",
        "uk.gov.hmrc.simplejourney.forms.AuditEventBaseFormProvider",
        "views.html.AuditEventView",
        withDefaultController = true,
        withDefaultFormProvider = true,
        ClassType("uk.gov.hmrc.simplejourney.models.AuditEvent")
      )
    }
  }

  it should "deserialise the submission journey" in {
    val journey = config.journeys.get("submission").map(_.journey).orNull

    journey should have size 9

    journey.head shouldBe SinglePagePart("cipAssessmentTicket", None)
    journey(1) shouldBe SinglePagePart("cipAssessmentPage", None)
    journey(2) shouldBe SinglePagePart("serviceName", None)
    journey(3) shouldBe SinglePagePart("serviceDescription", None)

    journey(4) shouldBe DoWhilePart(
      "addAnotherDataDomain",
      List(SinglePagePart("dataDomain", None)),
      "dataDomains"
    )

    inside(journey(5)) { case IfThenPart(choicePage, subJourney, as) =>
      choicePage shouldBe "addATaxRegime"

      subJourney.headOption.value shouldBe DoWhilePart(
        "addAnotherTaxRegime",
        List(SinglePagePart("taxRegime", None)),
        "taxRegimes"
      )

      as shouldBe None
    }

    journey(6) shouldBe SinglePagePart("auditProvider", None)

    inside(journey(7)) { case outer: DoWhilePart =>
      outer.choicePage shouldBe "addAnotherAuditSource"
      outer.as shouldBe "auditSources"

      outer.subJourney should have size 2

      outer.subJourney.head shouldBe SinglePagePart("auditSource", None)

      inside(outer.subJourney(1)) { case inner: DoWhilePart =>
        inner.choicePage shouldBe "addAnotherAuditEvent"
        inner.as shouldBe "auditEvents"
        inner.subJourney.head shouldBe SinglePagePart("auditEvent", None)
      }
    }
  }

  "JourneyPlugin.deserialiseJourneyConfig" should "use basePackage if provided" in {
    val configObject  = ConfigFactory.parseMap(Map("basePackage" -> basePackage.toString).asJava)
    val journeyConfig = JourneyPlugin.deserialiseJourneyConfig(Logger.Null, configObject)
    journeyConfig.basePackage shouldBe basePackage.toString
  }

  it should "use serviceName if basePackage is not provided" in {
    val configObject  = ConfigFactory.parseMap(Map("serviceName" -> "test-microservice").asJava)
    val journeyConfig = JourneyPlugin.deserialiseJourneyConfig(Logger.Null, configObject)
    journeyConfig.basePackage shouldBe "uk.gov.hmrc.testmicroservice"
  }

  "JourneyPlugin.deserialiseRootPage" should "use titleKey if provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("titleKey" -> "title").asJava)
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.titleKey shouldBe "title"
  }

  it should "use the page key to derive a title key if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Collections.emptyMap())
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.titleKey shouldBe "beforeYouBegin.title"
  }

  it should "use headingKey if provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("headingKey" -> "heading").asJava)
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.headingKey shouldBe "heading"
  }

  it should "use the page key to derive a heading key if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Collections.emptyMap())
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.headingKey shouldBe "beforeYouBegin.heading"
  }

  it should "use viewRoute if provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("viewRoute" -> "/view-route").asJava)
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.viewRoute shouldBe "/view-route"
  }

  it should "use the page key to derive a view route if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Collections.emptyMap())
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.viewRoute shouldBe "/before-you-begin"
  }

  it should "use controllerClass if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(Map("controllerClass" -> "controller.Class").asJava)
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.controllerClass shouldBe "controller.Class"
  }

  it should "use the base package and page key to derive a controller class if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Collections.emptyMap())
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.controllerClass shouldBe "uk.gov.hmrc.sbtjourneytest.controllers.BeforeYouBeginBaseController"
  }

  it should "use withDefaultController if provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("withDefaultController" -> false).asJava)
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.withDefaultController shouldBe false
  }

  it should "use true for withDefaultController if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Collections.emptyMap())
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.withDefaultController shouldBe true
  }

  it should "use viewClass if provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("viewClass" -> "view.Class").asJava)
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.viewClass shouldBe "view.Class"
  }

  it should "use the page key to derive a view class if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Collections.emptyMap())
    val (_, rootPage) =
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    rootPage.viewClass shouldBe "views.html.BeforeYouBeginView"
  }

  it should "throw an exception if the wrong kind of configuration value is provided" in {
    val configObject = ConfigValueFactory.fromAnyRef(1)

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val thrown = the[ConfigException.WrongType] thrownBy {
      JourneyPlugin.deserialiseRootPage(basePackage.toString, "beforeYouBegin", configObject)
    }

    errors.result() shouldBe empty

    thrown should have message "hardcoded value: rootPage.beforeYouBegin has type NUMBER rather than OBJECT"
  }

  "JourneyPlugin.deserialiseJourneyPage" should "use titleKey if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(Map("titleKey" -> "title", "answerType" -> "String").asJava)
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.titleKey shouldBe "title"
  }

  it should "use the page key to derive a title key if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.titleKey shouldBe "auditSource.title"
  }

  it should "use headingKey if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(Map("headingKey" -> "heading", "answerType" -> "String").asJava)
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.headingKey shouldBe "heading"
  }

  it should "use the page key to derive a heading key if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.headingKey shouldBe "auditSource.heading"
  }

  it should "use viewRoute if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(Map("viewRoute" -> "/view-route", "answerType" -> "String").asJava)
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.viewRoute shouldBe "/view-route"
  }

  it should "use the page key to derive a view route if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.viewRoute shouldBe "/audit-source"
  }

  it should "use changeRoute if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(
        Map("changeRoute" -> "/change-route", "answerType" -> "String").asJava
      )
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.changeRoute shouldBe "/change-route"
  }

  it should "use the page key to derive a change route if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.changeRoute shouldBe "/change-audit-source"
  }

  it should "use controllerClass if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(
        Map("controllerClass" -> "controller.Class", "answerType" -> "String").asJava
      )
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.controllerClass shouldBe "controller.Class"
  }

  it should "use the base package and page key to derive a controller class if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.controllerClass shouldBe "uk.gov.hmrc.sbtjourneytest.controllers.AuditSourceBaseController"
  }

  it should "use formProviderClass if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(
        Map("formProviderClass" -> "form.provider.Class", "answerType" -> "String").asJava
      )
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.formProviderClass shouldBe "form.provider.Class"
  }

  it should "use the base package and page key to derive a form provider class if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.formProviderClass shouldBe "uk.gov.hmrc.sbtjourneytest.forms.AuditSourceBaseFormProvider"
  }

  it should "use viewClass if provided" in {
    val configObject =
      ConfigValueFactory.fromMap(Map("viewClass" -> "view.Class", "answerType" -> "String").asJava)
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.viewClass shouldBe "view.Class"
  }

  it should "use the page key to derive a view class if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.viewClass shouldBe "views.html.AuditSourceView"
  }

  it should "use withDefaultController if provided" in {
    val configObject = ConfigValueFactory.fromMap(
      Map("withDefaultController" -> false, "answerType" -> "String").asJava
    )
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.withDefaultController shouldBe false
  }

  it should "use true for withDefaultController if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.withDefaultController shouldBe true
  }

  it should "use withDefaultFormProvider if provided" in {
    val configObject = ConfigValueFactory.fromMap(
      Map("withDefaultFormProvider" -> false, "answerType" -> "String").asJava
    )
    val errors = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.withDefaultFormProvider shouldBe false
  }

  it should "use true for withDefaultFormProvider if not provided" in {
    val configObject = ConfigValueFactory.fromMap(Map("answerType" -> "String").asJava)
    val errors       = ListBuffer.empty[JourneyConfigProblem]
    val (_, journeyPage) =
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configObject
      )
    errors.result() shouldBe empty
    journeyPage.withDefaultFormProvider shouldBe true
  }

  it should "throw an exception if the wrong kind of configuration value is provided" in {
    val configValue = ConfigValueFactory.fromAnyRef(1)

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val thrown = the[ConfigException.WrongType] thrownBy {
      JourneyPlugin.deserialiseJourneyPage(
        basePackage.toString,
        Map.empty,
        basePackage / "models",
        errors,
        "submission",
        "auditSource",
        configValue
      )
    }

    errors.result() shouldBe empty

    thrown should have message "hardcoded value: journeys.submission.auditSource has type NUMBER rather than OBJECT"
  }

  "JourneyPlugin.deserialiseFieldType" should "deserialise Int" in {
    val configValue = ConfigValueFactory.fromAnyRef("Int")

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe FieldType.INT
  }

  it should "deserialise Boolean" in {
    val configValue = ConfigValueFactory.fromAnyRef("Boolean")

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe FieldType.BOOLEAN
  }

  it should "deserialise String" in {
    val configValue = ConfigValueFactory.fromAnyRef("String")

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe FieldType.STRING
  }

  it should "deserialise BigDecimal" in {
    val configValue = ConfigValueFactory.fromAnyRef("BigDecimal")

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe FieldType.BIGDECIMAL
  }

  it should "deserialise LocalDate" in {
    val configValue = ConfigValueFactory.fromAnyRef("LocalDate")

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe FieldType.LOCALDATE
  }

  it should "deserialise Optional field types" in {
    val configValue = ConfigValueFactory.fromMap(Map("Option" -> "LocalDate").asJava)

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe OptionType(FieldType.LOCALDATE)
  }

  it should "deserialise references to model types" in {
    val configValue = ConfigValueFactory.fromAnyRef("TaxRegime")

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val models = Map(
      "TaxRegime" -> EnumModel("TaxRegime", List("SA", "VAT"))
    )

    val fieldType =
      JourneyPlugin.deserialiseFieldType(models, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe ClassType("uk.gov.hmrc.sbtjourneytest.models.TaxRegime")
  }

  it should "assume that unrecognised types are fully qualified class names" in {
    val configValue = ConfigValueFactory.fromAnyRef("net.sourceforge.plantuml.SourceStringReader")

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    errors.result() shouldBe empty

    fieldType shouldBe ClassType("net.sourceforge.plantuml.SourceStringReader")
  }

  it should "return null and record an error when the wrong kind of configuration value is used" in {
    val configValue = ConfigValueFactory.fromAnyRef(1)

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    val errorList = errors.result()
    errorList should have size 1
    errorList.head.problem shouldBe "Expected either a configuration object describing a collection type or a string describing a known type"

    fieldType shouldBe null
  }

  it should "return null and record an error when an unrecognised collection type is used" in {
    val configValue = ConfigValueFactory.fromMap(Map("HashMap" -> "LocalDate").asJava)

    val errors = ListBuffer.empty[JourneyConfigProblem]

    val fieldType =
      JourneyPlugin.deserialiseFieldType(Map.empty, basePackage / "models", errors, configValue)

    val errorList = errors.result()
    errorList should have size 1
    errorList.head.problem shouldBe "Expected a configuration object describing a collection type"

    fieldType shouldBe null
  }
}
