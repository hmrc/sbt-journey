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

import uk.gov.hmrc.sbt.journey.models.*
import uk.gov.hmrc.sbt.journey.templates.Imports.JavaTimePrefix
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.pascalCase

class Navigator(models: Map[String, AnswerModel]) extends Template {
  val importCollector = new ImportCollector(models)

  private def unapplyParams(pages: Map[String, JourneyPage], journeyPath: JourneyPath): String = {
    val paths = journeyPath.paths
      .map {
        case IndexPath(pageKey)       => s"${pageKey}Index"
        case ChoicePath(_, "default") => "_"
        case ChoicePath(pageKey, choice) =>
          val choiceType = ModelFields.fieldType(pages(pageKey).answerType)
          s"$choiceType.$choice"
        case _ => ""
      }
      .filterNot(_.isEmpty)

    if (paths.isEmpty) "" else paths.mkString("(", ",", ")")
  }

  private def routeParams(
    mode: Mode,
    fromIndexPaths: List[IndexPath],
    toIndexPaths: List[IndexPath]
  ): String = {
    val paths = fromIndexPaths.zipAll(toIndexPaths, null, null)
    if (paths.isEmpty) s"($mode)"
    else {
      val params = paths
        .map {
          // We don't have the index on the current page so we are heading into the first iteration
          case (null, IndexPath(_)) => "0"
          // We have a corresponding index parameter so we should pass it along
          case (IndexPath(from), IndexPath(to)) if from == to => s"${to}Index"
          // We have index parameters, but they aren't for the same do-while journey
          case (IndexPath(_), IndexPath(_)) => "0"
          // The destination does not need an index parameter
          case (IndexPath(_), null) => ""
        }
        .filterNot(_.isEmpty)

      if (params.isEmpty) s"($mode)"
      else params.mkString("(", ", ", s", $mode)")
    }
  }

  private def nextRouteParams(
    mode: Mode,
    pages: Map[String, JourneyPage],
    fromPath: JourneyPath,
    nextPart: JourneyPart,
    nextPath: JourneyPath
  ): String = {
    val nextPartPaths = nextPath.indexPaths ++ nextPart.startPageIndexes
    if (!pages.contains(nextPart.startPage)) ""
    else
      routeParams(mode, fromPath.indexPaths, nextPartPaths)
  }

  private def incRouteParams(mode: Mode, indexPaths: List[IndexPath]): String = {
    if (indexPaths.isEmpty) s"($mode)"
    else {
      // Set the last index path to index + 1 to head to the next iteration
      val initialIndexPaths = indexPaths.dropRight(1)
      val lastIndexPath     = indexPaths.last
      if (initialIndexPaths.isEmpty)
        s"(${lastIndexPath.pageKey}Index + 1, $mode)"
      else
        initialIndexPaths
          .map(idx => s"${idx.pageKey}Index")
          .mkString("(", ", ", s", ${lastIndexPath.pageKey}Index + 1, $mode)")
    }
  }

  private def switchCaseRoute(
    mode: Mode,
    journeyPath: JourneyPath,
    answerType: FieldType,
    nextPartPage: String,
    nextParams: String,
    choice: String,
    subJourney: List[JourneyPart]
  ): String = {
    val choiceType = ModelFields.fieldType(answerType)
    val choiceCase = if (choice == "default") "_" else s"$choiceType.$choice"
    subJourney.headOption
      .map { firstPart =>
        val firstPage      = pascalCase(firstPart.startPage)
        val firstPartPaths = journeyPath.indexPaths ++ firstPart.startPageIndexes
        val firstParams    = routeParams(mode, journeyPath.indexPaths, firstPartPaths)
        s"""      case $choiceCase => routes.${firstPage}BaseController.onPageLoad$firstParams"""
      }
      .getOrElse {
        s"""      case $choiceCase => routes.${nextPartPage}BaseController.onPageLoad$nextParams"""
      }
  }

  private def routesFor(
    mode: Mode,
    pages: Map[String, JourneyPage],
    journeyPart: JourneyPart,
    journeyPath: JourneyPath,
    nextPart: JourneyPart,
    nextPath: JourneyPath
  ): List[String] = journeyPart match {
    case SinglePagePart(pageKey, _) =>
      val nextPartPage = pascalCase(nextPart.startPage)
      val nextParams   = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)
      List(
        s"""|    case ${pascalCase(pageKey)}Page${unapplyParams(pages, journeyPath)} => _ => _ =>
            |      routes.${nextPartPage}BaseController.onPageLoad$nextParams""".stripMargin
      )

