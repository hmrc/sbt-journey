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
import play.sbt.routes.RoutesCompiler
import play.sbt.routes.RoutesKeys.*
import sbt.*
import sbt.Keys.*
import sbt.internal.util.complete.Parser
import sbt.nio.Keys.fileInputs
import sbt.util.CacheStoreFactory
import sbtcompat.PluginCompat.*
import uk.gov.hmrc.sbt.journey.models.*
import uk.gov.hmrc.sbt.journey.templates.*
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{kebabCase, packageCase, pascalCase}

import java.io.{ByteArrayOutputStream, OutputStream}
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

    val generateJourney = taskKey[Seq[File]](
      "Generate journey code from a journey.conf file."
    )

    val generateJourneyRoutes = taskKey[Seq[File]](
      "Generate journey routes from a journey.conf file."
    )

    val generateJourneyDiagrams = taskKey[Seq[File]](
      "Generate PlantUML diagrams from a journey.conf file."
    )

    val generateJourneyTests = taskKey[Seq[File]](
      "Generate journey tests from a journey.conf file."
    )

    val initialiseJourneyViews = taskKey[Unit](
      "Initialise view files for each of the journey pages if they don't already exist."
    )

    val overwriteJourneyViews = inputKey[Unit](
      "Overwrite the view files for each of the journey pages."
    )

    val initialiseJourneyForms = taskKey[Unit](
      "Initialise form providers for each of the journey pages if they don't already exist."
    )

    val overwriteJourneyForms = inputKey[Unit](
      "Overwrite the form providers for each of the journey pages."
    )
  }

  import autoImport.*

  override def projectSettings: Seq[Setting[?]] =
    inConfig(Compile)(journeySettings) ++
      inConfig(Test)(journeyTestSettings) ++
      journeyConfigSettings

  private val userConfirmation: Parser[Boolean] = {
    import complete.DefaultParsers.*
    (Space ~ chars("YN")).map { case (_, c) => c == 'Y' }
  }

  def journeySettings: Seq[Setting[?]] = Def.settings(
    sourceGenerators += generateJourney.taskValue,
    routes / sources ++= generateJourneyRoutes.value,
    generateJourney := {
      val logger            = streams.value.log
      val factory           = streams.value.cacheStoreFactory
      val baseDir           = (generateJourney / target).value
      val journeyConfigFile = (Compile / resourceDirectory).value / "journey.conf"
      val journeyConfig     = journeyConfiguration.value
      whenConfigChanges(factory, journeyConfigFile) { lastFiles =>
        cleanJourneyFiles(lastFiles)
        generateJourneyFiles(logger, baseDir, journeyConfig)
      }
    },
    generateJourney / fileInputs += ((Compile / resourceDirectory).value / "journey.conf").toGlob,
    generateJourney / target := {
      crossTarget.value / "journey" / Defaults.nameForSrc(configuration.value.name)
    },
    managedSourceDirectories += (generateJourney / target).value,
    generateJourneyRoutes := {
      val logger            = streams.value.log
      val factory           = streams.value.cacheStoreFactory
      val baseDir           = resourceManaged.value
      val journeyConfigFile = (Compile / resourceDirectory).value / "journey.conf"
      val journeyConfig     = journeyConfiguration.value
      whenConfigChanges(factory, journeyConfigFile) { lastFiles =>
        cleanJourneyRouteFiles(lastFiles)
        generateJourneyRouteFiles(logger, baseDir, journeyConfig)
      }
    },
    generateJourneyRoutes / fileInputs += ((Compile / resourceDirectory).value / "journey.conf").toGlob,
    generateJourneyDiagrams := {
      val logger        = streams.value.log
      val baseDir       = baseDirectory.value
      val journeyConfig = journeyConfiguration.value
      generateJourneyDiagramFiles(logger, baseDir, journeyConfig)
    },
    generateJourneyDiagrams / fileInputs += ((Compile / resourceDirectory).value / "journey.conf").toGlob,
    initialiseJourneyViews := {
      val logger        = streams.value.log
      val baseDir       = sourceDirectory.value
      val journeyConfig = journeyConfiguration.value
      initialiseJourneyViewFiles(logger, baseDir, journeyConfig)
    },
    overwriteJourneyViews := {
      if (userConfirmation.parsed) {
        val logger        = streams.value.log
        val baseDir       = sourceDirectory.value
        val journeyConfig = journeyConfiguration.value
        initialiseJourneyViewFiles(logger, baseDir, journeyConfig, overwrite = true)
      }
    },
    initialiseJourneyForms := {
      val logger        = streams.value.log
      val baseDir       = sourceDirectory.value
      val journeyConfig = journeyConfiguration.value
      initialiseJourneyFormFiles(logger, baseDir, journeyConfig)
    },
    overwriteJourneyForms := {
      if (userConfirmation.parsed) {
        val logger        = streams.value.log
        val baseDir       = sourceDirectory.value
        val journeyConfig = journeyConfiguration.value
        initialiseJourneyFormFiles(logger, baseDir, journeyConfig, overwrite = true)
      }
    }
  )

  def journeyTestSettings: Seq[Setting[?]] = Def.settings(
    sourceGenerators += generateJourneyTests.taskValue,
    generateJourneyTests := {
      val logger        = streams.value.log
      val baseDir       = (generateJourneyTests / target).value
      val journeyConfig = journeyConfiguration.value
      generateJourneyTestFiles(logger, baseDir, journeyConfig)
    },
    generateJourneyTests / fileInputs += ((Compile / resourceDirectory).value / "journey.conf").toGlob,
    generateJourneyTests / target := {
      crossTarget.value / "journey" / Defaults.nameForSrc(configuration.value.name)
    },
    managedSourceDirectories += (generateJourneyTests / target).value
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
      val logger = streams.value.log
      val config = resolvedJourneyConfiguration.value
      deserialiseJourneyConfig(logger, config)
    }
  )

  private def problem(origin: ConfigOrigin, msg: String): JourneyConfigProblem =
    JourneyConfigProblem(origin, msg)

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
    errors: mutable.ListBuffer[JourneyConfigProblem],
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
      deserialiseFieldType(models, modelsPackage, errors, config.getValue("answerType"))
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
    classOf[Int],
    classOf[Boolean]
  ).map(clazz => clazz.getSimpleName -> PrimitiveType(clazz)).toMap

  private val builtIns = Set[Class[? <: AnyRef]](
    classOf[String],
    classOf[java.time.LocalDate],
    classOf[scala.math.BigDecimal]
  ).map(clazz => clazz.getSimpleName -> ClassType(clazz.getName)).toMap

  private[journey] def deserialiseFieldType(
    models: Map[String, ?],
    modelsPackage: QualifiedName,
    errors: mutable.ListBuffer[JourneyConfigProblem],
    config: ConfigValue
  ): FieldType = {
    config.valueType() match {
      case ConfigValueType.OBJECT =>
        val obj        = config.asInstanceOf[ConfigObject]
        val entries    = obj.entrySet().asScala.toList
        val firstEntry = entries.head
        val modelName  = firstEntry.getKey
        if (modelName == "Option") {
          OptionType(deserialiseFieldType(models, modelsPackage, errors, obj.get(modelName)))
        } else {
          errors += problem(
            firstEntry.getValue.origin(),
            "Expected a configuration object describing a collection type"
          )
          null
        }
      case ConfigValueType.STRING =>
        val answerTypeString = config.unwrapped().asInstanceOf[String]
        val lowerTypeString  = answerTypeString.toLowerCase
        if (primitives.contains(lowerTypeString)) primitives(lowerTypeString)
        else if (builtIns.contains(answerTypeString)) builtIns(answerTypeString)
        else if (models.contains(answerTypeString)) ClassType(modelsPackage / answerTypeString)
        else ClassType(answerTypeString)
      case _ =>
        errors += problem(
          config.origin(),
          "Expected either a configuration object describing a collection type or a string describing a known type"
        )
        null
    }
  }

  private[journey] def deserialiseAnswerModel(
    modelsPackage: QualifiedName,
    models: Map[String, ConfigValue],
    errors: mutable.ListBuffer[JourneyConfigProblem],
    key: String,
    value: ConfigValue
  ) = {
    value match {
      case list: ConfigList if list.asScala.forall(_.valueType() == ConfigValueType.STRING) =>
        val entries = list.asScala.toList.map(_.unwrapped().asInstanceOf[String])

        if (entries.contains("default")) {
          val index = entries.indexOf("default")
          errors += problem(
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
              fieldName -> deserialiseFieldType(
                models,
                modelsPackage,
                errors,
                firstEntry.getValue
              )
            case other =>
              errors += problem(
                other.origin(),
                "Expected a configuration object describing a model field"
              )
              null
          }
        )
      case _ =>
        errors += problem(
          value.origin(),
          "Expected a configuration list describing the fields of a model or the cases of an enumeration"
        )
        null
    }
  }

  private[journey] def deserialiseJourneyParts(
    rootPages: Map[String, RootPage],
    models: Map[String, AnswerModel],
    pages: Map[String, JourneyPage],
    errors: mutable.ListBuffer[JourneyConfigProblem],
    choicePages: mutable.Builder[String, Set[String]],
    value: ConfigValue
  ): List[JourneyPart] = {
    if (value.valueType() == ConfigValueType.LIST) {
      value
        .asInstanceOf[ConfigList]
        .asScala
        .toList
        .map(deserialiseJourneyPart(rootPages, models, pages, errors, choicePages, _))
    } else {
      List(deserialiseJourneyPart(rootPages, models, pages, errors, choicePages, value))
    }
  }

  private[journey] def deserialiseJourneyPart(
    rootPages: Map[String, RootPage],
    models: Map[String, AnswerModel],
    pages: Map[String, JourneyPage],
    errors: mutable.ListBuffer[JourneyConfigProblem],
    choicePages: mutable.Builder[String, Set[String]],
    value: ConfigValue
  ): JourneyPart = {
    value match {
      case obj: ConfigObject if obj.containsKey("do") || obj.containsKey("while") =>
        val config     = obj.toConfig
        val choicePage = config.getString("while")

        if (!pages.contains(choicePage)) {
          errors += problem(obj.origin(), s"$choicePage is not one of the journey pages")
        }

        val answerType = Option(pages(choicePage).answerType)

        if (!answerType.contains(FieldType.BOOLEAN)) {
          errors += problem(obj.origin(), "Expected a choice page with a Boolean answerType")
        }

        choicePages += choicePage

        DoWhilePart(
          choicePage,
          deserialiseJourneyParts(
            rootPages,
            models,
            pages,
            errors,
            choicePages,
            config.getValue("do")
          ),
          config.getString("as")
        )

      case obj: ConfigObject if obj.containsKey("if") || obj.containsKey("then") =>
        val config     = obj.toConfig
        val choicePage = config.getString("if")

        if (!pages.contains(choicePage)) {
          errors += problem(obj.origin(), s"$choicePage is not one of the journey pages")
        }

        val answerType = Option(pages(choicePage).answerType)

        if (!answerType.contains(FieldType.BOOLEAN)) {
          errors += problem(obj.origin(), "Expected a choice page with a Boolean answerType")
        }

        choicePages += choicePage

        IfThenPart(
          choicePage,
          deserialiseJourneyParts(
            rootPages,
            models,
            pages,
            errors,
            choicePages,
            config.getValue("then")
          ),
          if (config.hasPath("as")) Some(config.getString("as")) else None
        )

      case obj: ConfigObject if obj.containsKey("switch") || obj.containsKey("case") =>
        val config     = obj.toConfig
        val choicePage = config.getString("switch")

        if (!pages.contains(choicePage)) {
          errors += problem(obj.origin(), s"$choicePage is not one of the journey pages")
        }

        val answerType  = Option(pages(choicePage).answerType)
        val answerModel = answerType.flatMap(_.typeName).flatMap(models.get)
        answerModel match {
          case Some(EnumModel(enumName, cases)) =>
            val caseObject = config.getObject("case")

            val subJourneys = caseObject.asScala.toMap.map { case (enumValue, journey) =>
              if (!cases.contains(enumValue)) {
                errors += problem(
                  caseObject.origin(),
                  s"The value $enumValue is not one of the cases of enum $enumName"
                )
              }
              enumValue -> deserialiseJourneyParts(
                rootPages,
                models,
                pages,
                errors,
                choicePages,
                journey
              )
            }

            SwitchCasePart(
              choicePage,
              subJourneys,
              if (config.hasPath("as")) Some(config.getString("as")) else None
            )

          case _ =>
            errors += problem(obj.origin(), "Expected a choice page with an enum answerType")
            null
        }

      case obj: ConfigObject if obj.containsKey("page") || obj.containsKey("as") =>
        val config  = obj.toConfig
        val pageKey = config.getString("page")
        if (!pages.contains(pageKey) && !rootPages.contains(pageKey)) {
          errors += problem(
            value.origin(),
            s"$pageKey is not one of the root pages or journey pages"
          )
        }
        SinglePagePart(
          pageKey,
          if (config.hasPath("as")) Some(config.getString("as")) else None
        )

      case value: ConfigValue if value.valueType() == ConfigValueType.STRING =>
        val pageKey = value.unwrapped().asInstanceOf[String]
        if (!pages.contains(pageKey) && !rootPages.contains(pageKey)) {
          errors += problem(
            value.origin(),
            s"$pageKey is not one of the root pages or journey pages"
          )
        }
        SinglePagePart(pageKey, None)

      case _ =>
        errors += problem(value.origin(), "Unrecognised journey entry")
        null
    }
  }

  private[journey] def deserialiseJourney(
    basePackage: String,
    rootPages: Map[String, RootPage],
    models: Map[String, AnswerModel],
    modelsPackage: QualifiedName,
    errors: mutable.ListBuffer[JourneyConfigProblem],
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
      pages.map(
        (deserialiseJourneyPage(basePackage, models, modelsPackage, errors, key, _, _)).tupled
      )

    val choicePages =
      Set.newBuilder[String]

    val journeyParts =
      parts.map(deserialiseJourneyPart(rootPages, models, journeyPages, errors, choicePages, _))

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
        errors += problem(
          config.getList("journey").get(parts.length - 1).origin(),
          "Expected the last part of the journey to be one of the root pages"
        )
    }

    key -> Journey(journeyPages ++ updatedJourneyPages, journeyParts)
  }

  private[journey] def deserialiseJourneyConfig(logger: Logger, config: Config): JourneyConfig = {
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

    val modelsPackage = QualifiedName(basePackage) / "models"

    val errors = mutable.ListBuffer.empty[JourneyConfigProblem]

    // Add a "Choice" model for Yes / No questions
    val choiceModel = EnumModel("Choice", List("Yes", "No"))
    val answerModels =
      models.map((deserialiseAnswerModel(modelsPackage, models, errors, _, _)).tupled)

    val journeyConfig = journeys.map(
      (deserialiseJourney(basePackage, rootPages, answerModels, modelsPackage, errors, _, _)).tupled
    )

    val errorList = errors.result()
    errorList.foreach { problem =>
      logger.err(s"${problem.origin.description()}: ${problem.problem}")
    }

    if (errorList.nonEmpty) { throw JourneyConfigException }

    JourneyConfig(
      basePackage,
      rootPages,
      answerModels + ("Choice" -> choiceModel),
      journeyConfig
    )
  }

  def whenConfigChanges(
    factory: CacheStoreFactory,
    configFile: File
  )(generateTask: Option[Seq[File]] => Seq[File]): Seq[File] = {
    val lastOutputCache = factory.make("lastOutput")
    val outputCache     = factory.make("outputs")
    val inputCache      = factory.make("inputs")

    // Track the last set of output files so that we can use it as our output when nothing has changed
    val lastTracker = Tracked.lastOutput[Unit, Seq[File]](lastOutputCache) { (_, lastFiles) =>
      // Track the output so that we can regenerate if anything is modified or removed
      Tracked.diffOutputs(outputCache, FileInfo.lastModified) { changeReport =>
        // Track the journey config file so that we can regenerate on configuration changes
        val inputTracker = Tracked.inputChanged(inputCache) { (configChanged, _: HashFileInfo) =>
          val outputsRemoved = changeReport.removed.nonEmpty
          val outputsChanged = changeReport.modified.nonEmpty
          if (configChanged || outputsRemoved || outputsChanged) generateTask(lastFiles)
          else lastFiles.getOrElse(generateTask(lastFiles))
        }

        inputTracker(FileInfo.hash(configFile))
      }(_.toSet)
    }

    lastTracker(())
  }

  private[journey] def cleanJourneyFiles(lastFiles: Option[Seq[File]]): Unit = {
    lastFiles.foreach(IO.delete)
  }

  private[journey] def generateJourneyFiles(
    logger: Logger,
    baseDirectory: FileRef,
    config: JourneyConfig
  ): Seq[File] = {
    val packageFolder = config.basePackage
      .split("\\.")
      .foldLeft(baseDirectory)(_ / _)

    val basePackage   = QualifiedName(config.basePackage)
    val modelsPackage = basePackage / "models"

    val rootPageFiles = config.rootPages.map { case (pageName, page) =>
      val rootPageController =
        packageFolder / "controllers" / s"${pascalCase(pageName)}Controller.scala"
      IO.write(rootPageController, RootPageController.render(basePackage, pageName, page))
      logger.info(s"Generated controller $rootPageController")
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
          logger.info(s"Generated journey model $modelFile")
          modelFile +: subJourneyModels
        case IfThenPart(choicePage, subJourney, as) =>
          val subJourneyModels = subJourney.flatMap(syntheticJourneyModels)
          val modelFields      = ModelFields.forParts(modelsPackage, journey, subJourney)
          val modelName        = pascalCase(as.getOrElse(choicePage))
          val modelFile        = packageFolder / "models" / s"$modelName.scala"
          IO.write(modelFile, JourneyModel.forIfThen(modelsPackage, modelName, modelFields))
          logger.info(s"Generated journey model $modelFile")
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
            logger.info(s"Generated journey model $modelFile")
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
        logger.info(s"Generated page object $journeyPageObjectFile")
        journeyPageObjectFile
      }

      val journeyFormProviderFiles = journey.pages.map { case (pageName, page) =>
        val journeyFormProviderFile =
          packageFolder / "forms" / s"${pascalCase(pageName)}FormProvider.scala"
        IO.write(
          journeyFormProviderFile,
          FormProvider.baseProvider(basePackage, config.models, page)
        )
        logger.info(s"Generated form provider $journeyFormProviderFile")
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
            journey,
            pageName,
            page,
          )
        )
        logger.info(s"Generated journey controller $journeyPageController")
        journeyPageController
      }

      val defaultProvidersModuleFile = packageFolder / "config" / "DefaultFormProvidersModule.scala"
      IO.write(defaultProvidersModuleFile, FormProvider.module(config))
      logger.info(s"Generated form providers module $defaultProvidersModuleFile")

      journeyControllerFiles ++
        journeyPageObjectFiles ++
        journeyFormProviderFiles ++
        journeyModelFiles(journey.journey) ++
        Seq(defaultProvidersModuleFile)
    }.toList

    val modelFiles = config.models.map { case (modelName, model) =>
      val modelFile = packageFolder / "models" / s"$modelName.scala"
      IO.write(modelFile, CustomModel.render(basePackage, model))
      logger.info(s"Generated model file $modelFile")
      modelFile
    }.toList

    val navigatorFile = packageFolder / "navigation" / s"JourneyNavigator.scala"
    IO.write(navigatorFile, Navigator.render(config))
    logger.info(s"Generated navigator $navigatorFile")

    rootPageFiles ++ modelFiles ++ journeyFiles :+ navigatorFile
  }

  private[journey] def cleanJourneyRouteFiles(lastFiles: Option[Seq[File]]): Unit = {
    lastFiles.foreach(IO.delete)
  }

  private[journey] def generateJourneyRouteFiles(
    logger: Logger,
    baseDirectory: FileRef,
    config: JourneyConfig
  ): Seq[File] = {
    val journeyRoutes = baseDirectory / "journey.routes"
    IO.write(journeyRoutes, Routes.render(config))
    logger.info(s"Generated routes file $journeyRoutes")
    Seq(journeyRoutes)
  }

  private[journey] def generateJourneyDiagramFiles(
    logger: Logger,
    baseDirectory: FileRef,
    config: JourneyConfig
  ): Seq[File] = {
    val journeyDiagrams    = PlantUml.forConfig(config)
    val journeyMermaidText = Mermaid.forConfig(config)

    val journeyTextFiles = journeyDiagrams.map { case (name, diagram) =>
      val textFile = baseDirectory / s"$name.txt"
      IO.write(textFile, diagram)
      logger.info(s"Generated PlantUML source $textFile for journey $name")
      textFile
    }.toList

    val journeyMdFiles = journeyMermaidText.map { case (name, diagram) =>
      val mdFile = baseDirectory / s"$name.md"
      IO.write(mdFile, diagram)
      logger.info(s"Generated Mermaid source $mdFile for journey $name")
      mdFile
    }

    val journeyDiagramFiles =
      try {
        val readerClass  = Class.forName("net.sourceforge.plantuml.SourceStringReader")
        val readerConstr = readerClass.getDeclaredConstructor(classOf[String])
        journeyDiagrams.map { case (name, diagram) =>
          val diagramFile  = baseDirectory / s"$name.png"
          val reader       = readerConstr.newInstance(diagram)
          val outputStream = new ByteArrayOutputStream()
          val outputImage  = readerClass.getDeclaredMethod("outputImage", classOf[OutputStream])
          outputImage.invoke(reader, outputStream)
          IO.write(diagramFile, outputStream.toByteArray)
          logger.info(s"Generated diagram $diagramFile for journey $name")
          diagramFile
        }.toList
      } catch {
        case _: ClassNotFoundException =>
          logger.warn(
            """Couldn't find PlantUML on the classpath. If you wish to generate journey diagrams, add "net.sourceforge.plantuml" % "plantuml-asl" as a dependency in project/build.sbt."""
          )
          List.empty
      }

    journeyTextFiles ++ journeyMdFiles ++ journeyDiagramFiles
  }

  private[journey] def generateJourneyTestFiles(
    logger: Logger,
    baseDirectory: FileRef,
    config: JourneyConfig
  ): Seq[File] = {
    Seq.empty
  }

  private[journey] def initialiseJourneyViewFiles(
    logger: Logger,
    baseDirectory: File,
    config: JourneyConfig,
    overwrite: Boolean = false
  ): Unit = {
    val packageFolder = baseDirectory
    // TODO: Switch to this once we have a better template
    // val packageFolder = config.basePackage
    //   .split("\\.")
    //   .foldLeft(baseDirectory)(_ / _)

    val viewsFolder = packageFolder / "views"

    config.rootPages.foreach { case (pageName, _) =>
      val viewFile = viewsFolder / s"${pascalCase(pageName)}View.scala.html"
      if (overwrite || !viewFile.exists()) {
        IO.write(viewFile, ViewStub.renderNoForm(pageName))
        logger.info(s"Generated view file $viewFile for page $pageName")
      } else {
        logger.warn(s"Skipping view file $viewFile because it already exists")
      }
    }

    config.journeys.foreach { case (_, journey) =>
      journey.pages.foreach { case (pageName, page) =>
        val viewFile = viewsFolder / s"${pascalCase(pageName)}View.scala.html"
        if (overwrite || !viewFile.exists()) {
          IO.write(viewFile, ViewStub.renderForm(config.models, page))
          logger.info(s"Generated view file $viewFile for page $pageName")
        } else {
          logger.warn(s"Skipping view file $viewFile because it already exists")
        }
      }
    }
  }

  private[journey] def initialiseJourneyFormFiles(
    logger: Logger,
    baseDirectory: File,
    config: JourneyConfig,
    overwrite: Boolean = false
  ): Unit = {
    val packageFolder = baseDirectory
    // TODO: Switch to this once we have a better template
    // val packageFolder = config.basePackage
    //   .split("\\.")
    //   .foldLeft(baseDirectory)(_ / _)

    val formsFolder = packageFolder / "forms"
    val basePackage = QualifiedName(config.basePackage)

    config.journeys.foreach { case (_, journey) =>
      journey.pages.foreach { case (pageName, page) =>
        val formFile = formsFolder / s"${pascalCase(pageName)}FormProvider.scala"
        if (overwrite || !formFile.exists()) {
          IO.write(formFile, FormProvider.providerStub(basePackage, config.models, page))
          logger.info(s"Generated form provider $formFile for page $pageName")
        } else {
          logger.warn(s"Skipping form provider $formFile because it already exists")
        }
      }
    }
  }
}
