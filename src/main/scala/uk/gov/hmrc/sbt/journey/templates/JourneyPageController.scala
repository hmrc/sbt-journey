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

object JourneyPageController extends Template {
  private def initialiseAnswers(requiresData: Boolean, indent: Int) =
    if (requiresData) ""
    else s"$NL${" " * indent}.getOrElse(UserAnswers(request.userId))"

  def onPageLoadDeclFor(indexParams: String): String =
    s"  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent]"

  def onSubmitDeclFor(indexParams: String): String =
    s"  def onSubmit(${indexParams}mode: Mode): Action[AnyContent]"

  def actionFor(requiresData: Boolean) =
    if (requiresData) "(identify andThen getData andThen requireData)"
    else "(identify andThen getData)"

  def indexParamsFor(path: JourneyPath): String = {
    val paths = path.indexPaths
    if (paths.isEmpty) ""
    else paths.map(p => s"${p.pageKey}Index: Int").mkString("", ", ", ", ")
  }

  def pageParamsFor(paths: List[PathAtom]): String = {
    val pageParams = paths
      .flatMap {
        case IndexPath(pageKey) =>
          List(s"${pageKey}Index")
        case ChoicePath(pageKey, _) =>
          List(pageKey)
        case _ =>
          List.empty
      }

    if (pageParams.isEmpty) ""
    else pageParams.mkString("(", ", ", ")")
  }

  def fetchAnswerGeneratorsFor(path: JourneyPath): List[String] = {
    @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): List[String] = paths match {
      case ChoicePath(pageKey, _) :: tail =>
        // The page params for the prefix of this choice path should give
        // us all the params for the associated choice page
        val pageName   = pascalCase(pageKey)
        val pageParams = pageParamsFor(tail.reverse)
        val generator =
          s"${" " * 6}$pageKey <- userAnswers.get(${pageName}Page$pageParams)"
        go(tail, generator :: acc)
      case _ :: tail =>
        go(tail, acc)
      case Nil =>
        acc
    }

