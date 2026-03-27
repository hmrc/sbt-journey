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

import scala.annotation.tailrec

object PageObject {
  private def applyParams(journey: Journey, path: JourneyPath): List[String] = {
    @tailrec def params(paths: List[PathAtom], acc: List[String] = Nil): List[String] =
      paths match {
        case Nil =>
          acc.reverse
        case IndexPath(pageKey) :: tail =>
          val indexParam = s"${camelCase(pageKey)}Index: Int"
          params(tail, indexParam :: acc)
        case ChoicePath(pageKey, _) :: tail =>
          val choiceType  = ModelFields.fieldType(journey.pages(pageKey).answerType)
          val choiceParam = s"$pageKey: $choiceType"
          params(tail, choiceParam :: acc)
        case _ :: tail =>
          params(tail, acc)
      }

    params(path.paths)
  }

  private def submitRouteFor(pageName: String, path: JourneyPath): String = {
    val indexPaths = path.indexPaths
    if (indexPaths.isEmpty) s"routes.${pageName}BaseController.onSubmit"
    else
      path.indexPaths
        .map(idx => s"${camelCase(idx.pageKey)}Index")
        .mkString(
          s"mode => routes.${pageName}BaseController.onSubmit(",
          ", ",
          ", mode)"
        )
  }

  private def jsPathFor(path: JourneyPath): String = {
    @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): String = paths match {
      case Nil => acc.reverse.mkString(" \\ ")
      case IndexPath(pageKey) :: tail =>
        val indexParam = s"${pageKey}Index"
        go(tail, indexParam :: s""""$pageKey"""" :: acc)
      case ChoicePath(pageKey, _) :: tail =>
        val choiceParam = s"$pageKey.toString"
        go(tail, choiceParam :: acc)
      case StringPath(pageKey) :: tail =>
        go(tail, s""""$pageKey"""" :: acc)
      case Root :: tail =>
        go(tail, "JsPath" :: acc)
    }

    go(path.paths)
  }

  private def unapplyTypeFor(journey: Journey, path: JourneyPath): String = {
    @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): String =
      paths match {
        case Nil =>
          if (acc.length == 1) acc.head
          else acc.reverse.mkString("(", ", ", ")")
        case IndexPath(_) :: tail =>
          go(tail, "Int" :: acc)
        case ChoicePath(pageKey, _) :: tail =>
          val choiceType = ModelFields.fieldType(journey.pages(pageKey).answerType)
          go(tail, choiceType :: acc)
        case _ :: tail =>
          go(tail, acc)
      }

    go(path.paths)
  }

  private def unapplyResultFor(journey: Journey, path: JourneyPath): String = {
    @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): String =
      paths match {
        case Nil =>
          if (acc.length == 1) acc.head
          else acc.reverse.mkString("(", ", ", ")")
        case IndexPath(pageKey) :: tail =>
          go(tail, s"${pageKey}Index" :: acc)
        case ChoicePath(pageKey, choice) :: tail =>
          val choiceType = ModelFields.fieldType(journey.pages(pageKey).answerType)
          go(tail, s"$choiceType.$choice" :: acc)
        case _ :: tail =>
          go(tail, acc)
      }

    go(path.paths)
  }

  private def jsPathNodesFor(path: JourneyPath): String = {
    @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): String =
      paths match {
        case Nil =>
          acc.reverse.mkString("", " :: ", " :: Nil")
        case IndexPath(pageKey) :: tail =>
          go(tail, s"IdxPathNode(${pageKey}Index)" :: s"""KeyPathNode("$pageKey")""" :: acc)
        case ChoicePath(_, choice) :: tail =>
          go(tail, s"""KeyPathNode("$choice")""" :: acc)
        case StringPath(pageKey) :: tail =>
          go(tail, s"""KeyPathNode("$pageKey")""" :: acc)
        case Root :: tail =>
          go(tail, acc)
      }

    go(path.paths)
  }

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
        overloads.map(applyMethod(capitalPageName, journey, _)).mkString(System.lineSeparator())
      val unapplyMethods =
        overloads.map(unapplyMethod(capitalPageName, journey, _)).mkString(System.lineSeparator())

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
