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
  private def listAnswerType(
    modelsPackage: QualifiedName,
    journey: Journey,
    journeyPage: JourneyPage
  ): FieldType = {
    def go(parts: List[JourneyPart]): Option[JourneyPart] = parts match {
      case (part @ DoWhilePart(choicePage, subJourney, _)) :: tail =>
        if (choicePage == journeyPage.pageKey) Some(part) else go(subJourney).orElse(go(tail))
      case SwitchCasePart(_, subJourneys, _) :: tail =>
        subJourneys.values.toList.flatMap(go).headOption.orElse(go(tail))
      case IfThenPart(_, subJourney, _) :: tail =>
        go(subJourney).orElse(go(tail))
      case SinglePagePart(_, _) :: tail =>
        go(tail)
      case Nil =>
        None
    }

    go(journey.journey)
      .collect { case DoWhilePart(_, subJourney, _) =>
        val subJourneyFields = subJourney.flatMap(ModelFields.forPart(modelsPackage, journey, _))
        if (subJourneyFields.length == 1) subJourneyFields.head._2
        else ClassType("play.api.libs.json.JsObject")
      }
      .getOrElse(
        throw new NoSuchElementException(
          s"Unable to find the list answer type for the add-another page ${journeyPage.pageKey}"
        )
      )
  }

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

  def applyMethod(pageName: String, journey: Journey, path: JourneyPath): String = {
    val params       = applyParams(journey, path)
    val paramsString = if (params.isEmpty) "" else params.mkString("(", ", ", ")")
    val jsPath       = jsPathFor(path)
    s"""|  def apply$paramsString: ${pageName}Page =
        |    new ${pageName}Page($jsPath)""".stripMargin
  }

  def render(basePackage: QualifiedName, journey: Journey, journeyPage: JourneyPage): String = {
    val modelsPackage   = basePackage / "models"
    val pagesPackage    = basePackage / "pages"
    val capitalPageName = pascalCase(journeyPage.pageKey)
    val overloads       = journey.pathsFor(journeyPage.pageKey)

    val answerType =
      if (overloads.exists(_.isIndex)) listAnswerType(modelsPackage, journey, journeyPage)
      else journey.pages(journeyPage.pageKey).answerType

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
         |import _root_.pages.* // TODO: Remove this once we have a better template
         |import play.api.libs.json.JsPath
         |$imports
         |
         |object ${capitalPageName}Page extends QuestionPage[$pageType] {
         |  override def path: JsPath = ${jsPathFor(overloads.head)}
         |  override def toString: String = "${journeyPage.pageKey}"
         |}
         |""".stripMargin
    } else {
      val applyMethods =
        overloads.map(applyMethod(capitalPageName, journey, _)).mkString(System.lineSeparator())

      s"""package ${basePackage / "pages"}
         |
         |import _root_.pages.* // TODO: Remove this once we have a better template
         |import play.api.libs.json.JsPath
         |$imports
         |
         |case class ${capitalPageName}Page private (override val path: JsPath) extends QuestionPage[$pageType] {
         |  override def toString: String = "${journeyPage.pageKey}"
         |}
         |
         |object ${capitalPageName}Page {
         |$applyMethods
         |}
         |""".stripMargin
    }
  }
}
