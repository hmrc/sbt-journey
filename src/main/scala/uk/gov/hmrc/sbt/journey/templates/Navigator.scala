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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.pascalCase

class Navigator(models: Map[String, AnswerModel]) extends Template {
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
    mode: String,
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
    mode: String,
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

  private def incRouteParams(mode: String, indexPaths: List[IndexPath]): String = {
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

  private def routesFor(
    mode: String,
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
      val firstPage      = pascalCase(subJourney.head.startPage)
      val firstPartPaths = journeyPath.indexPaths ++ subJourney.head.startPageIndexes
      val firstParams    = routeParams(mode, journeyPath.indexPaths, firstPartPaths)
      val nextPartPage   = pascalCase(nextPart.startPage)
      val nextParams     = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)
      val choicePath     = journeyPath / ChoicePath(as.getOrElse(choicePage), "Yes")
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
      val answerType                = pages(choicePage).answerType
      val choiceType                = ModelFields.fieldType(pages(choicePage).answerType)
      val Some(EnumModel(_, cases)) = answerType.typeName.flatMap(models.get)
      val uncoveredCases            = cases.toSet.diff(subJourneys.keySet)
      val hasDefault                = subJourneys.contains("default")
      val isExhaustive              = uncoveredCases.isEmpty || hasDefault
      val nextPartPage              = pascalCase(nextPart.startPage)
      val nextParams                = nextRouteParams(mode, pages, journeyPath, nextPart, nextPath)
      val wildcardRoute =
        if (isExhaustive) ""
        else s"""$NL      case _ => routes.${nextPartPage}BaseController.onPageLoad$nextParams"""
      val choiceRoutes = subJourneys.map { case (choice, subJourney) =>
        val firstPage      = pascalCase(subJourney.head.startPage)
        val firstPartPaths = journeyPath.indexPaths ++ subJourney.head.startPageIndexes
        val firstParams    = routeParams(mode, journeyPath.indexPaths, firstPartPaths)
        val choiceCase     = if (choice == "default") "_" else s"$choiceType.$choice"
        s"""      case $choiceCase => routes.${firstPage}BaseController.onPageLoad$firstParams"""
      }.toList.sorted
      val choicePageRoutes =
        s"""|    case ${pascalCase(choicePage)}Page${unapplyParams(pages, journeyPath)} => _ => {
            |${choiceRoutes.mkString(NL)}$wildcardRoute
            |    }""".stripMargin
      val subJourneyRoutes = subJourneys.flatMap { case (choice, subJourney) =>
        val choicePath = journeyPath / ChoicePath(as.getOrElse(choicePage), choice)
        routesFor(mode, pages, subJourney, choicePath, nextPart, nextPath)
      }.toList
      choicePageRoutes :: subJourneyRoutes
  }

  private def routesFor(
    mode: String,
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
        routesFor("NormalMode", journey.pages, part, Root, nextPart, Root)
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
        initialParts.flatMap(routesFor("CheckMode", journey.pages, _, Root, lastPart, Root))
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
}
