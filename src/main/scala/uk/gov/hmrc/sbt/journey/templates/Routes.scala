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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{camelCase, kebabCase}

object Routes extends Template {
  private def appendRoute(left: String, right: String): String =
    if (left.endsWith("/")) s"$left$right" else s"$left/$right"

  private[templates] def fileUploadRoutes(
    journeyPage: JourneyPage,
    indexes: List[IndexPath]
  ): String = {
    val viewRoute              = journeyPage.viewRoute
    val uploadSuccessViewRoute = appendRoute(viewRoute, ":id/success")
    val uploadFailureViewRoute = appendRoute(viewRoute, ":id/failure")

    val changeRoute              = journeyPage.changeRoute
    val uploadSuccessChangeRoute = appendRoute(changeRoute, ":id/success")
    val uploadFailureChangeRoute = appendRoute(changeRoute, ":id/failure")

    val viewRoutePadding =
      " " * (math.max(uploadSuccessChangeRoute.length - viewRoute.length, 0) + 2)
    val viewUploadRoutePadding =
      " " * (math.max(uploadSuccessChangeRoute.length - uploadSuccessViewRoute.length, 0) + 2)
    val changeRoutePadding =
      " " * (math.max(uploadSuccessChangeRoute.length - changeRoute.length, 0) + 2)
    val changeUploadRoutePadding = " " * 2

    val controllerClass = journeyPage.controllerClass

    if (indexes.isEmpty) {
      s"""|GET ${viewRoute}${viewRoutePadding}${controllerClass}.onPageLoad(mode: Mode = NormalMode)
          |GET ${uploadSuccessViewRoute}${viewUploadRoutePadding}${controllerClass}.onUploadSuccess(id: java.util.UUID, mode: Mode = NormalMode)
          |GET ${uploadFailureViewRoute}${viewUploadRoutePadding}${controllerClass}.onUploadFailure(id: java.util.UUID, mode: Mode = NormalMode)
          |GET ${changeRoute}${changeRoutePadding}${controllerClass}.onPageLoad(mode: Mode = CheckMode)
          |GET ${uploadSuccessChangeRoute}${changeUploadRoutePadding}${controllerClass}.onUploadSuccess(id: java.util.UUID, mode: Mode = CheckMode)
          |GET ${uploadFailureChangeRoute}${changeUploadRoutePadding}${controllerClass}.onUploadFailure(id: java.util.UUID, mode: Mode = CheckMode)""".stripMargin
    } else {
      val indexRoute = indexes
        .map { path => s"${kebabCase(path.pageKey)}/:${camelCase(path.pageKey)}" }
        .mkString("/", "/", "")

      val indexParams = indexes
        .map { path => s"${camelCase(path.pageKey)}: Int" }
        .mkString(", ")

      s"""|GET ${indexRoute}${viewRoute}${viewRoutePadding}${controllerClass}.onPageLoad($indexParams, mode: Mode = NormalMode)
          |GET ${indexRoute}${uploadSuccessViewRoute}${viewUploadRoutePadding}${controllerClass}.onUploadSuccess($indexParams, id: java.util.UUID, mode: Mode = NormalMode)
          |GET ${indexRoute}${uploadFailureViewRoute}${viewUploadRoutePadding}${controllerClass}.onUploadFailure($indexParams, id: java.util.UUID, mode: Mode = NormalMode)
          |GET ${indexRoute}${changeRoute}${changeRoutePadding}${controllerClass}.onPageLoad($indexParams, mode: Mode = CheckMode)
          |GET ${indexRoute}${uploadSuccessChangeRoute}${changeUploadRoutePadding}${controllerClass}.onUploadSuccess($indexParams, id: java.util.UUID, mode: Mode = CheckMode)
          |GET ${indexRoute}${uploadFailureChangeRoute}${changeUploadRoutePadding}${controllerClass}.onUploadFailure($indexParams, id: java.util.UUID, mode: Mode = CheckMode)""".stripMargin
    }
  }

  private[templates] def standardRoutes(
    journeyPage: JourneyPage,
    indexes: List[IndexPath]
  ): String = {
    val viewRoute          = journeyPage.viewRoute
    val changeRoute        = journeyPage.changeRoute
    val controllerClass    = journeyPage.controllerClass
    val viewRoutePadding   = " " * (math.max(changeRoute.length - viewRoute.length, 0) + 2)
    val changeRoutePadding = " " * (math.max(viewRoute.length - changeRoute.length, 0) + 2)

    if (indexes.isEmpty) {
      s"""|GET  ${viewRoute}${viewRoutePadding}${controllerClass}.onPageLoad(mode: Mode = NormalMode)
          |POST ${viewRoute}${viewRoutePadding}${controllerClass}.onSubmit(mode: Mode = NormalMode)
          |GET  ${changeRoute}${changeRoutePadding}${controllerClass}.onPageLoad(mode: Mode = CheckMode)
          |POST ${changeRoute}${changeRoutePadding}${controllerClass}.onSubmit(mode: Mode = CheckMode)""".stripMargin
    } else {
      val indexRoute = indexes
        .map { path => s"${kebabCase(path.pageKey)}/:${camelCase(path.pageKey)}" }
        .mkString("/", "/", "")

      val indexParams = indexes
        .map { path => s"${camelCase(path.pageKey)}: Int" }
        .mkString(", ")

      s"""|GET  ${indexRoute}${viewRoute}${viewRoutePadding}${controllerClass}.onPageLoad($indexParams, mode: Mode = NormalMode)
          |POST ${indexRoute}${viewRoute}${viewRoutePadding}${controllerClass}.onSubmit($indexParams, mode: Mode = NormalMode)
          |GET  ${indexRoute}${changeRoute}${changeRoutePadding}${controllerClass}.onPageLoad($indexParams, mode: Mode = CheckMode)
          |POST ${indexRoute}${changeRoute}${changeRoutePadding}${controllerClass}.onSubmit($indexParams, mode: Mode = CheckMode)""".stripMargin
    }
  }

  private[templates] def withJourneyPaths(journey: Journey, journeyPage: JourneyPage)(
    renderRoute: (JourneyPage, List[IndexPath]) => String
  ): String = {
    journey
      .pathsFor(journeyPage.pageKey)
      .map(_.indexPaths)
      .map(renderRoute(journeyPage, _))
      .mkString(NL * 2)
  }

  def journeyRoutes(journeyConfig: JourneyConfig): String = {
    val rootPages =
      journeyConfig.rootPages.values.toList.sortBy(_.viewRoute)

    val rootPageRoutes = rootPages.map { case RootPage(_, _, viewRoute, controllerClass, _, _) =>
      s"GET  ${viewRoute}  ${controllerClass}.onPageLoad"
    }

    val journeyPageRoutes = journeyConfig.journeys.flatMap { case (_, journey) =>
      val journeyPages = journey.pages.values.toList.sortBy(_.viewRoute)
      journeyPages
        .map(withJourneyPaths(journey, _) { (journeyPage, indexes) =>
          if (journeyPage.answerType.isFileUpload)
            fileUploadRoutes(journeyPage, indexes)
          else
            standardRoutes(journeyPage, indexes)
        })
        .filterNot(_.isEmpty)
    }

    (rootPageRoutes ++ journeyPageRoutes).mkString(NL * 2)
  }

  def internalRoutes(basePackage: QualifiedName): String = {
    s"""+ nocsrf
       |POST /file-upload/:id/notification  ${basePackage}.controllers.upscan.UpscanNotificationBaseController.onNotificationReceived(id: java.util.UUID)""".stripMargin
  }
}
