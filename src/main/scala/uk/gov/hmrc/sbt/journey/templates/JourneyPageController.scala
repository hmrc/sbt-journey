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

import scala.annotation.tailrec

object JourneyPageController {
  def render(
    basePackage: QualifiedName,
    requiresData: Boolean,
    pageName: String,
    journeyPage: JourneyPage,
    journey: Journey
  ): String = {
    val capitalPageName = pascalCase(pageName)
    val interfaceName   = s"${capitalPageName}BaseController"

    val defaultImplName  = s"Default${capitalPageName}Controller"
    val pageClassName    = s"${capitalPageName}Page"
    val formProviderName = QualifiedName(journeyPage.formProviderClass)

    val action =
      if (requiresData) "(identify andThen getData andThen requireData)"
      else "(identify andThen getData)"

    val overloads = journey.pathsFor(pageName)

    def initialiseAnswers(indent: Int) =
      if (requiresData) ""
      else s"\n|${" " * indent}.getOrElse(UserAnswers(request.userId))"

    def onPageLoadDeclFor(indexParam: String): String = {
      s"  def onPageLoad(${indexParam}mode: Mode): Action[AnyContent]"
    }

    def onSubmitDeclFor(indexParam: String): String = {
      s"  def onSubmit(${indexParam}mode: Mode): Action[AnyContent]"
    }

    def indexParamsFor(path: JourneyPath): String = {
      val paths = path.indexPaths
      if (paths.isEmpty) ""
      else paths.map(p => s"${p.pageKey}Index: Int").mkString("", ", ", ", ")
    }

    def pageParamsFor(paths: List[PathAtom]): String = {
      @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): String = paths match {
        case Nil =>
          if (acc.isEmpty) ""
          else acc.reverse.mkString("(", ", ", ")")
        case IndexPath(pageKey) :: tail =>
          val indexParam = s"${pageKey}Index"
          go(tail, indexParam :: acc)
        case ChoicePath(pageKey, _) :: tail =>
          go(tail, pageKey :: acc)
        case _ :: tail =>
          go(tail, acc)
      }

      go(paths)
    }

    def fetchAnswerGeneratorsFor(path: JourneyPath): List[String] = {
      @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): List[String] = paths match {
        case Nil => acc.reverse
        case ChoicePath(pageKey, _) :: tail =>
          val pageName   = pascalCase(pageKey)
          val pageParams = pageParamsFor(tail.reverse)
          val generator =
            s"${" " * 6}$pageKey <- request.userAnswers.get(${pageName}Page$pageParams)"
          go(tail, generator :: acc)
        case _ :: tail =>
          go(tail, acc)
      }

      go(path.paths.reverse)
    }

    def onPageLoadImplFor(
      path: JourneyPath,
      indexParams: String,
      pageParams: String,
      fetchAnswerGenerators: List[String]
    ): String = {
      if (path.isIndex) {
        // TODO: Decide whether to fill this based upon the existing answers
        //  Problem: we can't tell the difference between "No" to add another element and "not filled yet"
        s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
            |    val page = $pageClassName$pageParams
            |    Ok(view(form(), page.submitRoute(mode), mode))
            |  }""".stripMargin
      } else if (fetchAnswerGenerators.isEmpty) {
        s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
            |    val page = $pageClassName$pageParams
            |    val preparedForm = request.userAnswers${initialiseAnswers(6)}
            |      .get(page)
            |      .map(form().fill)
            |      .getOrElse(form())
            |
            |    Ok(view(preparedForm, page.submitRoute(mode), mode))
            |  }""".stripMargin
      } else {
        s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
            |    ???
            |  }""".stripMargin
      }
    }

    def onSubmitImplFor(
      path: JourneyPath,
      indexParams: String,
      pageParams: String,
      fetchAnswerGenerators: List[String]
    ): String = {
      if (path.isIndex && fetchAnswerGenerators.isEmpty) {
        s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
            |    val page = $pageClassName$pageParams
            |    form().bindFromRequest().fold(
            |      formWithErrors =>
            |        BadRequest(view(formWithErrors, page.submitRoute(mode), mode)),
            |      answer =>
            |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
            |    )
            |  }
            |""".stripMargin
      } else if (path.isIndex) {
        s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action.async { implicit request =>
            |    ???
            |  }
            |""".stripMargin
      } else if (fetchAnswerGenerators.isEmpty)
        s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action.async { implicit request =>
            |    val page = $pageClassName$pageParams
            |    form().bindFromRequest().fold(
            |      formWithErrors =>
            |        Future.successful(BadRequest(view(formWithErrors, page.submitRoute(mode), mode))),
            |      answer => Future.fromTry {
            |        request
            |          .userAnswers${initialiseAnswers(10)}
            |          .set(page, answer)
            |          .map { updatedAnswers =>
            |            Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
            |          }
            |      }
            |    )
            |  }
            |""".stripMargin
      else
        s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action.async { implicit request =>
            |    ???
            |  }
            |""".stripMargin
    }

    val indexes = overloads.map(indexParamsFor)

    val onPageLoadDecls = indexes.map(onPageLoadDeclFor)
    val onSubmitDecls   = indexes.map(onSubmitDeclFor)

    val (onPageLoadImpls, onSubmitImpls) = overloads
      .zip(indexes)
      .map { case (path, indexParams) =>
        val pageParams       = pageParamsFor(path.paths)
        val answerGenerators = fetchAnswerGeneratorsFor(path)
        val onPageLoadImpl   = onPageLoadImplFor(path, indexParams, pageParams, answerGenerators)
        val onSubmitImpl     = onSubmitImplFor(path, indexParams, pageParams, answerGenerators)
        (onPageLoadImpl, onSubmitImpl)
      }
      .unzip

    s"""package ${basePackage / "controllers"}
       |
       |import controllers.actions.*  // ${basePackage / "controllers.actions.*"}
       |import models.Mode // ${basePackage / "models.Mode"}
       |import models.UserAnswers
       |import ${basePackage / "models.*"}
       |import ${basePackage / "forms.*"}
       |import ${basePackage / "navigation.*"}
       |import ${basePackage / "pages.*"}
       |
       |import play.api.i18n.I18nSupport
       |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
       |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
       |
       |import com.google.inject.ImplementedBy
       |import javax.inject.{Inject, Singleton}
       |import scala.concurrent.{ExecutionContext, Future}
       |
       |@ImplementedBy(classOf[$defaultImplName])
       |trait $interfaceName extends FrontendBaseController with I18nSupport {
       |${onPageLoadDecls.mkString(System.lineSeparator())}
       |${onSubmitDecls.mkString(System.lineSeparator())}
       |}
       |
       |@Singleton
       |class $defaultImplName @Inject() (
       |  identify: IdentifierAction,
       |  getData: DataRetrievalAction,
       |  requireData: DataRequiredAction,
       |  navigator: JourneyNavigator,
       |  form: ${formProviderName.parts.last},
       |  view: ${journeyPage.viewClass},
       |  override val controllerComponents: MessagesControllerComponents
       |)(implicit ec: ExecutionContext) extends $interfaceName {
       |
       |${onPageLoadImpls.mkString(System.lineSeparator() * 2)}
       |
       |${onSubmitImpls.mkString(System.lineSeparator() * 2)}
       |}
       |""".stripMargin
  }

}
