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

import uk.gov.hmrc.sbt.journey.models.{Journey, JourneyConfig, JourneyPage, RootPage}
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{camelCase, kebabCase}

object Routes {
  private[templates] def journeyRoutes(journey: Journey, journeyPage: JourneyPage): String = {
    val viewRoute       = journeyPage.viewRoute
    val changeRoute     = journeyPage.changeRoute
    val controllerClass = journeyPage.controllerClass
    val viewRoutePadding = " " * (math.max(changeRoute.length - viewRoute.length, 0) + 2)
    val changeRoutePadding = " " * (math.max(viewRoute.length - changeRoute.length, 0) + 2)

    journey
      .pathsFor(journeyPage.pageKey)
      .map(_.indexPaths)
      .map { indexes =>
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
      .mkString(System.lineSeparator() * 2)
  }

  def render(journeyConfig: JourneyConfig): String = {
    val rootPages =
      journeyConfig.rootPages.values.toList.sortBy(_.viewRoute)

    val rootPageRoutes = rootPages.map { case RootPage(_, _, viewRoute, controllerClass, _) =>
      s"GET  ${viewRoute}  ${controllerClass}.onPageLoad"
    }

    val journeyPageRoutes = journeyConfig.journeys.flatMap { case (_, journey) =>
      val journeyPages = journey.pages.values.toList.sortBy(_.viewRoute)
      journeyPages.map { journeyPage =>
        journeyRoutes(journey, journeyPage)
      }
    }

    (rootPageRoutes ++ journeyPageRoutes).mkString(System.lineSeparator() * 2)
  }
}