    // Process the path in reverse so that we can see the prefix of each path as "tail"
    go(path.paths.reverse)
  }

  def onPageLoadImplFor(
    pageClassName: String,
    path: JourneyPath,
    indexParams: String,
    pageParams: String,
    answerGenerators: List[String],
    requiresData: Boolean
  ): String = {
    val action = actionFor(requiresData)
    if (path.isIndex && answerGenerators.isEmpty) {
      // TODO: Decide whether to fill this based upon the existing answers
      //  Problem: we can't tell the difference between "No" to add another element and "not filled yet"
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val page = $pageClassName$pageParams
          |    Ok(view(form(), page.submitRoute(mode), mode))
          |  }""".stripMargin
    } else if (path.isIndex) {
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |    } yield Ok(view(form(), page.submitRoute(mode), mode))
          |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          |  }""".stripMargin
    } else if (answerGenerators.isEmpty) {
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val page = $pageClassName$pageParams
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val preparedForm = userAnswers
          |      .get(page)
          |      .map(form().fill)
          |      .getOrElse(form())
          |    Ok(view(preparedForm, page.submitRoute(mode), mode))
          |  }""".stripMargin
    } else {
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |      preparedForm = userAnswers.get(page)
          |        .map(form().fill)
          |        .getOrElse(form())
          |    } yield Ok(view(preparedForm, page.submitRoute(mode), mode))
          |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          |  }""".stripMargin
    }
  }

  def onSubmitImplFor(
    pageClassName: String,
    path: JourneyPath,
    indexParams: String,
    pageParams: String,
    answerGenerators: List[String],
    requiresData: Boolean
  ): String = {
    val action = actionFor(requiresData)
    if (path.isIndex && answerGenerators.isEmpty) {
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val page = $pageClassName$pageParams
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    form().bindFromRequest().fold(
          |      formWithErrors =>
          |        BadRequest(view(formWithErrors, page.submitRoute(mode), mode)),
          |      answer =>
          |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
          |    )
          |  }""".stripMargin
    } else if (path.isIndex) {
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |    } yield form().bindFromRequest().fold(
          |      formWithErrors =>
          |        BadRequest(view(formWithErrors, page.submitRoute(mode), mode)),
          |      answer =>
          |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
          |    )
          |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          |  }""".stripMargin
    } else if (answerGenerators.isEmpty)
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action.async { implicit request =>
          |    val page = $pageClassName$pageParams
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    form().bindFromRequest().fold(
          |      formWithErrors =>
          |        Future.successful(BadRequest(view(formWithErrors, page.submitRoute(mode), mode))),
          |      answer =>
          |        for {
          |          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
          |          _ <- sessionRepository.set(updatedAnswers)
          |        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
          |    )
          |  }""".stripMargin
    else
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action.async { implicit request =>
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |    } yield form().bindFromRequest().fold(
          |      formWithErrors =>
          |        Future.successful(BadRequest(view(formWithErrors, page.submitRoute(mode), mode))),
          |      answer =>
          |        for {
          |          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
          |          _ <- sessionRepository.set(updatedAnswers)
          |        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
          |    )
          |    result.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
          |  }""".stripMargin
  }

  def render(
    basePackage: QualifiedName,
    requiresData: Boolean,
    journey: Journey,
    pageName: String,
    journeyPage: JourneyPage
  ): String = {
    val capitalPageName = pascalCase(pageName)
    val withDefault     = journeyPage.withDefaultController
    val interfaceName   = s"${capitalPageName}BaseController"

    val defaultImplName  = s"Default${capitalPageName}Controller"
    val pageClassName    = s"${capitalPageName}Page"
    val formProviderName = QualifiedName(journeyPage.formProviderClass)

    val overloads = journey.pathsFor(pageName)

    val indexes = overloads.map(indexParamsFor)

    val onPageLoadDecls = indexes.map(onPageLoadDeclFor)
    val onSubmitDecls   = indexes.map(onSubmitDeclFor)

    val (onPageLoadImpls, onSubmitImpls) = overloads
      .zip(indexes)
      .map { case (path, indexParams) =>
        val pageParams       = pageParamsFor(path.paths)
        val answerGenerators = fetchAnswerGeneratorsFor(path)

        val onPageLoadImpl = onPageLoadImplFor(
          pageClassName,
          path,
          indexParams,
          pageParams,
          answerGenerators,
          requiresData
        )

        val onSubmitImpl = onSubmitImplFor(
          pageClassName,
          path,
          indexParams,
          pageParams,
          answerGenerators,
          requiresData
        )
        (onPageLoadImpl, onSubmitImpl)
      }
      .unzip

    val implementedBy =
      if (!withDefault) ""
      else s"@ImplementedBy(classOf[$defaultImplName])$NL"

    val defaultImpl =
      if (!withDefault) ""
      else
        s"""
           |@Singleton
           |class $defaultImplName @Inject() (
           |  identify: IdentifierAction,
           |  getData: DataRetrievalAction,
           |  requireData: DataRequiredAction,
           |  navigator: JourneyNavigator,
           |  sessionRepository: SessionRepository,
           |  form: ${formProviderName.parts.last},
           |  view: ${journeyPage.viewClass},
           |  override val controllerComponents: MessagesControllerComponents
           |)(implicit ec: ExecutionContext) extends $interfaceName {
           |
           |${onPageLoadImpls.mkString(NL * 2)}
           |
           |${onSubmitImpls.mkString(NL * 2)}
           |}
           |""".stripMargin

    s"""package ${basePackage / "controllers"}
       |
       |import controllers.actions.*  // ${basePackage / "controllers.actions.*"}
       |import controllers.routes // ${basePackage / "controllers.routes"}
       |import models.Mode // ${basePackage / "models.Mode"}
       |import models.UserAnswers // ${basePackage / "models.UserAnswers"}
       |import repositories.SessionRepository // ${basePackage / "repositories.SessionRepository"}
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
       |${implementedBy}trait $interfaceName extends FrontendBaseController with I18nSupport {
       |${onPageLoadDecls.mkString(NL)}
       |${onSubmitDecls.mkString(NL)}
       |}
       |$defaultImpl""".stripMargin
  }

}
