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

import com.typesafe.config.*
import com.typesafe.config.ConfigException.ValidationProblem
import play.sbt.routes.RoutesCompiler
import play.sbt.routes.RoutesKeys.*
import sbt.*
import sbt.Keys.*
import sbtcompat.PluginCompat.*
import uk.gov.hmrc.sbt.journey.models.*
import uk.gov.hmrc.sbt.journey.templates.*
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{kebabCase, packageCase, pascalCase}

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

object JourneyPlugin extends AutoPlugin {
  override def requires: Plugins = RoutesCompiler

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

    val generateJourney = taskKey[Seq[FileRef]](
      "Generate Play Framework controller interfaces from a journey.conf file."
    )

    val generateJourneyRoutes = taskKey[Seq[FileRef]](
      "Generate Play Framework routes from a journey.conf file."
    )

    val generateJourneyTests = taskKey[Seq[FileRef]](
      "Generate Play Framework controller tests from a journey.conf file."
    )

    val initialiseJourneyViews = taskKey[Unit](
      "Initialise view files for each of the journey pages if they don't already exist."
    )
  }

  import autoImport.*

  override def projectSettings: Seq[Setting[?]] =
    inConfig(Compile)(journeySettings) ++
      inConfig(Test)(journeyTestSettings) ++
      journeyConfigSettings

  def journeySettings: Seq[Setting[?]] = Def.settings(
    sourceGenerators += generateJourney.taskValue,
    routes / sources ++= generateJourneyRoutes.value,
    generateJourney := {
      val baseDir       = (generateJourney / target).value
      val journeyConfig = journeyConfiguration.value
      generateJourneyFiles(baseDir, journeyConfig)
    },
    generateJourney / target := crossTarget.value / "journey" / Defaults.nameForSrc(
      configuration.value.name
    ),
    managedSourceDirectories += (generateJourney / target).value,
    generateJourneyRoutes := {
      val baseDir       = resourceManaged.value
      val journeyConfig = journeyConfiguration.value
      generateJourneyRouteFiles(baseDir, journeyConfig)
    },
    initialiseJourneyViews := {
      val baseDir       = sourceDirectory.value
      val journeyConfig = journeyConfiguration.value
      initialiseJourneyViewFiles(baseDir, journeyConfig)
    }
  )

  def journeyTestSettings: Seq[Setting[?]] = Def.settings(
    sourceGenerators += generateJourneyTests.taskValue,
    generateJourneyTests / target := crossTarget.value / "journey" / Defaults.nameForSrc(
      configuration.value.name
    ),
    managedSourceDirectories += (generateJourneyTests / target).value,
    generateJourneyTests := {
      val baseDir       = (generateJourneyTests / target).value
      val journeyConfig = journeyConfiguration.value
      generateJourneyTestFiles(baseDir, journeyConfig)
    }
  )

  def journeyConfigSettings: Seq[Setting[?]] = Def.settings(
    parsedJourneyConfiguration := {
      val configFile = (Compile / resourceDirectory).value / "journey.conf"
      ConfigFactory.parseFile(configFile)
    },
    resolvedJourneyConfiguration := {
      parsedJourneyConfiguration.value.resolve()
    },
    journeyConfiguration := {
      val config = resolvedJourneyConfiguration.value
      deserialiseJourneyConfig(config)
    }
  )

  private def throwValidationFailed(path: String, origin: ConfigOrigin, msg: String): Nothing =
    throw new ConfigException.ValidationFailed(
      List(new ValidationProblem(path, origin, msg)).asJava
    )

  private def getConfig(configValue: ConfigValue, path: String): Config =
    getObject(configValue, path).toConfig

  private def getObject(configValue: ConfigValue, path: String): ConfigObject =
    configValue match {
      case obj: ConfigObject =>
        obj
      case _ =>
        throw new ConfigException.WrongType(
          configValue.origin(),
          path,
          ConfigValueType.OBJECT.name(),
          configValue.valueType().name()
        )
    }

  private[journey] def deserialiseRootPage(
    basePackage: String,
    key: String,
    value: ConfigValue
  ): (String, RootPage) = {
    val config = getConfig(value, s"rootPage.$key")
    val titleKey =
      if (config.hasPath("titleKey")) config.getString("titleKey") else s"$key.title"
    val headingKey =
      if (config.hasPath("headingKey")) config.getString("headingKey") else s"$key.heading"
    val viewRoute =
      if (config.hasPath("viewRoute")) config.getString("viewRoute") else s"/${kebabCase(key)}"
    val controllerClass =
      if (config.hasPath("controllerClass")) config.getString("controllerClass")
      else s"$basePackage.controllers.${pascalCase(key)}BaseController"
    val withDefaultController =
      if (config.hasPath("withDefaultController")) config.getBoolean("withDefaultController")
      else true
    val viewClass =
      if (config.hasPath("viewClass")) config.getString("viewClass")
      else s"views.html.${pascalCase(key)}View"
    key -> RootPage(
      titleKey,
      headingKey,
      viewRoute,
      controllerClass,
      viewClass,
      withDefaultController
    )
  }

  private[journey] def deserialiseJourneyPage(
    basePackage: String,
    models: Map[String, AnswerModel],
    modelsPackage: QualifiedName,
    journey: String,
    key: String,
    value: ConfigValue
  ): (String, JourneyPage) = {
    val config = getConfig(value, s"journeys.$journey.$key")
    val titleKey =
      if (config.hasPath("titleKey")) config.getString("titleKey") else s"$key.title"
    val headingKey =
      if (config.hasPath("headingKey")) config.getString("headingKey") else s"$key.heading"
    val viewRoute =
      if (config.hasPath("viewRoute")) config.getString("viewRoute") else s"/${kebabCase(key)}"
    val changeRoute =
      if (config.hasPath("changeRoute")) config.getString("changeRoute")
      else s"/change-${kebabCase(key)}"
    val controllerClass =
      if (config.hasPath("controllerClass")) config.getString("controllerClass")
      else s"$basePackage.controllers.${pascalCase(key)}BaseController"
    val formProviderClass =
      if (config.hasPath("formProviderClass")) config.getString("formProviderClass")
      else s"$basePackage.forms.${pascalCase(key)}BaseFormProvider"
    val viewClass =
      if (config.hasPath("viewClass")) config.getString("viewClass")
      else s"views.html.${pascalCase(key)}View"
    val withDefaultController =
      if (config.hasPath("withDefaultController")) config.getBoolean("withDefaultController")
      else true
    val withDefaultFormProvider =
      if (config.hasPath("withDefaultFormProvider")) config.getBoolean("withDefaultFormProvider")
      else true
    val answerType =
      deserialiseAnswerModel(models, modelsPackage, config.getValue("answerType"))
    key -> JourneyPage(
      key,
      titleKey,
      headingKey,
      viewRoute,
      changeRoute,
      controllerClass,
      formProviderClass,
      viewClass,
      withDefaultController,
      withDefaultFormProvider,
      answerType
    )
  }

  private val primitives = Set[Class[? <: AnyVal]](
    classOf[Byte],
    classOf[Short],
    classOf[Int],
    classOf[Long],
    classOf[Float],
    classOf[Double],
    classOf[Char],
    classOf[Boolean]
  ).map(clazz => clazz.getSimpleName -> PrimitiveType(clazz)).toMap

  private val builtIns = Set[Class[? <: AnyRef]](
    classOf[String],
    classOf[Exception],
    classOf[Throwable],
    classOf[java.time.DayOfWeek],
    classOf[java.time.Instant],
    classOf[java.time.LocalDate],
    classOf[java.time.LocalDateTime],
    classOf[java.time.LocalTime],
    classOf[java.time.Month],
    classOf[java.time.MonthDay],
    classOf[java.time.OffsetDateTime],
    classOf[java.time.OffsetTime],
    classOf[java.time.Period],
    classOf[java.time.Year],
    classOf[java.time.YearMonth],
    classOf[java.time.ZonedDateTime],
    classOf[java.time.ZoneId],
    classOf[java.time.ZoneOffset],
    classOf[scala.concurrent.duration.Duration],
    classOf[scala.concurrent.duration.FiniteDuration],
    classOf[java.util.concurrent.TimeUnit]
  ).map(clazz => clazz.getSimpleName -> ClassType(clazz.getName)).toMap

  private[journey] def deserialiseAnswerModel(
    models: Map[String, ?],
    modelsPackage: QualifiedName,
    config: ConfigValue
  ): FieldType = {
    config.valueType() match {
      case ConfigValueType.OBJECT =>
        val configObject = config.asInstanceOf[ConfigObject]
        val entries      = configObject.entrySet().asScala.toList
        val firstEntry   = entries.head
        val modelName    = firstEntry.getKey
        if (modelName == "List")
          ListType(deserialiseAnswerModel(models, modelsPackage, configObject.get(modelName)))
        else if (modelName == "Option")
          OptionType(deserialiseAnswerModel(models, modelsPackage, configObject.get(modelName)))
        else if (modelName == "Set")
          SetType(deserialiseAnswerModel(models, modelsPackage, configObject.get(modelName)))
        else if (modelName == "Array")
          ArrayType(deserialiseAnswerModel(models, modelsPackage, configObject.get(modelName)))
        else if (modelName == "Map") {
          val configList = configObject.toConfig.getList(modelName)
          val keyModel   = deserialiseAnswerModel(models, modelsPackage, configList.get(0))
          val valueModel = deserialiseAnswerModel(models, modelsPackage, configList.get(1))
          MapType(keyModel, valueModel)
        } else {
          val origin = firstEntry.getValue.origin()
          sys.error(
            s"${origin.description()} Expected a configuration object describing a collection type"
          )
        }
      case ConfigValueType.STRING =>
        val answerTypeString = config.unwrapped().asInstanceOf[String]
        val lowerTypeString  = answerTypeString.toLowerCase
        if (primitives.contains(lowerTypeString)) primitives(lowerTypeString)
        else if (builtIns.contains(answerTypeString)) builtIns(answerTypeString)
        else if (models.contains(answerTypeString)) ClassType(modelsPackage / answerTypeString)
        else ClassType(answerTypeString)
      case _ =>
        val origin = config.origin()
        sys.error(
          s"${origin.description()} Expected either a configuration object describing a custom model or a string describing a known type"
        )
    }
  }

  private[journey] def deserialiseRootAnswerModel(
    modelsPackage: QualifiedName,
    models: Map[String, ConfigValue],
    key: String,
    value: ConfigValue
  ) = {
    value match {
      case list: ConfigList if list.asScala.forall(_.valueType() == ConfigValueType.STRING) =>
        val entries = list.asScala.toList.map(_.unwrapped().asInstanceOf[String])

        if (entries.contains("default")) {
          val index = entries.indexOf("default")
          throwValidationFailed(
            s"models.$key.$index",
            list.get(index).origin(),
            "'default' cannot be used as an enum value as it is reserved for catch-all subjourneys"
          )
        }

        key -> EnumModel(key, entries)

      case list: ConfigList =>
        key -> CaseClassModel(
          key,
          list.asScala.toList.map {
            case fieldConfig: ConfigObject =>
              val entries    = fieldConfig.entrySet().asScala.toList
              val firstEntry = entries.head
              val fieldName  = firstEntry.getKey
              fieldName -> deserialiseAnswerModel(models, modelsPackage, firstEntry.getValue)
            case other =>
              val origin = other.origin()
              sys.error(
                s"${origin.description()} Expected a configuration object describing a model field"
              )
          }
        )
      case _ =>
        val origin = value.origin()
        sys.error(
          s"${origin.description()} Expected a configuration list describing the fields of a model or the cases of an enumeration at models.$key"
        )
    }
  }

  private[journey] def deserialiseJourneyParts(
    rootPages: Map[String, RootPage],
    models: Map[String, AnswerModel],
    pages: Map[String, JourneyPage],
    choicePages: mutable.Builder[String, Set[String]],
    value: ConfigValue
  ): List[JourneyPart] = {
    if (value.valueType() == ConfigValueType.LIST) {
      value
        .asInstanceOf[ConfigList]
        .asScala
        .toList
        .map(deserialiseJourneyPart(rootPages, models, pages, choicePages, _))
    } else {
      List(deserialiseJourneyPart(rootPages, models, pages, choicePages, value))
    }
  }

  private[journey] def deserialiseJourneyPart(
    rootPages: Map[String, RootPage],
    models: Map[String, AnswerModel],
    pages: Map[String, JourneyPage],
    choicePages: mutable.Builder[String, Set[String]],
    value: ConfigValue
  ): JourneyPart = {
    value match {
      case obj: ConfigObject if obj.containsKey("do") || obj.containsKey("while") =>
        val config     = obj.toConfig
        val choicePage = config.getString("while")

        if (!pages.contains(choicePage)) {
          throwValidationFailed(
            "while",
            obj.origin(),
            s"$choicePage is not one of the journey pages"
          )
        }

        val answerType = pages(choicePage).answerType

        if (answerType != FieldType.BOOLEAN) {
          throwValidationFailed(
            "while",
            obj.origin(),
            "Expected a choice page with a boolean answerType"
          )
        }

        choicePages += choicePage

        DoWhilePart(
          choicePage,
          deserialiseJourneyParts(rootPages, models, pages, choicePages, config.getValue("do")),
          config.getString("as")
        )

      case obj: ConfigObject if obj.containsKey("if") || obj.containsKey("then") =>
        val config     = obj.toConfig
        val choicePage = config.getString("if")

        if (!pages.contains(choicePage)) {
          throwValidationFailed(
            "if",
            obj.origin(),
            s"$choicePage is not one of the journey pages"
          )
        }

        val answerType = pages(choicePage).answerType

        if (answerType != FieldType.BOOLEAN) {
          throwValidationFailed(
            "if",
            obj.origin(),
            "Expected a choice page with a boolean answerType"
          )
        }

        choicePages += choicePage

        IfThenPart(
          choicePage,
          deserialiseJourneyParts(rootPages, models, pages, choicePages, config.getValue("then")),
          if (config.hasPath("as")) Some(config.getString("as")) else None
        )

      case obj: ConfigObject if obj.containsKey("switch") || obj.containsKey("case") =>
        val config     = obj.toConfig
        val choicePage = config.getString("switch")

        if (!pages.contains(choicePage)) {
          throwValidationFailed(
            "switch",
            obj.origin(),
            s"$choicePage is not one of the journey pages"
          )
        }

        val answerType  = pages(choicePage).answerType.typeName
        val answerModel = answerType.flatMap(models.get)
        answerModel match {
          case Some(EnumModel(enumName, cases)) =>
            val caseObject = config.getObject("case")

            val subJourneys = caseObject.asScala.toMap.map { case (enumValue, journey) =>
              if (!cases.contains(enumValue)) {
                throwValidationFailed(
                  enumValue,
                  caseObject.origin(),
                  s"The value $enumValue is not one of the cases of enum $enumName"
                )
              }
              enumValue -> deserialiseJourneyParts(rootPages, models, pages, choicePages, journey)
            }

            SwitchCasePart(
              choicePage,
              subJourneys,
              if (config.hasPath("as")) Some(config.getString("as")) else None
            )

          case _ =>
            throwValidationFailed(
              "switch",
              obj.origin(),
              "Expected a choice page with an enum answerType"
            )
        }

      case obj: ConfigObject if obj.containsKey("page") || obj.containsKey("as") =>
        val config  = obj.toConfig
        val pageKey = config.getString("page")
        if (pages.contains(pageKey) || rootPages.contains(pageKey))
          SinglePagePart(
            pageKey,
            if (config.hasPath("as")) Some(config.getString("as")) else None
          )
        else
          throwValidationFailed(
            pageKey,
            value.origin(),
            s"$pageKey is not one of the root pages or journey pages"
          )

      case value: ConfigValue if value.valueType() == ConfigValueType.STRING =>
        val pageKey = value.unwrapped().asInstanceOf[String]
        if (pages.contains(pageKey) || rootPages.contains(pageKey))
          SinglePagePart(pageKey, None)
        else
          throwValidationFailed(
            pageKey,
            value.origin(),
            s"$pageKey is not one of the root pages or journey pages"
          )

      case _ =>
        val origin = value.origin()
        sys.error(s"${origin.description()} Unrecognised journey entry")
    }
  }

  private[journey] def deserialiseJourney(
    basePackage: String,
    rootPages: Map[String, RootPage],
    models: Map[String, AnswerModel],
    modelsPackage: QualifiedName,
    key: String,
    value: ConfigValue
  ): (String, Journey) = {
    val config = getConfig(value, s"journeys.$key")

    val pages = config
      .getObject("pages")
      .asScala
      .toMap

    val parts = config
      .getList("journey")
      .asScala
      .toList

    val journeyPages =
      pages.map((deserialiseJourneyPage(basePackage, models, modelsPackage, key, _, _)).tupled)

    val choicePages =
      Set.newBuilder[String]

    val journeyParts =
      parts.map(deserialiseJourneyPart(rootPages, models, journeyPages, choicePages, _))

    val updatedJourneyPages = choicePages
      .result()
      .map(page =>
        page -> journeyPages(page).copy(answerType = ClassType(modelsPackage / "Choice"))
      )
      .toMap

    journeyParts.lastOption.foreach {
      case SinglePagePart(pageKey, _) if rootPages.contains(pageKey) =>
      // This is valid
      case _ =>
        throwValidationFailed(
          s"journeys.$key.journey",
          config.getList("journey").get(parts.length - 1).origin(),
          "Expected the last part of the journey to be one of the root pages"
        )
    }

    key -> Journey(journeyPages ++ updatedJourneyPages, journeyParts)
  }

  private[journey] def deserialiseJourneyConfig(config: Config): JourneyConfig = {
    val indexPage = config.getString("indexPage")

    val basePackage =
      if (config.hasPath("basePackage")) config.getString("basePackage")
      else {
        val serviceName = config.getString("serviceName")
        s"uk.gov.hmrc.${packageCase(serviceName)}"
      }

    val roots =
      if (config.hasPath("rootPages"))
        config
          .getObject("rootPages")
          .asScala
          .toMap
      else
        Map.empty[String, ConfigValue]

    val models =
      if (config.hasPath("models"))
        config
          .getObject("models")
          .asScala
          .toMap
      else
        Map.empty[String, ConfigValue]

    val journeys =
      if (config.hasPath("journeys"))
        config
          .getObject("journeys")
          .asScala
          .toMap
      else
        Map.empty[String, ConfigValue]

    val rootPages = roots.map((deserialiseRootPage(basePackage, _, _)).tupled)

    if (!rootPages.contains(indexPage)) {
      throwValidationFailed(
        "indexPage",
        config.getValue("indexPage").origin(),
        s"$indexPage is not one of the root pages"
      )
    }

    val modelsPackage = QualifiedName(basePackage) / "models"

    // Add a "Choice" model for Yes / No questions
    val choiceModel  = EnumModel("Choice", List("Yes", "No"))
    val answerModels = models.map((deserialiseRootAnswerModel(modelsPackage, models, _, _)).tupled)

    JourneyConfig(
      basePackage,
      indexPage,
      rootPages,
      answerModels + ("Choice" -> choiceModel),
      journeys.map(
        (deserialiseJourney(basePackage, rootPages, answerModels, modelsPackage, _, _)).tupled
      )
    )
  }

  private[journey] def generateJourneyFiles(
    baseDirectory: FileRef,
    config: JourneyConfig
  ): Seq[FileRef] = {
    val packageFolder = config.basePackage
      .split("\\.")
      .foldLeft(baseDirectory)(_ / _)

    val basePackage   = QualifiedName(config.basePackage)
    val modelsPackage = basePackage / "models"

    val rootPageFiles = config.rootPages.map { case (pageName, page) =>
      val rootPageController =
        packageFolder / "controllers" / s"${pascalCase(pageName)}Controller.scala"
      IO.write(rootPageController, RootPageController.render(basePackage, pageName, page))
      rootPageController
    }.toList

    val journeyFiles = config.journeys.flatMap { case (_, journey) =>
      def syntheticJourneyModels(journeyPart: JourneyPart): Seq[File] = journeyPart match {
        case SwitchCasePart(choicePage, subJourney, as) =>
          val subJourneyModels = subJourney.values.toList.flatMap(_.flatMap(syntheticJourneyModels))
          val modelName        = pascalCase(as.getOrElse(choicePage))
          val modelFile        = packageFolder / "models" / s"$modelName.scala"
          val cases = subJourney.mapValues(ModelFields.forParts(modelsPackage, journey, _))
          IO.write(modelFile, JourneyModel.forSwitchCase(modelsPackage, modelName, cases))
          modelFile +: subJourneyModels
        case IfThenPart(choicePage, subJourney, as) =>
          val subJourneyModels = subJourney.flatMap(syntheticJourneyModels)
          val modelFields      = ModelFields.forParts(modelsPackage, journey, subJourney)
          val modelName        = pascalCase(as.getOrElse(choicePage))
          val modelFile        = packageFolder / "models" / s"$modelName.scala"
          IO.write(modelFile, JourneyModel.forIfThen(modelsPackage, modelName, modelFields))
          modelFile +: subJourneyModels
        case DoWhilePart(_, subJourney, as) =>
          val subJourneyModels = subJourney.flatMap(syntheticJourneyModels)
          val modelFields      = ModelFields.forParts(modelsPackage, journey, subJourney)
          if (modelFields.length == 1) {
            // We don't need to generate a model for this subjourney because it doesn't have multiple answers
            subJourneyModels
          } else {
            val modelName = pascalCase(as)
            val modelFile = packageFolder / "models" / s"$modelName.scala"
            IO.write(modelFile, JourneyModel.forDoWhile(modelsPackage, modelName, modelFields))
            modelFile +: subJourneyModels
          }
        case SinglePagePart(_, _) =>
          Seq.empty
      }

      def journeyModelFiles(journeyParts: List[JourneyPart]): Seq[File] =
        journeyParts.flatMap(syntheticJourneyModels)

      val journeyPageObjectFiles = journey.pages.map { case (pageName, page) =>
        val journeyPageObjectFile =
          packageFolder / "pages" / s"${pascalCase(pageName)}Page.scala"
        IO.write(journeyPageObjectFile, PageObject.render(basePackage, journey, page))
        journeyPageObjectFile
      }

      val journeyFormProviderFiles = journey.pages.map { case (pageName, page) =>
        val journeyFormProviderFile =
          packageFolder / "forms" / s"${pascalCase(pageName)}FormProvider.scala"
        IO.write(journeyFormProviderFile, FormProvider.render(basePackage, config.models, page))
        journeyFormProviderFile
      }

      val journeyControllerFiles = journey.pages.map { case (pageName, page) =>
        val journeyPageController =
          packageFolder / "controllers" / s"${pascalCase(pageName)}Controller.scala"
        IO.write(
          journeyPageController,
          JourneyPageController.render(
            basePackage,
            requiresData = pageName != journey.startPage,
            pageName,
            page,
            journey
          )
        )
        journeyPageController
      }

      journeyControllerFiles ++ journeyPageObjectFiles ++ journeyFormProviderFiles ++ journeyModelFiles(
        journey.journey
      )
    }.toList

    val modelFiles = config.models.map { case (modelName, model) =>
      val modelFile = packageFolder / "models" / s"$modelName.scala"
      IO.write(modelFile, CustomModel.render(basePackage, model))
      modelFile
    }.toList

    val navigatorFile = packageFolder / "navigation" / s"JourneyNavigator.scala"
    IO.write(navigatorFile, Navigator.render(config))

    rootPageFiles ++ modelFiles ++ journeyFiles :+ navigatorFile
  }

  private[journey] def generateJourneyRouteFiles(
    baseDirectory: FileRef,
    config: JourneyConfig
  ): Seq[FileRef] = {
    val journeyRoutes = baseDirectory / "journey.routes"
    IO.write(journeyRoutes, Routes.render(config))
    Seq(journeyRoutes)
  }

  private[journey] def generateJourneyTestFiles(
    baseDirectory: FileRef,
    config: JourneyConfig
  ): Seq[FileRef] = {
    Seq.empty
  }

  private[journey] def initialiseJourneyViewFiles(
    baseDirectory: File,
    config: JourneyConfig
  ): Unit = {
    val packageFolder = baseDirectory
    // TODO: Switch to this once we have a better template
    // val packageFolder = config.basePackage
    //   .split("\\.")
    //   .foldLeft(baseDirectory)(_ / _)

    val viewsFolder = packageFolder / "views"

    config.rootPages.foreach { case (pageName, _) =>
      val viewFile = viewsFolder / s"${pascalCase(pageName)}View.scala.html"
      if (!viewFile.exists()) {
        IO.write(viewFile, ViewStub.renderNoForm(pageName))
      }
    }

    config.journeys.foreach { case (_, journey) =>
      journey.pages.foreach { case (pageName, _) =>
        val viewFile = viewsFolder / s"${pascalCase(pageName)}View.scala.html"
        if (!viewFile.exists()) {
          IO.write(viewFile, ViewStub.renderForm(pageName))
        }
      }
    }
  }
}
