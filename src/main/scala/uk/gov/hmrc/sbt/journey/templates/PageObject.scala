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
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{camelCase, pascalCase}

object PageObject extends Template {
  private def applyParams(journey: Journey, path: JourneyPath): List[String] =
    path.paths.collect {
      case IndexPath(pageKey) =>
        s"${camelCase(pageKey)}Index: Int"
      case ChoicePath(pageKey, _) =>
        val answerType = journey.pages(pageKey).answerType
        val choiceType = ModelFields.fieldType(answerType)
        s"$pageKey: $choiceType"
    }

  private def submitRouteFor(pageName: String, path: JourneyPath): String = {
    val indexPaths = path.indexPaths
    if (indexPaths.isEmpty)
      s"routes.${pageName}BaseController.onSubmit"
    else
      path.indexPaths
        .map(idx => s"${camelCase(idx.pageKey)}Index")
        .mkString(
          s"mode => routes.${pageName}BaseController.onSubmit(",
          ", ",
          ", mode)"
        )
  }

  private def jsPathFor(path: JourneyPath): String =
    path.paths
      .flatMap {
        case IndexPath(pageKey) =>
          List(s""""$pageKey"""", s"${pageKey}Index")
        case ChoicePath(pageKey, _) =>
          List(s""""$pageKey"""", s"$pageKey.toString")
        case StringPath(pageKey) =>
          List(s""""$pageKey"""")
        case Root =>
          List("JsPath")
      }
      .mkString(" \\ ")

  private def unapplyTypeFor(journey: Journey, path: JourneyPath): String = {
    val paths = path.paths.collect {
      case IndexPath(_) => "Int"
      case ChoicePath(pageKey, _) =>
        val answerType = journey.pages(pageKey).answerType
        ModelFields.fieldType(answerType)
    }

    if (paths.length == 1) paths.head
    else paths.mkString("(", ", ", ")")
  }

  private def unapplyResultFor(journey: Journey, path: JourneyPath): String = {
    val paths = path.paths.collect {
      case IndexPath(pageKey) =>
        s"${pageKey}Index"
      case ChoicePath(pageKey, _) =>
        val answerType = journey.pages(pageKey).answerType
        val choiceType = ModelFields.fieldType(answerType)
        s"$choiceType.valueOf($pageKey)"
    }

    if (paths.length == 1) paths.head
    else paths.mkString("(", ", ", ")")
  }

  private def jsPathNodesFor(path: JourneyPath): String =
    path.paths
      .flatMap {
        case IndexPath(pageKey) =>
          List(s"""KeyPathNode("$pageKey")""", s"IdxPathNode(${pageKey}Index)")
        case ChoicePath(pageKey, _) =>
          List(s"""KeyPathNode("$pageKey")""", s"KeyPathNode($pageKey)")
        case StringPath(pageKey) =>
          List(s"""KeyPathNode("$pageKey")""")
        case _ =>
          List.empty
      }
      .mkString("", " :: ", " :: Nil")

  def applyMethod(pageName: String, journey: Journey, path: JourneyPath): String = {
    val params       = applyParams(journey, path)
    val paramsString = if (params.isEmpty) "" else params.mkString("(", ", ", ")")
    val jsPath       = jsPathFor(path)
    val submitRoute  = submitRouteFor(pageName, path)
    s"""|  def apply$paramsString: ${pageName}Page =
        |    new ${pageName}Page(
        |      $jsPath,
        |      $submitRoute
        |    )""".stripMargin
  }

  def unapplyMethod(pageName: String, journey: Journey, path: JourneyPath): String = {
    val unapplyType   = unapplyTypeFor(journey, path)
    val unapplyResult = unapplyResultFor(journey, path)
    val jsPathNodes   = jsPathNodesFor(path)
    s"""|  def unapply(page: ${pageName}Page): Option[$unapplyType] =
        |    page.path.path match {
        |      case $jsPathNodes => Some($unapplyResult)
        |      case _ => None
        |    }""".stripMargin
  }

  def render(basePackage: QualifiedName, journey: Journey, journeyPage: JourneyPage): String = {
    val pagesPackage    = basePackage / "pages"
    val capitalPageName = pascalCase(journeyPage.pageKey)
    val overloads       = journey.pathsFor(journeyPage.pageKey)
    val answerType      = journey.pages(journeyPage.pageKey).answerType

    val pageType = ModelFields.fieldType(answerType)

    val answerTypeImports = Imports.importedSymbols(answerType)

    val choiceModelImports = overloads
      .flatMap(_.choicePaths)
      .map { case ChoicePath(pageKey, _) =>
        Imports.importedSymbols(journey.pages(pageKey).answerType)
      }

    val importedPrefixes = choiceModelImports.foldLeft(answerTypeImports)(Imports.merge)
    val imports = Imports.importsFor(pagesPackage, importedPrefixes, addFormatImports = false)

    if (overloads.length == 1 && applyParams(journey, overloads.head).isEmpty) {
      s"""package ${basePackage / "pages"}
         |
         |import models.Mode // ${basePackage / "models.Mode"}
         |import _root_.pages.* // TODO: Remove this once we have a better template
         |import play.api.libs.json.JsPath
         |import play.api.mvc.Call
         |import ${basePackage / "controllers.routes"}
         |$imports
         |
         |object ${capitalPageName}Page extends QuestionPage[$pageType] {
         |  override def path: JsPath = ${jsPathFor(overloads.head)}
         |  override def submitRoute(mode: Mode): Call = routes.${capitalPageName}BaseController.onSubmit(mode)
         |  override def toString: String = "${journeyPage.pageKey}"
         |}
         |""".stripMargin
    } else {
      val applyMethods =
        overloads.map(applyMethod(capitalPageName, journey, _)).mkString(NL)
      val unapplyMethods =
        overloads.map(unapplyMethod(capitalPageName, journey, _)).mkString(NL)

      s"""package ${basePackage / "pages"}
         |
         |import models.Mode // ${basePackage / "models.Mode"}
         |import _root_.pages.* // TODO: Remove this once we have a better template
         |import play.api.libs.json.{JsPath, KeyPathNode, IdxPathNode}
         |import play.api.mvc.Call
         |import ${basePackage / "controllers.routes"}
         |$imports
         |
         |case class ${capitalPageName}Page private (override val path: JsPath, makeRoute: Mode => Call) extends QuestionPage[$pageType] {
         |  override def submitRoute(mode: Mode): Call = makeRoute(mode)
         |  override def toString: String = "${journeyPage.pageKey}"
         |}
         |
         |object ${capitalPageName}Page {
         |$applyMethods
         |
         |$unapplyMethods
         |}
         |""".stripMargin
    }
  }
}
