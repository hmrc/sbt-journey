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

  def onUpscanPageLoadDeclFor(indexParams: String): String =
    s"  def onPageLoad(${indexParams}mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]"

  def onSubmitDeclFor(indexParams: String): String =
    s"  def onSubmit(${indexParams}mode: Mode): Action[AnyContent]"

  def onUploadSuccessDeclFor(indexParams: String): String =
    s"  def onUploadSuccess(${indexParams}id: UUID, mode: Mode): Action[AnyContent]"

  def onUploadFailureDeclFor(indexParams: String): String =
    s"  def onUploadFailure(${indexParams}id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]"

  def actionFor(requiresData: Boolean) =
    if (requiresData) "(identify andThen getData andThen requireData)"
    else "(identify andThen getData)"

  def indexParamsFor(path: JourneyPath): String = {
    val paths = path.indexPaths
    if (paths.isEmpty) ""
    else paths.map(p => s"${p.pageKey}Index: Int").mkString("", ", ", ", ")
  }

  def indexParamNamesFor(path: JourneyPath): String = {
    val paths = path.indexPaths
    if (paths.isEmpty) ""
    else paths.map(p => s"${p.pageKey}Index").mkString("", ", ", ", ")
  }

  def submitRouteFor(controllerClassName: String, path: JourneyPath): String = {
    s"journeyRoutes.$controllerClassName.onSubmit(${indexParamNamesFor(path)}mode)"
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
    controllerClassName: String,
    pageClassName: String,
    path: JourneyPath,
    indexParams: String,
    pageParams: String,
    answerGenerators: List[String],
    requiresData: Boolean
  ): String = {
    val action      = actionFor(requiresData)
    val submitRoute = submitRouteFor(controllerClassName, path)
    if (path.isIndex && answerGenerators.isEmpty) {
      // TODO: Decide whether to fill this based upon the existing answers
      //  Problem: we can't tell the difference between "No" to add another element and "not filled yet"
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val submitRoute = $submitRoute
          |    val page = $pageClassName$pageParams
          |    Ok(view(form(), submitRoute, mode))
          |  }""".stripMargin
    } else if (path.isIndex) {
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val submitRoute = $submitRoute
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |    } yield Ok(view(form(), submitRoute, mode))
          |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          |  }""".stripMargin
    } else if (answerGenerators.isEmpty) {
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val submitRoute = $submitRoute
          |    val page = $pageClassName$pageParams
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val preparedForm = userAnswers
          |      .get(page)
          |      .map(form().fill)
          |      .getOrElse(form())
          |    Ok(view(preparedForm, submitRoute, mode))
          |  }""".stripMargin
    } else {
      s"""|  def onPageLoad(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val submitRoute = $submitRoute
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |      preparedForm = userAnswers.get(page)
          |        .map(form().fill)
          |        .getOrElse(form())
          |    } yield Ok(view(preparedForm, submitRoute, mode))
          |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          |  }""".stripMargin
    }
  }

  def onSubmitImplFor(
    controllerClassName: String,
    pageClassName: String,
    path: JourneyPath,
    indexParams: String,
    pageParams: String,
    answerGenerators: List[String],
    requiresData: Boolean
  ): String = {
    val action      = actionFor(requiresData)
    val submitRoute = submitRouteFor(controllerClassName, path)
    if (path.isIndex && answerGenerators.isEmpty) {
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val submitRoute = $submitRoute
          |    val page = $pageClassName$pageParams
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    form().bindFromRequest().fold(
          |      formWithErrors =>
          |        BadRequest(view(formWithErrors, submitRoute, mode)),
          |      answer =>
          |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
          |    )
          |  }""".stripMargin
    } else if (path.isIndex) {
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action { implicit request =>
          |    val submitRoute = $submitRoute
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |    } yield form().bindFromRequest().fold(
          |      formWithErrors =>
          |        BadRequest(view(formWithErrors, submitRoute, mode)),
          |      answer =>
          |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
          |    )
          |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          |  }""".stripMargin
    } else if (answerGenerators.isEmpty)
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action.async { implicit request =>
          |    val submitRoute = $submitRoute
          |    val page = $pageClassName$pageParams
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    form().bindFromRequest().fold(
          |      formWithErrors =>
          |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
          |      answer =>
          |        for {
          |          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
          |          _ <- sessionRepository.set(updatedAnswers)
          |        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
          |    )
          |  }""".stripMargin
    else
      s"""|  def onSubmit(${indexParams}mode: Mode): Action[AnyContent] = $action.async { implicit request =>
          |    val submitRoute = $submitRoute
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |    } yield form().bindFromRequest().fold(
          |      formWithErrors =>
          |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
          |      answer =>
          |        for {
          |          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
          |          _ <- sessionRepository.set(updatedAnswers)
          |        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
          |    )
          |    result.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
          |  }""".stripMargin
  }

  def onUpscanPageLoadImplFor(
    controllerClassName: String,
    indexParams: String,
    requiresData: Boolean
  ): String = {
    val action = actionFor(requiresData)
    s"""|  def onPageLoad(${indexParams}mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = $action.async { implicit request =>
        |    val uploadId = UploadId.next()
        |    for {
        |      initiateResponse <- upscanConnector.initiate(
        |        callbackUrl = upscanRoutes.UpscanNotificationBaseController.onNotificationReceived(uploadId.id),
        |        successRedirect = journeyRoutes.$controllerClassName.onUploadSuccess(${indexParams}uploadId.id, mode),
        |        errorRedirect = journeyRoutes.$controllerClassName.onUploadFailure(${indexParams}uploadId.id, mode, None, None, None)
        |      )
        |      uploadId <- fileUploadRepository.initiate(uploadId, request.userId, initiateResponse.reference)
        |      formTemplate = initiateResponse.uploadRequest
        |      preparedForm <- upscanReference.fold(Future.successful(form())) { ref =>
        |        val reference = UpscanReference(ref)
        |        fileUploadRepository.setRejected(request.userId, reference).map { _ =>
        |          val errorCode = upscanErrorCode.orNull
        |          val errorMessage = upscanErrorMessage.orNull
        |          logger.error(s"File upload with reference $$reference failed with error code $$errorCode: $$errorMessage")
        |          val uploadError = UploadError.fromErrorCode(errorCode)
        |          form().withError("file", uploadError.messageKey)
        |        }
        |      }
        |    } yield Ok(view(preparedForm, formTemplate, mode))
        |  }""".stripMargin
  }

  def onUploadSuccessImplFor(
    pageClassName: String,
    indexParams: String,
    pageParams: String,
    answerGenerators: List[String],
    requiresData: Boolean
  ): String = {
    val action = actionFor(requiresData)
    if (answerGenerators.isEmpty) {
      s"""|  def onUploadSuccess(${indexParams}id: UUID, mode: Mode): Action[AnyContent] = $action.async { implicit request =>
          |    val uploadId = UploadId(id)
          |    val page = $pageClassName$pageParams
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    for {
          |      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
          |      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
          |      _ <- sessionRepository.set(updatedAnswers)
          |    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
          |  }""".stripMargin
    } else {
      s"""|  def onUploadSuccess(${indexParams}id: UUID, mode: Mode): Action[AnyContent] = $action.async { implicit request =>
          |    val uploadId = UploadId(id)
          |    val userAnswers = request.userAnswers${initialiseAnswers(requiresData, 6)}
          |    val result = for {
          |${answerGenerators.mkString(NL)}
          |      page = $pageClassName$pageParams
          |    } yield for {
          |      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
          |      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
          |      _ <- sessionRepository.set(updatedAnswers)
          |    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
          |    result.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
          |  }""".stripMargin
    }
  }

  def onUploadFailureImplFor(
    controllerClassName: String,
    indexParams: String,
    indexParamNames: String,
    requiresData: Boolean
  ): String = {
    val action = actionFor(requiresData)
    s"""|  def onUploadFailure(${indexParams}id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = $action { implicit request =>
          |    Redirect(journeyRoutes.$controllerClassName.onPageLoad(${indexParamNames}mode, upscanReference, upscanErrorCode, upscanErrorMessage))
          |  }""".stripMargin
  }

  def journeyController(
    basePackage: QualifiedName,
    requiresData: Boolean,
    journey: Journey,
    pageName: String,
    journeyPage: JourneyPage
  ): String = {
    val capitalPageName = pascalCase(pageName)
    val withDefault     = journeyPage.withDefaultController
    val interfaceName   = s"${capitalPageName}BaseController"

    val defaultImplName   = s"Default${capitalPageName}Controller"
    val pageClassName     = s"${capitalPageName}Page"
    val formProviderClass = QualifiedName(journeyPage.formProviderClass)
    val viewClass         = QualifiedName(journeyPage.viewClass)

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
          interfaceName,
          pageClassName,
          path,
          indexParams,
          pageParams,
          answerGenerators,
          requiresData
        )

        val onSubmitImpl = onSubmitImplFor(
          interfaceName,
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
           |  form: ${formProviderClass.parts.last},
           |  view: ${viewClass.parts.last},
           |  override val controllerComponents: MessagesControllerComponents
           |)(using ExecutionContext) extends $interfaceName {
           |
           |${onPageLoadImpls.distinct.mkString(NL * 2)}
           |
           |${onSubmitImpls.distinct.mkString(NL * 2)}
           |}
           |""".stripMargin

    s"""package ${basePackage / "controllers"}
       |
       |import controllers.actions.*  // ${basePackage / "controllers.actions.*"}
       |import controllers.routes // ${basePackage / "controllers.routes"}
       |import models.Mode // ${basePackage / "models.Mode"}
       |import models.UserAnswers // ${basePackage / "models.UserAnswers"}
       |import repositories.SessionRepository // ${basePackage / "repositories.SessionRepository"}
       |import ${basePackage / "controllers"}.{routes as journeyRoutes}
       |import ${basePackage / "models.*"}
       |import ${journeyPage.formProviderClass}
       |import ${basePackage / "navigation.*"}
       |import ${basePackage / "pages.*"}
       |import ${journeyPage.viewClass}
       |
       |import play.api.i18n.I18nSupport
       |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
       |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
       |
       |import com.google.inject.ImplementedBy
       |import javax.inject.{Inject, Singleton}
       |import scala.concurrent.{ExecutionContext, Future}
       |
       |${implementedBy}trait $interfaceName extends FrontendBaseController, I18nSupport {
       |${onPageLoadDecls.distinct.mkString(NL)}
       |${onSubmitDecls.distinct.mkString(NL)}
       |}
       |$defaultImpl""".stripMargin
  }

  def testIndexParamsFor(path: JourneyPath): String = {
    val paths = path.indexPaths
    if (paths.isEmpty) ""
    else paths.map(_ => "0").mkString("", ", ", ", ")
  }

  def testPageParamsFor(pages: Map[String, JourneyPage], paths: List[PathAtom]): String = {
    val pageParams = paths
      .flatMap {
        case IndexPath(_) =>
          List("0")
        case ChoicePath(pageKey, _) =>
          List(pageKey)
        case _ =>
          List.empty
      }

    if (pageParams.isEmpty) ""
    else pageParams.mkString("(", ", ", ")")
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

  def answerSettersFor(pages: Map[String, JourneyPage], path: JourneyPath): List[String] = {
    val p = " " * 6
    @tailrec def go(paths: List[PathAtom], acc: List[String] = Nil): List[String] = paths match {
      case ChoicePath(pageKey, choice) :: tail =>
        // The page params for the prefix of this choice path should give
        // us all the params for the associated choice page
        val pageName   = pascalCase(pageKey)
        val pageParams = testPageParamsFor(pages, tail.reverse)
        val choiceType = ModelFields.fieldType(pages(pageKey).answerType)
        val generator =
          s"""${p}$pageKey = $choiceType.$choice
             |${p}answers <- answers.set(${pageName}Page$pageParams, $pageKey)""".stripMargin
        go(tail, generator :: acc)
      case _ :: tail =>
        go(tail, acc)
      case Nil =>
        acc
    }

    // Process the path in reverse so that we can see the prefix of each path as "tail"
    go(path.paths.reverse)
  }

  private def onPageLoadTestsFor(
    pages: Map[String, JourneyPage],
    controllerClassName: String,
    controllerImplName: String,
    viewClassName: String,
    page: JourneyPage,
    path: JourneyPath,
    indexParams: String,
    answerSetters: List[String],
    requiresData: Boolean
  ): String = {
    val userAnswers =
      if (answerSetters.isEmpty)
        """    val userAnswers = Success(UserAnswers("id"))"""
      else
        s"""    val userAnswers = for {
           |      answers <- Success(UserAnswers("id"))
           |${answerSetters.mkString(NL)}
           |    } yield answers""".stripMargin

    val answerType    = pages(page.pageKey).answerType
    val generatorType = ModelFields.fieldType(answerType)

    val okTest =
      s"""should "return a 200 OK response containing the view HTML when everything is successful" in {
         |    val view = mock[${viewClassName}]
         |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
         |$userAnswers
         |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
         |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode)
         |    val request = FakeRequest(route)
         |    val response = controller.onPageLoad(${indexParams}NormalMode)(request)
         |    status(response) shouldBe OK
         |    contentAsString(response) shouldBe mockViewResponse
         |    val submitRoute = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
         |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
         |  }""".stripMargin

    val pageName    = s"${pascalCase(page.pageKey)}Page"
    val pageParams  = testPageParamsFor(pages, path.paths)
    val pageSetters = if (answerSetters.isEmpty) "" else answerSetters.mkString("", NL, NL)
    val prefilledAnswers =
      s"""    val userAnswers = for {
         |      answers <- Success(UserAnswers("id"))
         |$pageSetters      answers <- answers.set($pageName$pageParams, answer)
         |    } yield answers""".stripMargin
    val prefilledTest =
      if (!path.isIndex)
        List(
          s"""should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
           |    val view = mock[${viewClassName}]
           |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
           |$prefilledAnswers
           |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
           |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode)
           |    val request = FakeRequest(route)
           |    val response = controller.onPageLoad(${indexParams}NormalMode)(request)
           |    status(response) shouldBe OK
           |    contentAsString(response) shouldBe mockViewResponse
           |    val submitRoute = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
           |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[$generatorType]])
           |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
           |    formCaptor.getValue().value shouldBe Some(answer)
           |  }""".stripMargin
        )
      else
        List.empty

    val noAnswersTest =
      if (requiresData)
        List(
          s"""should "redirect to journey recovery when the user's answers can't be retrieved" in {
           |    val controller = makeController(userAnswers = None)
           |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode)
           |    val request = FakeRequest(route)
           |    val response = controller.onPageLoad(${indexParams}NormalMode)(request)
           |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
           |    status(response) shouldBe SEE_OTHER
           |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
           |  }""".stripMargin
        )
      else
        List(
          s"""should "create an empty user answers object when the user's answers can't be retrieved" in {
           |    val view = mock[${viewClassName}]
           |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
           |    val controller = makeController(userAnswers = None, view = view)
           |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode)
           |    val request = FakeRequest(route)
           |    val response = controller.onPageLoad(${indexParams}NormalMode)(request)
           |    status(response) shouldBe OK
           |    contentAsString(response) shouldBe mockViewResponse
           |    val submitRoute = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
           |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
           |  }""".stripMargin
        )

    val missingSetters = answerSetters.drop(1)
    val missingAnswers =
      if (missingSetters.isEmpty)
        """    val userAnswers = Success(UserAnswers("id"))"""
      else
        s"""    val userAnswers = for {
           |      answers <- Try(UserAnswers("id"))
           |${missingSetters.mkString(NL)}
           |    } yield answers""".stripMargin

    val missingAnswerTest =
      if (answerSetters.nonEmpty)
        List(
          s"""should "redirect to journey recovery when a prerequisite answer is missing" in {
             |$missingAnswers
             |    val controller = makeController(userAnswers = userAnswers.toOption)
             |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode)
             |    val request = FakeRequest(route)
             |    val response = controller.onPageLoad(${indexParams}NormalMode)(request)
             |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
             |    status(response) shouldBe SEE_OTHER
             |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
             |  }""".stripMargin
        )
      else
        List.empty

    val exceptionTest =
      if (answerSetters.nonEmpty)
        List(
          s"""should "throw exceptions to the top level error handler" in {
           |$userAnswers
           |    val repository = mock[SessionRepository]
           |    when(repository.get(any())).thenReturn(Future.failed(new MongoException("Error")))
           |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
           |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode)
           |    val request = FakeRequest(route)
           |    recoverToSucceededIf[MongoException] {
           |      controller.onPageLoad(${indexParams}NormalMode)(request)
           |    }
           |  }""".stripMargin
        )
      else
        List.empty

    val descParams = testDescParams(path)

    (okTest :: prefilledTest ::: noAnswersTest ::: missingAnswerTest ::: exceptionTest).mkString(
      s"""$NL  "$controllerImplName.onPageLoad$descParams" """,
      s"${NL * 2}  it ",
      ""
    )
  }

  private def onSubmitTestsFor(
    pages: Map[String, JourneyPage],
    controllerClassName: String,
    controllerImplName: String,
    viewClassName: String,
    page: JourneyPage,
    path: JourneyPath,
    indexParams: String,
    answerSetters: List[String],
    requiresData: Boolean
  ): String = {
    val userAnswers =
      if (answerSetters.isEmpty)
        """    val userAnswers = Success(UserAnswers("id"))"""
      else
        s"""    val userAnswers = for {
           |      answers <- Try(UserAnswers("id"))
           |${answerSetters.mkString(NL)}
           |    } yield answers""".stripMargin

    val answerType    = pages(page.pageKey).answerType
    val generatorType = ModelFields.fieldType(answerType)

    val okTest =
      s"""should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
         |    val navigator = mock[JourneyNavigator]
         |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
         |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
         |    val repository = mock[SessionRepository]
         |    when(repository.set(any())).thenReturn(Future.successful(true))
         |$userAnswers
         |    val controller = makeController(
         |      userAnswers = userAnswers.toOption,
         |      navigator = navigator,
         |      repository = repository,
         |      formProvider = defaultFormProvider
         |    )
         |    val route = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
         |    val form = defaultFormProvider().fill(answer)
         |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
         |    val response = controller.onSubmit(${indexParams}NormalMode)(request)
         |    status(response) shouldBe SEE_OTHER
         |    redirectLocation(response) shouldBe Some(nextPage.path())
         |  }""".stripMargin

    val noAnswersTest =
      if (requiresData)
        List(
          s"""should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
           |    val controller = makeController(userAnswers = None)
           |    val route = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
           |    val form = defaultFormProvider().fill(answer)
           |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
           |    val response = controller.onSubmit(${indexParams}NormalMode)(request)
           |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
           |    status(response) shouldBe SEE_OTHER
           |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
           |  }""".stripMargin
        )
      else if (answerSetters.isEmpty)
        List(
          s"""should "create an empty user answers object when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
           |    val navigator = mock[JourneyNavigator]
           |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
           |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
           |    val repository = mock[SessionRepository]
           |    when(repository.set(any())).thenReturn(Future.successful(true))
           |    val controller = makeController(
           |      userAnswers = None,
           |      navigator = navigator,
           |      repository = repository,
           |      formProvider = defaultFormProvider
           |    )
           |    val route = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
           |    val form = defaultFormProvider().fill(answer)
           |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
           |    val response = controller.onSubmit(${indexParams}NormalMode)(request)
           |    status(response) shouldBe SEE_OTHER
           |    redirectLocation(response) shouldBe Some(nextPage.path())
           |  }""".stripMargin
        )
      else
        List.empty

    val missingSetters = answerSetters.drop(1)
    val missingAnswers =
      if (missingSetters.isEmpty)
        """    val userAnswers = Success(UserAnswers("id"))"""
      else
        s"""    val userAnswers = for {
         |      answers <- Try(UserAnswers("id"))
         |${missingSetters.mkString(NL)}
         |    } yield answers""".stripMargin

    val missingAnswerTest =
      if (answerSetters.nonEmpty)
        List(
          s"""should "redirect to journey recovery when a prerequisite answer is missing" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
             |$missingAnswers
             |    val controller = makeController(userAnswers = userAnswers.toOption)
             |    val route = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
             |    val form = defaultFormProvider().fill(answer)
             |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
             |    val response = controller.onSubmit(${indexParams}NormalMode)(request)
             |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
             |    status(response) shouldBe SEE_OTHER
             |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
             |  }""".stripMargin
        )
      else
        List.empty

    val badRequestTest =
      if (!page.answerType.canBeEmpty)
        List(
          s"""should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
           |    val view = mock[${viewClassName}]
           |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
           |$userAnswers
           |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
           |    val route = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
           |    val request = FakeRequest(route)
           |    val response = controller.onSubmit(${indexParams}NormalMode)(request)
           |    status(response) shouldBe BAD_REQUEST
           |    contentAsString(response) shouldBe mockViewResponse
           |    val submitRoute = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
           |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[$generatorType]])
           |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
           |    formCaptor.getValue().errors should not be(empty)
           |  }""".stripMargin
        )
      else
        List.empty

    val exceptionTest =
      List(
        s"""should "throw exceptions to the top level error handler" in {
         |$userAnswers
         |    val repository = mock[SessionRepository]
         |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
         |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
         |    val route = journeyRoutes.${controllerClassName}.onSubmit(${indexParams}NormalMode)
         |    val request = FakeRequest(route)
         |    recoverToSucceededIf[MongoException] {
         |      controller.onPageLoad(${indexParams}NormalMode)(request)
         |    }
         |  }""".stripMargin
      )

    val descParams = testDescParams(path)

    (okTest :: noAnswersTest ::: missingAnswerTest ::: badRequestTest ::: exceptionTest).mkString(
      s"""$NL  "$controllerImplName.onSubmit$descParams" """,
      s"${NL * 2}  it ",
      ""
    )
  }

  def journeyControllerSpec(
    basePackage: QualifiedName,
    requiresData: Boolean,
    journey: Journey,
    pageName: String,
    journeyPage: JourneyPage
  ): String = {
    val capitalPageName = pascalCase(pageName)
    val interfaceName   = s"${capitalPageName}BaseController"

    val defaultImplName       = s"Default${capitalPageName}Controller"
    val formProviderClass     = QualifiedName(journeyPage.formProviderClass)
    val formProviderClassName = formProviderClass.parts.last
    val viewClass             = QualifiedName(journeyPage.viewClass)
    val viewClassName         = viewClass.parts.last

    val overloads = journey.pathsFor(pageName)

    val indexes = overloads.map(testIndexParamsFor)

    val (onPageLoadTests, onSubmitTests) = overloads
      .zip(indexes)
      .map { case (path, indexParams) =>
        val answerSetters = answerSettersFor(journey.pages, path)

        val pageLoadTests = onPageLoadTestsFor(
          journey.pages,
          interfaceName,
          defaultImplName,
          viewClassName,
          journey.pages(pageName),
          path,
          indexParams,
          answerSetters,
          requiresData
        )

        val submitTests = onSubmitTestsFor(
          journey.pages,
          interfaceName,
          defaultImplName,
          viewClassName,
          journey.pages(pageName),
          path,
          indexParams,
          answerSetters,
          requiresData
        )

        (pageLoadTests, submitTests)
      }
      .unzip

    s"""package ${basePackage / "controllers"}
       |
       |import _root_.controllers.actions.* // ${basePackage / "controllers" / "actions" / "*"}
       |import _root_.controllers.routes // ${basePackage / "controllers" / "routes"}
       |import _root_.models.{NormalMode, UserAnswers} // ${basePackage / "models"}.{NormalMode,UserAnswers}
       |import _root_.generators.Generators // ${basePackage / "generators.Generators"}
       |import org.apache.pekko.actor.ActorSystem
       |import org.mockito.ArgumentCaptor
       |import org.mockito.ArgumentMatchers.{any, eq as eqTo}
       |import org.mockito.Mockito.{verify,when}
       |import org.mongodb.scala.MongoException
       |import org.scalacheck.{Arbitrary,Gen}
       |import org.scalatest.{RecoverMethods,TryValues}
       |import org.scalatest.flatspec.AnyFlatSpec
       |import org.scalatest.matchers.should.Matchers
       |import org.scalatestplus.mockito.MockitoSugar
       |import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
       |import play.api.data.Form
       |import play.api.i18n.Messages
       |import play.api.test.FakeRequest
       |import play.api.test.Helpers.*
       |import play.twirl.api.HtmlFormat.raw
       |import repositories.SessionRepository // ${basePackage / "repositories" / "SessionRepository"}
       |import ${basePackage / "controllers"}.routes as journeyRoutes
       |import ${journeyPage.formProviderClass}
       |import ${basePackage / "forms" / "*"}
       |import ${basePackage / "models" / "*"}
       |import ${basePackage / "navigation" / "JourneyNavigator"}
       |import ${basePackage / "pages" / "*"}
       |import ${journeyPage.viewClass}
       |
       |import scala.concurrent.{ExecutionContext,Future}
       |import scala.util.{Success,Try}
       |
       |class ${defaultImplName}Spec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
       |  given system: ActorSystem = ActorSystem("test")
       |  given ExecutionContext    = system.dispatcher
       |  given Messages            = stubMessages()
       |
       |  private val defaultFormProvider = new Default${capitalPageName}FormProvider()
       |  private val mockViewResponse    = "<html>Hello</html>"
       |
       |  private def makeController(
       |    userAnswers: Option[UserAnswers],
       |    navigator: JourneyNavigator = mock[JourneyNavigator],
       |    repository: SessionRepository = mock[SessionRepository],
       |    formProvider: ${formProviderClassName} = mock[${formProviderClassName}],
       |    view: ${viewClassName} = mock[${viewClassName}]
       |  ) = new $defaultImplName(
       |    new FakeIdentifierAction(stubPlayBodyParsers),
       |    new FakeDataRetrievalAction(userAnswers),
       |    new DataRequiredActionImpl(),
       |    navigator,
       |    repository,
       |    formProvider,
       |    view,
       |    stubMessagesControllerComponents()
       |  )
       |${onPageLoadTests.mkString(NL * 2)}
       |${onSubmitTests.mkString(NL * 2)}
       |}
       |""".stripMargin
  }

  def fileUploadController(
    basePackage: QualifiedName,
    requiresData: Boolean,
    journey: Journey,
    pageName: String,
    journeyPage: JourneyPage
  ): String = {
    val capitalPageName = pascalCase(pageName)
    val withDefault     = journeyPage.withDefaultController
    val interfaceName   = s"${capitalPageName}BaseController"

    val defaultImplName   = s"Default${capitalPageName}Controller"
    val pageClassName     = s"${capitalPageName}Page"
    val formProviderClass = QualifiedName(journeyPage.formProviderClass)
    val viewClass         = QualifiedName(journeyPage.viewClass)

    val overloads = journey.pathsFor(pageName)

    val indexes = overloads.map(indexParamsFor)

    val onPageLoadDecls      = indexes.map(onUpscanPageLoadDeclFor)
    val onUploadSuccessDecls = indexes.map(onUploadSuccessDeclFor)
    val onUploadFailureDecls = indexes.map(onUploadFailureDeclFor)

    val implParams = overloads.zip(indexes).map { case (path, indexParams) =>
      val pageParams       = pageParamsFor(path.paths)
      val answerGenerators = fetchAnswerGeneratorsFor(path)
      (path, indexParams, pageParams, answerGenerators)
    }

    val onPageLoadImpls = implParams.map { case (_, indexParams, _, _) =>
      onUpscanPageLoadImplFor(interfaceName, indexParams, requiresData)
    }

    val onUploadSuccessImpls = implParams.map {
      case (_, indexParams, pageParams, answerGenerators) =>
        onUploadSuccessImplFor(
          pageClassName,
          indexParams,
          pageParams,
          answerGenerators,
          requiresData
        )
    }

    val onUploadFailureImpls = implParams.map { case (path, indexParams, _, _) =>
      val indexParamNames = indexParamNamesFor(path)
      onUploadFailureImplFor(
        interfaceName,
        indexParams,
        indexParamNames,
        requiresData
      )
    }

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
           |  upscanConnector: UpscanConnector,
           |  fileUploadRepository: FileUploadRepository,
           |  form: ${formProviderClass.parts.last},
           |  view: ${viewClass.parts.last},
           |  override val controllerComponents: MessagesControllerComponents
           |)(using ExecutionContext) extends $interfaceName {
           |
           |${onPageLoadImpls.distinct.mkString(NL * 2)}
           |
           |${onUploadSuccessImpls.distinct.mkString(NL * 2)}
           |
           |${onUploadFailureImpls.distinct.mkString(NL * 2)}
           |}
           |""".stripMargin

    s"""package ${basePackage / "controllers"}
       |
       |import controllers.actions.*  // ${basePackage / "controllers.actions.*"}
       |import controllers.routes // ${basePackage / "controllers.routes"}
       |import models.Mode // ${basePackage / "models.Mode"}
       |import models.UserAnswers // ${basePackage / "models.UserAnswers"}
       |import repositories.SessionRepository // ${basePackage / "repositories.SessionRepository"}
       |import ${basePackage / "controllers"}.{routes as journeyRoutes}
       |import ${basePackage / "controllers" / "upscan"}.{routes as upscanRoutes}
       |import ${basePackage / "connectors" / "UpscanConnector"}
       |import ${basePackage / "models.*"}
       |import ${basePackage / "models.upscan.*"}
       |import ${journeyPage.formProviderClass}
       |import ${basePackage / "navigation.*"}
       |import ${basePackage / "repositories" / "FileUploadRepository"}
       |import ${basePackage / "pages.*"}
       |import ${journeyPage.viewClass}
       |
       |import play.api.Logging
       |import play.api.i18n.I18nSupport
       |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
       |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
       |
       |import com.google.inject.ImplementedBy
       |import java.util.UUID
       |import javax.inject.{Inject, Singleton}
       |import scala.concurrent.{ExecutionContext, Future}
       |
       |${implementedBy}trait $interfaceName extends FrontendBaseController, I18nSupport, Logging {
       |${onPageLoadDecls.distinct.mkString(NL)}
       |${onUploadSuccessDecls.distinct.mkString(NL)}
       |${onUploadFailureDecls.distinct.mkString(NL)}
       |}
       |$defaultImpl""".stripMargin
  }

  def onUpscanPageLoadTestsFor(
    controllerClassName: String,
    controllerImplName: String,
    viewClassName: String,
    indexParams: String,
    path: JourneyPath,
    requiresData: Boolean
  ): String = {
    val okTest =
      s"""should "return a 200 OK response containing the view HTML when everything is successful" in {
         |    val upscanConnector = mock[UpscanConnector]
         |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
         |    val fileUploadRepository = mock[FileUploadRepository]
         |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
         |    val view = mock[${viewClassName}]
         |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
         |    val controller = makeController(
         |      userAnswers = Some(UserAnswers("id")),
         |      upscanConnector = upscanConnector,
         |      fileUploadRepository = fileUploadRepository,
         |      formProvider = defaultFormProvider,
         |      view = view
         |    )
         |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode, None, None, None)
         |    val request = FakeRequest(route)
         |    val response = controller.onPageLoad(${indexParams}NormalMode, None, None, None)(request)
         |    status(response) shouldBe OK
         |    contentAsString(response) shouldBe mockViewResponse
         |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
         |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
         |    formCaptor.getValue().errors should be(empty)
         |  }""".stripMargin

    val noAnswersTest =
      if (requiresData)
        s"""should "redirect to journey recovery when the user's answers can't be retrieved" in {
           |    val controller = makeController(userAnswers = None)
           |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode, None, None, None)
           |    val request = FakeRequest(route)
           |    val response = controller.onPageLoad(${indexParams}NormalMode, None, None, None)(request)
           |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
           |    status(response) shouldBe SEE_OTHER
           |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
           |  }""".stripMargin
      else
        s"""should "create an empty user answers object when the user's answers can't be retrieved" in {
           |    val upscanConnector = mock[UpscanConnector]
           |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
           |    val fileUploadRepository = mock[FileUploadRepository]
           |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
           |    val view = mock[${viewClassName}]
           |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
           |    val controller = makeController(
           |      userAnswers = None,
           |      upscanConnector = upscanConnector,
           |      fileUploadRepository = fileUploadRepository,
           |      formProvider = defaultFormProvider,
           |      view = view
           |    )
           |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode, None, None, None)
           |    val request = FakeRequest(route)
           |    val response = controller.onPageLoad(${indexParams}NormalMode, None, None, None)(request)
           |    status(response) shouldBe OK
           |    contentAsString(response) shouldBe mockViewResponse
           |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
           |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
           |    formCaptor.getValue().errors should be(empty)
           |  }""".stripMargin

    val uploadErrorTest =
      s"""should "render the upload form with errors when the user is redirected to the page with error detail parameters" in {
       |    val upscanConnector = mock[UpscanConnector]
       |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
       |    val fileUploadRepository = mock[FileUploadRepository]
       |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
       |    when(fileUploadRepository.setRejected(any(), UpscanReference(any()))).thenReturn(Future.unit)
       |    val view = mock[${viewClassName}]
       |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
       |    val controller = makeController(
       |      userAnswers = Some(UserAnswers("id")),
       |      upscanConnector = upscanConnector,
       |      fileUploadRepository = fileUploadRepository,
       |      formProvider = defaultFormProvider,
       |      view = view
       |    )
       |    val key = Some(fileReference)
       |    val errorCode = Some("EntityTooSmall")
       |    val errorMessage = Some("we were instructed to reject this upload")
       |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode, key, errorCode, errorMessage)
       |    val request = FakeRequest(route)
       |    val response = controller.onPageLoad(${indexParams}NormalMode, key, errorCode, errorMessage)(request)
       |    status(response) shouldBe OK
       |    contentAsString(response) shouldBe mockViewResponse
       |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
       |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
       |    formCaptor.getValue().errors should contain(FormError("file", "upload.error.fileTooSmall"))
       |  }""".stripMargin

    val exceptionTest =
      s"""should "throw exceptions to the top level error handler" in {
       |    val upscanConnector = mock[UpscanConnector]
       |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
       |    val controller = makeController(userAnswers = Some(UserAnswers("id")), upscanConnector = upscanConnector)
       |    val route = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode, None, None, None)
       |    val request = FakeRequest(route)
       |    recoverToSucceededIf[UpstreamErrorResponse] {
       |      controller.onPageLoad(${indexParams}NormalMode, None, None, None)(request)
       |    }
       |  }""".stripMargin

    val descParams = testDescParams(path)

    List(okTest, noAnswersTest, uploadErrorTest, exceptionTest).mkString(
      s"""$NL  "$controllerImplName.onPageLoad$descParams" """,
      s"${NL * 2}  it ",
      ""
    )
  }

  def onUploadSuccessTestsFor(
    pages: Map[String, JourneyPage],
    controllerClassName: String,
    controllerImplName: String,
    indexParams: String,
    page: JourneyPage,
    path: JourneyPath,
    answerSetters: List[String],
    requiresData: Boolean
  ): String = {
    val userAnswers =
      if (answerSetters.isEmpty)
        """    val userAnswers = Success(UserAnswers("id"))"""
      else
        s"""    val userAnswers = for {
           |      answers <- Try(UserAnswers("id"))
           |${answerSetters.mkString(NL)}
           |    } yield answers""".stripMargin

    val answerType    = pages(page.pageKey).answerType
    val generatorType = ModelFields.fieldType(answerType)

    val okTest =
      s"""should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
         |    val navigator = mock[JourneyNavigator]
         |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
         |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
         |    val repository = mock[SessionRepository]
         |    when(repository.set(any())).thenReturn(Future.successful(true))
         |    val fileUploadRepository = mock[FileUploadRepository]
         |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
         |$userAnswers
         |    val controller = makeController(
         |      userAnswers = userAnswers.toOption,
         |      navigator = navigator,
         |      repository = repository,
         |      fileUploadRepository = fileUploadRepository,
         |      formProvider = defaultFormProvider
         |    )
         |    val uploadId = UUID.randomUUID()
         |    val route = journeyRoutes.${controllerClassName}.onUploadSuccess(${indexParams}uploadId, NormalMode)
         |    val request = FakeRequest(route)
         |    val response = controller.onUploadSuccess(${indexParams}uploadId, NormalMode)(request)
         |    status(response) shouldBe SEE_OTHER
         |    redirectLocation(response) shouldBe Some(nextPage.path())
         |  }""".stripMargin

    val noAnswersTest =
      if (requiresData)
        List(
          s"""should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
             |    val controller = makeController(userAnswers = None)
             |    val uploadId = UUID.randomUUID()
             |    val route = journeyRoutes.${controllerClassName}.onUploadSuccess(${indexParams}uploadId, NormalMode)
             |    val request = FakeRequest(route)
             |    val response = controller.onUploadSuccess(${indexParams}uploadId, NormalMode)(request)
             |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
             |    status(response) shouldBe SEE_OTHER
             |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
             |  }""".stripMargin
        )
      else if (answerSetters.isEmpty)
        List(
          s"""should "create an empty user answers object when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: $generatorType) =>
             |    val navigator = mock[JourneyNavigator]
             |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
             |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
             |    val repository = mock[SessionRepository]
             |    when(repository.set(any())).thenReturn(Future.successful(true))
             |    val fileUploadRepository = mock[FileUploadRepository]
             |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
             |    val controller = makeController(
             |      userAnswers = None,
             |      navigator = navigator,
             |      repository = repository,
             |      fileUploadRepository = fileUploadRepository,
             |      formProvider = defaultFormProvider
             |    )
             |    val uploadId = UUID.randomUUID()
             |    val route = journeyRoutes.${controllerClassName}.onUploadSuccess(${indexParams}uploadId, NormalMode)
             |    val request = FakeRequest(route)
             |    val response = controller.onUploadSuccess(${indexParams}uploadId, NormalMode)(request)
             |    status(response) shouldBe SEE_OTHER
             |    redirectLocation(response) shouldBe Some(nextPage.path())
             |  }""".stripMargin
        )
      else
        List.empty

    val missingSetters = answerSetters.drop(1)
    val missingAnswers =
      if (missingSetters.isEmpty)
        """    val userAnswers = Success(UserAnswers("id"))"""
      else
        s"""    val userAnswers = for {
           |      answers <- Try(UserAnswers("id"))
           |${missingSetters.mkString(NL)}
           |    } yield answers""".stripMargin

    val missingAnswerTest =
      if (answerSetters.nonEmpty)
        List(
          s"""should "redirect to journey recovery when a prerequisite answer is missing" in {
             |$missingAnswers
             |    val controller = makeController(userAnswers = userAnswers.toOption)
             |    val uploadId = UUID.randomUUID()
             |    val route = journeyRoutes.${controllerClassName}.onUploadSuccess(${indexParams}uploadId, NormalMode)
             |    val request = FakeRequest(route)
             |    val response = controller.onUploadSuccess(${indexParams}uploadId, NormalMode)(request)
             |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
             |    status(response) shouldBe SEE_OTHER
             |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
             |  }""".stripMargin
        )
      else
        List.empty

    val exceptionTest =
      List(
        s"""should "throw exceptions to the top level error handler" in {
         |    val fileUploadRepository = mock[FileUploadRepository]
         |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.failed(new MongoException("Error")))
         |$userAnswers
         |    val controller = makeController(userAnswers = userAnswers.toOption, fileUploadRepository = fileUploadRepository)
         |    val uploadId = UUID.randomUUID()
         |    val route = journeyRoutes.${controllerClassName}.onUploadSuccess(${indexParams}uploadId, NormalMode)
         |    val request = FakeRequest(route)
         |    recoverToSucceededIf[MongoException] {
         |      controller.onUploadSuccess(${indexParams}uploadId, NormalMode)(request)
         |    }
         |  }""".stripMargin
      )

    val descParams = testDescParams(path)

    (okTest :: noAnswersTest ::: missingAnswerTest ::: exceptionTest).mkString(
      s"""$NL  "$controllerImplName.onUploadSuccess$descParams" """,
      s"${NL * 2}  it ",
      ""
    )
  }

  def onUploadFailureTestsFor(
    controllerClassName: String,
    controllerImplName: String,
    indexParams: String,
    path: JourneyPath,
    requiresData: Boolean
  ): String = {
    val okTest =
      s"""should "redirect to onPageLoad preserving the query string" in {
       |    val controller = makeController(userAnswers = Some(UserAnswers("id")))
       |    val uploadId = UUID.randomUUID()
       |    val key = Some(fileReference)
       |    val errorCode = Some("EntityTooSmall")
       |    val errorMessage = Some("we were instructed to reject this upload")
       |    val route = journeyRoutes.${controllerClassName}.onUploadFailure(${indexParams}uploadId, NormalMode, key, errorCode, errorMessage)
       |    val request = FakeRequest(route)
       |    val response = controller.onUploadFailure(${indexParams}uploadId, NormalMode, key, errorCode, errorMessage)(request)
       |    val nextPage = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode, key, errorCode, errorMessage)
       |    status(response) shouldBe SEE_OTHER
       |    redirectLocation(response) shouldBe Some(nextPage.path())
       |  }""".stripMargin

    val noAnswersTest =
      if (requiresData)
        s"""should "redirect to journey recovery when the user's answers can't be retrieved" in {
           |    val controller = makeController(userAnswers = None)
           |    val uploadId = UUID.randomUUID()
           |    val key = Some(fileReference)
           |    val errorCode = Some("EntityTooSmall")
           |    val errorMessage = Some("we were instructed to reject this upload")
           |    val route = journeyRoutes.${controllerClassName}.onUploadFailure(${indexParams}uploadId, NormalMode, key, errorCode, errorMessage)
           |    val request = FakeRequest(route)
           |    val response = controller.onUploadFailure(${indexParams}uploadId, NormalMode, key, errorCode, errorMessage)(request)
           |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
           |    status(response) shouldBe SEE_OTHER
           |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
           |  }""".stripMargin
      else
        s"""should "create an empty user answers object when the user's answers can't be retrieved" in {
         |    val controller = makeController(userAnswers = None)
         |    val uploadId = UUID.randomUUID()
         |    val key = Some(fileReference)
         |    val errorCode = Some("EntityTooSmall")
         |    val errorMessage = Some("we were instructed to reject this upload")
         |    val route = journeyRoutes.${controllerClassName}.onUploadFailure(${indexParams}uploadId, NormalMode, key, errorCode, errorMessage)
         |    val request = FakeRequest(route)
         |    val response = controller.onUploadFailure(${indexParams}uploadId, NormalMode, key, errorCode, errorMessage)(request)
         |    val nextPage = journeyRoutes.${controllerClassName}.onPageLoad(${indexParams}NormalMode, key, errorCode, errorMessage)
         |    status(response) shouldBe SEE_OTHER
         |    redirectLocation(response) shouldBe Some(nextPage.path())
         |  }""".stripMargin

    val descParams = testDescParams(path)

    List(okTest, noAnswersTest).mkString(
      s"""$NL  "$controllerImplName.onUploadFailure$descParams" """,
      s"${NL * 2}  it ",
      ""
    )
  }

  def fileUploadControllerSpec(
    basePackage: QualifiedName,
    requiresData: Boolean,
    journey: Journey,
    pageName: String,
    journeyPage: JourneyPage
  ): String = {
    val capitalPageName = pascalCase(pageName)
    val interfaceName   = s"${capitalPageName}BaseController"

    val defaultImplName       = s"Default${capitalPageName}Controller"
    val formProviderClass     = QualifiedName(journeyPage.formProviderClass)
    val formProviderClassName = formProviderClass.parts.last
    val viewClassName         = QualifiedName(journeyPage.viewClass)

    val overloads = journey.pathsFor(pageName)

    val indexes = overloads.map(testIndexParamsFor)

    val implParams = overloads.zip(indexes).map { case (path, indexParams) =>
      val pageParams    = pageParamsFor(path.paths)
      val answerSetters = answerSettersFor(journey.pages, path)
      (path, indexParams, pageParams, answerSetters)
    }

    val onPageLoadTests = implParams.map { case (path, indexParams, _, _) =>
      onUpscanPageLoadTestsFor(
        interfaceName,
        defaultImplName,
        viewClassName.parts.last,
        indexParams,
        path,
        requiresData
      )
    }

    val onUploadSuccessTests = implParams.map {
      case (path, indexParams, pageParams, answerSetters) =>
        onUploadSuccessTestsFor(
          journey.pages,
          interfaceName,
          defaultImplName,
          indexParams,
          journeyPage,
          path,
          answerSetters,
          requiresData
        )
    }

    val onUploadFailureTests = implParams.map { case (path, indexParams, _, _) =>
      onUploadFailureTestsFor(
        interfaceName,
        defaultImplName,
        indexParams,
        path,
        requiresData
      )
    }

    s"""package ${basePackage / "controllers"}
       |
       |import _root_.controllers.actions.* // ${basePackage / "controllers" / "actions" / "*"}
       |import _root_.controllers.routes // ${basePackage / "controllers" / "routes"}
       |import _root_.models.{NormalMode, UserAnswers} // ${basePackage / "models"}.{NormalMode,UserAnswers}
       |import _root_.generators.Generators // ${basePackage / "generators.Generators"}
       |import org.apache.pekko.actor.ActorSystem
       |import org.mockito.ArgumentCaptor
       |import org.mockito.ArgumentMatchers.{any, eq as eqTo}
       |import org.mockito.Mockito.{verify,when}
       |import org.mongodb.scala.MongoException
       |import org.scalacheck.{Arbitrary,Gen}
       |import org.scalatest.{RecoverMethods,TryValues}
       |import org.scalatest.flatspec.AnyFlatSpec
       |import org.scalatest.matchers.should.Matchers
       |import org.scalatestplus.mockito.MockitoSugar
       |import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
       |import play.api.data.{Form,FormError}
       |import play.api.i18n.Messages
       |import play.api.test.FakeRequest
       |import play.api.test.Helpers.*
       |import play.twirl.api.HtmlFormat.raw
       |import repositories.SessionRepository // ${basePackage / "repositories" / "SessionRepository"}
       |import uk.gov.hmrc.http.UpstreamErrorResponse
       |import ${basePackage / "connectors" / "UpscanConnector"}
       |import ${basePackage / "controllers"}.routes as journeyRoutes
       |import ${journeyPage.formProviderClass}
       |import ${basePackage / "forms" / "*"}
       |import ${basePackage / "models" / "*"}
       |import ${basePackage / "models" / "upscan" / "*"}
       |import ${basePackage / "navigation" / "JourneyNavigator"}
       |import ${basePackage / "pages" / "*"}
       |import ${basePackage / "repositories" / "FileUploadRepository"}
       |import ${journeyPage.viewClass}
       |
       |import java.time.{Instant,ZoneOffset}
       |import java.time.format.DateTimeFormatter
       |import java.util.UUID
       |import scala.concurrent.{ExecutionContext,Future}
       |import scala.util.{Success,Try}
       |
       |class ${defaultImplName}Spec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
       |  given system: ActorSystem = ActorSystem("test")
       |  given ExecutionContext    = system.dispatcher
       |  given Messages            = stubMessages()
       |
       |  private val defaultFormProvider = new Default${capitalPageName}FormProvider()
       |  private val mockViewResponse    = "<html>Hello</html>"
       |  private val fileReference       = UUID.randomUUID().toString
       |  private val responseTime        = Instant.now().atZone(ZoneOffset.UTC)
       |  private val formatter           = DateTimeFormatter.ISO_ZONED_DATE_TIME
       |  private val shortFormatter      = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
       |
       |  private val initiateResponse = UpscanInitiateResponse(
       |    reference = UpscanReference(fileReference),
       |    uploadRequest = UpscanFormTemplate(
       |      href = "http://localhost:9570/upscan/upload-proxy",
       |      fields = Map(
       |        "success_action_redirect" -> s"http://localhost:9000/sbt-journey-test/",
       |        "x-amz-credential"                    -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
       |        "x-amz-meta-upscan-initiate-response" -> formatter.format(responseTime),
       |        "x-amz-meta-original-filename"        -> "$${filename}",
       |        "x-amz-algorithm"                     -> "AWS4-HMAC-SHA256",
       |        "x-amz-signature"                     -> "xxxx",
       |        "error_action_redirect"   -> "http://localhost:9000/sbt-journey-test/",
       |        "x-amz-meta-session-id"   -> UUID.randomUUID().toString,
       |        "x-amz-meta-callback-url" -> "http://localhost:9000/sbt-journey-test/",
       |        "x-amz-date"              -> shortFormatter.format(responseTime),
       |        "x-amz-meta-upscan-initiate-received" -> formatter.format(responseTime),
       |        "x-amz-meta-request-id"               -> UUID.randomUUID().toString,
       |        "key"                                 -> fileReference,
       |        "acl"                                 -> "private",
       |        "x-amz-meta-consuming-service"        -> "sbt-journey-test",
       |        "policy" -> "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMCwxMDQ4NTc2MDBdXX0="
       |      )
       |    )
       |  )
       |
       |  private def makeController(
       |    userAnswers: Option[UserAnswers],
       |    navigator: JourneyNavigator = mock[JourneyNavigator],
       |    repository: SessionRepository = mock[SessionRepository],
       |    upscanConnector: UpscanConnector = mock[UpscanConnector],
       |    fileUploadRepository: FileUploadRepository = mock[FileUploadRepository],
       |    formProvider: ${formProviderClassName} = mock[${formProviderClassName}],
       |    view: ${viewClassName} = mock[${viewClassName}]
       |  ) = new $defaultImplName(
       |    new FakeIdentifierAction(stubPlayBodyParsers),
       |    new FakeDataRetrievalAction(userAnswers),
       |    new DataRequiredActionImpl(),
       |    navigator,
       |    repository,
       |    upscanConnector,
       |    fileUploadRepository,
       |    formProvider,
       |    view,
       |    stubMessagesControllerComponents()
       |  )
       |${onPageLoadTests.mkString(NL * 2)}
       |${onUploadSuccessTests.mkString(NL * 2)}
       |${onUploadFailureTests.mkString(NL * 2)}
       |}
       |""".stripMargin
  }

}