    case IfThenPart(choicePage, subJourney, as) =>
      val firstPage    = pascalCase(subJourney.head.startPage)
      val firstParams  = nextRouteParams(mode, pages, journeyPath, subJourney.head, journeyPath)
      val nextPartPage = pascalCase(nextPart.startPage)
      val nextParams   = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)
      val choicePath   = journeyPath / ChoicePath(as.getOrElse(choicePage), "Yes")
      val choicePageRoutes =
        s"""    case ${pascalCase(choicePage)}Page${unapplyParams(pages, journeyPath)} => _ => {
           |      case Choice.Yes => routes.${firstPage}BaseController.onPageLoad$firstParams
           |      case Choice.No  => routes.${nextPartPage}BaseController.onPageLoad$nextParams
           |    }""".stripMargin
      val subJourneyRoutes = routesFor(mode, pages, subJourney, choicePath, nextPart, nextPath)
      choicePageRoutes :: subJourneyRoutes

    case DoWhilePart(choicePage, subJourney, as) =>
      val indexPath    = journeyPath / IndexPath(as)
      val firstPage    = pascalCase(subJourney.head.startPage)
      val firstParams  = incRouteParams(mode, indexPath.indexPaths)
      val nextPartPage = pascalCase(nextPart.startPage)
      val nextParams   = nextRouteParams(mode, pages, indexPath, nextPart, nextPath)
      val choicePageRoutes =
        s"""|    case ${pascalCase(choicePage)}Page${unapplyParams(pages, indexPath)} => _ => {
            |      case Choice.Yes => routes.${firstPage}BaseController.onPageLoad$firstParams
            |      case Choice.No  => routes.${nextPartPage}BaseController.onPageLoad$nextParams
            |    }""".stripMargin
      val choicePagePart = SinglePagePart(choicePage, None)
      val subJourneyRoutes =
        routesFor(mode, pages, subJourney, indexPath, choicePagePart, indexPath)
      choicePageRoutes :: subJourneyRoutes

    case SwitchCasePart(choicePage, subJourneys, as) =>
      val answerType = pages(choicePage).answerType

      val Some(model @ EnumModel(_, _)) = answerType.typeName.flatMap(models.get)

      val nextPartPage = pascalCase(nextPart.startPage)
      val nextParams   = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)

      val uncoveredCases = model.uncoveredCases(subJourneys.keySet)

      val wildcardRoute =
        if (model.isCoveredBy(subJourneys.keySet)) ""
        else if (uncoveredCases.size == 1) {
          val choiceCase = uncoveredCases.head
          val choiceType = ModelFields.fieldType(answerType)
          s"""$NL      case $choiceType.$choiceCase => routes.${nextPartPage}BaseController.onPageLoad$nextParams"""
        } else {
          s"""$NL      case _ => routes.${nextPartPage}BaseController.onPageLoad$nextParams"""
        }

      val choiceRoutes = subJourneys
        .filterKeys(_ != "default")
        .map(
          (switchCaseRoute(mode, journeyPath, answerType, nextPartPage, nextParams, _, _)).tupled
        )

      val defaultRoute = subJourneys
        .get("default")
        .map(switchCaseRoute(mode, journeyPath, answerType, nextPartPage, nextParams, "default", _))

      val choicePageRoutes =
        s"""|    case ${pascalCase(choicePage)}Page${unapplyParams(pages, journeyPath)} => _ => {
            |${(choiceRoutes ++ defaultRoute).mkString(NL)}$wildcardRoute
            |    }""".stripMargin

      val subJourneyRoutes = subJourneys
        .filterKeys(_ != "default")
        .flatMap { case (choice, subJourney) =>
          val choicePath = journeyPath / ChoicePath(as.getOrElse(choicePage), choice)
          routesFor(mode, pages, subJourney, choicePath, nextPart, nextPath)
        }
        .toList

      val defaultSubJourneyRoutes = subJourneys
        .get("default")
        .toList
        .flatMap { subJourney =>
          val choicePath = journeyPath / ChoicePath(as.getOrElse(choicePage), "default")
          routesFor(mode, pages, subJourney, choicePath, nextPart, nextPath)
        }

      choicePageRoutes :: subJourneyRoutes ::: defaultSubJourneyRoutes
  }

  private def routesFor(
    mode: Mode,
    pages: Map[String, JourneyPage],
    journeyParts: List[JourneyPart],
    journeyPath: JourneyPath,
    nextPart: JourneyPart,
    nextPath: JourneyPath
  ): List[String] = {
    // Routes that navigate from one page to the next between the journey parts
    val sequentialRoutes = journeyParts.sliding(2).toList.flatMap {
      case firstPart :: secondPart :: Nil =>
        routesFor(mode, pages, firstPart, journeyPath, secondPart, journeyPath)
      case _ => Nil
    }

    // Routes that navigate from the end of this journey part to the next outer part
    val continuationRoutes = journeyParts.lastOption.toList.flatMap { lastPart =>
      routesFor(mode, pages, lastPart, journeyPath, nextPart, nextPath)
    }

    sequentialRoutes ++ continuationRoutes
  }

  private def normalRoutesFor(journey: Journey): List[String] =
    // We should go to the next journey part after completing each part in "Normal" mode
    journey.journey.sliding(2).toList.flatMap {
      case part :: nextPart :: Nil =>
        routesFor(NormalMode, journey.pages, part, Root, nextPart, Root)
      case _ => Nil
    }

  private[templates] def normalRoutesFor(journeyConfig: JourneyConfig): String = {
    val routes = journeyConfig.journeys.values.toList.flatMap(normalRoutesFor)
    if (routes.isEmpty) "" else routes.mkString(NL)
  }

  private def checkRoutesFor(journey: Journey): List[String] = {
    // We should go to the last journey part after completing each part in "Check" mode
    val lastPart     = journey.journey.lastOption
    val initialParts = journey.journey.dropRight(1)
    lastPart
      .map { lastPart =>
        initialParts.flatMap(routesFor(CheckMode, journey.pages, _, Root, lastPart, Root))
      }
      .getOrElse(List.empty)
  }

  private[templates] def checkRoutesFor(journeyConfig: JourneyConfig): String = {
    val routes = journeyConfig.journeys.values.toList.flatMap(checkRoutesFor)
    if (routes.isEmpty) "" else routes.mkString(NL)
  }

  def render(config: JourneyConfig): String = {
    val basePackage = QualifiedName(config.basePackage)

    s"""package ${basePackage / "navigation"}
       |
       |import ${basePackage / "controllers.routes"}
       |import ${basePackage / "models.*"}
       |import ${basePackage / "pages.*"}
       |import _root_.models.Mode // ${basePackage / "models.Mode"}
       |import _root_.models.CheckMode // ${basePackage / "models.CheckMode"}
       |import _root_.models.NormalMode // ${basePackage / "models.NormalMode"}
       |import _root_.models.UserAnswers // ${basePackage / "models.UserAnswers"}
       |import _root_.pages.Page // ${basePackage / "pages.Page"}
       |import _root_.pages.QuestionPage // ${basePackage / "pages.QuestionPage"}
       |import com.google.inject.ImplementedBy
       |import play.api.mvc.Call
       |
       |import javax.inject.{Inject,Singleton}
       |
       |@ImplementedBy(classOf[DefaultJourneyNavigator])
       |trait JourneyNavigator {
       |  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, latestAnswer: page.AnswerType): Call
       |}
       |
       |@Singleton
       |class DefaultJourneyNavigator @Inject() () extends JourneyNavigator {
       |  private val normalRoutes: (page: Page) => UserAnswers => page.AnswerType => Call = {
       |${normalRoutesFor(config)}
       |  }
       |
       |  private val checkRoutes: (page: Page) => UserAnswers => page.AnswerType => Call = {
       |${checkRoutesFor(config)}
       |  }
       |
       |  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, latestAnswer: page.AnswerType): Call =
       |    mode match {
       |      case NormalMode => normalRoutes(page)(userAnswers)(latestAnswer)
       |      case CheckMode  => checkRoutes(page)(userAnswers)(latestAnswer)
       |    }
       |}
       |""".stripMargin
  }

  private def indexParamDecls(journeyPath: JourneyPath): String = {
    val indexParams = journeyPath.indexPaths.map { case IndexPath(pageKey) =>
      s"""    val ${pageKey}Index = 0"""
    }

    if (indexParams.isEmpty) ""
    else indexParams.mkString("", NL, NL)
  }

  private def applyParams(pages: Map[String, JourneyPage], journeyPath: JourneyPath): String = {
    val paths = journeyPath.paths
      .map {
        case IndexPath(pageKey) =>
          s"${pageKey}Index"
        case ChoicePath(pageKey, choice) =>
          val choiceType = ModelFields.fieldType(pages(pageKey).answerType)
          s"$choiceType.$choice"
        case _ => ""
      }
      .filterNot(_.isEmpty)

    if (paths.isEmpty) "" else paths.mkString("(", ", ", ")")
  }

  private def testDescParams(journeyPath: JourneyPath): String = {
    val (_, paths) = journeyPath.paths
      .foldLeft((0, List.empty[String])) {
        case ((idx, ps), IndexPath(_))          => (idx + 1, ('i' + idx).toChar.toString :: ps)
        case ((idx, ps), ChoicePath(_, choice)) => (idx, choice :: ps)
        case ((idx, ps), _)                     => (idx, ps)
      }

    if (paths.isEmpty) "" else paths.reverse.mkString("(", ", ", ")")
  }

  private def switchCaseTest(
    mode: Mode,
    indexParams: String,
    descParams: String,
    currentPage: String,
    currentParams: String
  )(nextPage: String, nextParams: String, choiceType: String, choice: String) = {
    s"""should "navigate from ${currentPage}Page$descParams to ${nextPage}Page when the user chooses $choice in ${mode.testDescription}" in {
       |${indexParams}    navigator.nextPage(${currentPage}Page$currentParams, $mode, userAnswers, $choiceType.$choice) shouldBe routes.${nextPage}BaseController.onPageLoad$nextParams
       |  }""".stripMargin
  }

  private def testsFor(
    mode: Mode,
    pages: Map[String, JourneyPage],
    journeyPart: JourneyPart,
    journeyPath: JourneyPath,
    nextPart: JourneyPart,
    nextPath: JourneyPath
  ): List[String] = journeyPart match {
    case SinglePagePart(pageKey, _) =>
      val answerType    = pages(pageKey).answerType
      val generatorType = ModelFields.fieldType(answerType)
      val currentPage   = pascalCase(pageKey)
      val currentParams = applyParams(pages, journeyPath)
      val indexParams   = indexParamDecls(journeyPath)
      val descParams    = testDescParams(journeyPath)
      val nextPartPage  = pascalCase(nextPart.startPage)
      val nextParams    = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)
      List(
        s"""should "navigate from ${currentPage}Page$descParams to ${nextPartPage}Page for all answers in ${mode.testDescription}" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
           |${indexParams}    navigator.nextPage(${currentPage}Page$currentParams, $mode, userAnswers, answer) shouldBe routes.${nextPartPage}BaseController.onPageLoad$nextParams
           |  }""".stripMargin
      )
    case IfThenPart(choicePage, subJourney, as) =>
      val currentPage   = pascalCase(choicePage)
      val currentParams = applyParams(pages, journeyPath)
      val indexParams   = indexParamDecls(journeyPath)
      val descParams    = testDescParams(journeyPath)
      val firstPage     = pascalCase(subJourney.head.startPage)
      val firstParams   = nextRouteParams(mode, pages, journeyPath, subJourney.head, journeyPath)
      val nextPartPage  = pascalCase(nextPart.startPage)
      val nextParams    = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)
      val choicePath    = journeyPath / ChoicePath(as.getOrElse(choicePage), "Yes")
      val choicePageTests = List(
        s"""should "navigate from ${currentPage}Page$descParams to ${firstPage}Page when the user chooses Yes in ${mode.testDescription}" in {
           |${indexParams}    navigator.nextPage(${currentPage}Page$currentParams, $mode, userAnswers, Choice.Yes) shouldBe routes.${firstPage}BaseController.onPageLoad$firstParams
           |  }""".stripMargin,
        s"""should "navigate from ${currentPage}Page$descParams to ${nextPartPage}Page when the user chooses No in ${mode.testDescription}" in {
           |${indexParams}    navigator.nextPage(${currentPage}Page$currentParams, $mode, userAnswers, Choice.No) shouldBe routes.${nextPartPage}BaseController.onPageLoad$nextParams
           |  }""".stripMargin
      )
      val subJourneyTests = testsFor(mode, pages, subJourney, choicePath, nextPart, nextPath)
      choicePageTests ++ subJourneyTests
    case DoWhilePart(choicePage, subJourney, as) =>
      val indexPath     = journeyPath / IndexPath(as)
      val indexParams   = indexParamDecls(indexPath)
      val descParams    = testDescParams(indexPath)
      val currentPage   = pascalCase(choicePage)
      val currentParams = applyParams(pages, indexPath)
      val firstPage     = pascalCase(subJourney.head.startPage)
      val firstParams   = incRouteParams(mode, indexPath.indexPaths)
      val nextPartPage  = pascalCase(nextPart.startPage)
      val nextParams    = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)
      val choicePageTests = List(
        s"""should "navigate from ${currentPage}Page$descParams to ${firstPage}Page at the next index when the user chooses Yes in ${mode.testDescription}" in {
           |${indexParams}    navigator.nextPage(${currentPage}Page$currentParams, $mode, userAnswers, Choice.Yes) shouldBe routes.${firstPage}BaseController.onPageLoad$firstParams
           |  }""".stripMargin,
        s"""should "navigate from ${currentPage}Page$descParams to ${nextPartPage}Page when the user chooses No in ${mode.testDescription}" in {
           |${indexParams}    navigator.nextPage(${currentPage}Page$currentParams, $mode, userAnswers, Choice.No) shouldBe routes.${nextPartPage}BaseController.onPageLoad$nextParams
           |  }""".stripMargin
      )
      val choicePagePart  = SinglePagePart(choicePage, None)
      val subJourneyTests = testsFor(mode, pages, subJourney, indexPath, choicePagePart, indexPath)
      subJourneyTests ++ choicePageTests

    case SwitchCasePart(choicePage, subJourneys, as) =>
      val currentPage   = pascalCase(choicePage)
      val currentParams = applyParams(pages, journeyPath)
      val indexParams   = indexParamDecls(journeyPath)
      val descParams    = testDescParams(journeyPath)

      val answerType = pages(choicePage).answerType
      val choiceType = ModelFields.fieldType(answerType)

      val Some(model @ EnumModel(_, _)) = answerType.typeName.flatMap(models.get)
      val uncoveredCases                = model.uncoveredCases(subJourneys.keySet)

      val nextPartPage = pascalCase(nextPart.startPage)
      val nextParams   = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)

      val testFor = switchCaseTest(mode, indexParams, descParams, currentPage, currentParams) _

      val choiceTests = subJourneys
        .filterKeys(_ != "default")
        .map { case (choice, subJourney) =>
          subJourney.headOption
            .map { firstPart =>
              val firstPage   = pascalCase(firstPart.startPage)
              val firstParams = nextRouteParams(mode, pages, journeyPath, firstPart, journeyPath)
              testFor(firstPage, firstParams, choiceType, choice)
            }
            .getOrElse {
              testFor(nextPartPage, nextParams, choiceType, choice)
            }
        }
        .toList

      val defaultTests = subJourneys
        .get("default")
        .toList
        .flatMap { subJourney =>
          for (choice <- uncoveredCases) yield {
            val firstPage =
              pascalCase(subJourney.head.startPage)
            val firstParams =
              nextRouteParams(mode, pages, journeyPath, subJourney.head, journeyPath)
            testFor(firstPage, firstParams, choiceType, choice)
          }
        }

      val uncoveredCaseTests =
        if (model.isCoveredBy(subJourneys.keySet)) List.empty
        else
          uncoveredCases.toList.map { choice =>
            testFor(nextPartPage, nextParams, choiceType, choice)
          }

      val subJourneyTests = subJourneys.flatMap {
        case ("default", subJourney) =>
          val choicePath = journeyPath / ChoicePath(as.getOrElse(choicePage), uncoveredCases.head)
          testsFor(mode, pages, subJourney, choicePath, nextPart, nextPath)
        case (choice, subJourney) =>
          val choicePath = journeyPath / ChoicePath(as.getOrElse(choicePage), choice)
          testsFor(mode, pages, subJourney, choicePath, nextPart, nextPath)
      }

      choiceTests ++ defaultTests ++ uncoveredCaseTests ++ subJourneyTests
  }

  private def testsFor(
    mode: Mode,
    pages: Map[String, JourneyPage],
    journeyParts: List[JourneyPart],
    journeyPath: JourneyPath,
    nextPart: JourneyPart,
    nextPath: JourneyPath
  ): List[String] = {
    // Routes that navigate from one page to the next between the journey parts
    val sequentialRoutes = journeyParts.sliding(2).toList.flatMap {
      case firstPart :: secondPart :: Nil =>
        testsFor(mode, pages, firstPart, journeyPath, secondPart, journeyPath)
      case _ => Nil
    }

    // Routes that navigate from the end of this journey part to the next outer part
    val continuationRoutes = journeyParts.lastOption.toList.flatMap { lastPart =>
      testsFor(mode, pages, lastPart, journeyPath, nextPart, nextPath)
    }

    sequentialRoutes ++ continuationRoutes
  }

  def normalModeTestsFor(journey: Journey): List[String] = {
    journey.journey.sliding(2).toList.flatMap {
      case part :: nextPart :: Nil =>
        testsFor(NormalMode, journey.pages, part, Root, nextPart, Root)
      case _ => Nil
    }
  }

  private[templates] def normalModeTestsFor(config: JourneyConfig): List[String] = {
    config.journeys.values.toList.flatMap { journey =>
      normalModeTestsFor(journey)
    }
  }

  def checkModeTestsFor(journey: Journey): List[String] = {
    val lastPart     = journey.journey.lastOption
    val initialParts = journey.journey.dropRight(1)
    lastPart
      .map { lastPart =>
        initialParts.flatMap(testsFor(CheckMode, journey.pages, _, Root, lastPart, Root))
      }
      .getOrElse(List.empty)
  }

  private[templates] def checkModeTestsFor(config: JourneyConfig): List[String] = {
    config.journeys.values.toList.flatMap { journey =>
      checkModeTestsFor(journey)
    }
  }

  def renderSpec(config: JourneyConfig): String = {
    val basePackage = QualifiedName(config.basePackage)

    val allTests = normalModeTestsFor(config) ++ checkModeTestsFor(config)

    val testCases =
      if (allTests.isEmpty) ""
      else
        allTests.mkString(
          s"""$NL  "DefaultJourneyNavigator" """,
          s"${NL * 2}  it ",
          ""
        )

    val answerTypeImports = config.journeys.flatMap { case (_, journey) =>
      journey.pages.map { case (_, page) =>
        importCollector.importedSymbols(page.answerType, recursive = false)
      }
    }

    val modelImports = models.collect { case (_, CaseClassModel(_, fields)) =>
      importCollector.importedSymbols(fields, recursive = true)
    }

    val allImports = (answerTypeImports ++ modelImports).foldLeft(
      Map.empty[List[String], Set[String]]
    )(Imports.merge)

    val usesLocalDate =
      allImports.contains(JavaTimePrefix) &&
        allImports(JavaTimePrefix).contains("LocalDate")

    val localDateImport =
      if (usesLocalDate)
        s"import java.time.LocalDate$NL"
      else
        ""

    s"""package ${basePackage / "navigation"}
       |
       |import _root_.generators.Generators // ${basePackage / "generators.Generators"}
       |import _root_.models.CheckMode // ${basePackage / "models.CheckMode"}
       |import _root_.models.NormalMode // ${basePackage / "models.NormalMode"}
       |import _root_.models.UserAnswers // ${basePackage / "models.UserAnswers"}
       |import ${basePackage / "controllers.routes"}
       |import ${basePackage / "models.*"}
       |import ${basePackage / "pages.*"}
       |${localDateImport}import org.scalatest.flatspec.AnyFlatSpec
       |import org.scalatest.matchers.should.Matchers
       |import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
       |
       |class DefaultJourneyNavigatorSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, Generators {
       |  private val navigator = new DefaultJourneyNavigator()
       |  private val userAnswers = UserAnswers("userId")
       |$testCases
       |}
       |""".stripMargin
  }
}
