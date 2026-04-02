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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.sbt.journey.models.*
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.{kebabCase, pascalCase}

class JourneyPageControllerSpec extends AnyFlatSpec with Matchers {

  val basePackage = QualifiedName("uk.gov.hmrc.sbtjourneytest")

  def journeyPage(pageKey: String, answerType: FieldType, withDefaultController: Boolean = true) =
    JourneyPage(
      pageKey,
      s"$pageKey.title",
      s"$pageKey.heading",
      s"/${kebabCase(pageKey)}",
      s"/change-${kebabCase(pageKey)}",
      (basePackage / "controllers" / s"${pascalCase(pageKey)}BaseController").toString,
      (basePackage / "forms" / s"${pascalCase(pageKey)}BaseFormProvider").toString,
      s"views.html.${pascalCase(pageKey)}View",
      withDefaultController,
      withDefaultFormProvider = true,
      answerType
    )

  "JourneyPageController" should "generate a controller for a top-level journey page that doesn't require data" in {
    val serviceName = journeyPage("serviceName", FieldType.STRING)

    val journey = Journey(
      Map(serviceName.pageKey -> serviceName),
      List(SinglePagePart(serviceName.pageKey, None))
    )

    JourneyPageController.render(
      basePackage,
      requiresData = false,
      journey,
      "serviceName",
      serviceName
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultServiceNameController])
        |trait ServiceNameBaseController extends FrontendBaseController with I18nSupport {
        |  def onPageLoad(mode: Mode): Action[AnyContent]
        |  def onSubmit(mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultServiceNameController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: ServiceNameBaseFormProvider,
        |  view: views.html.ServiceNameView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends ServiceNameBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |      .getOrElse(UserAnswers(request.userId))
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |      .getOrElse(UserAnswers(request.userId))
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, page.submitRoute(mode), mode))),
        |      answer =>
        |        for {
        |          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
        |          _ <- sessionRepository.set(updatedAnswers)
        |        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
        |    )
        |  }
        |}
        |""".stripMargin
  }

  "JourneyPageController" should "generate a controller for a top-level journey page that does require data" in {
    val serviceName = journeyPage("serviceName", FieldType.STRING)

    val journey = Journey(
      Map(serviceName.pageKey -> serviceName),
      List(SinglePagePart(serviceName.pageKey, None))
    )

    JourneyPageController.render(
      basePackage,
      requiresData = true,
      journey,
      "serviceName",
      serviceName
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultServiceNameController])
        |trait ServiceNameBaseController extends FrontendBaseController with I18nSupport {
        |  def onPageLoad(mode: Mode): Action[AnyContent]
        |  def onSubmit(mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultServiceNameController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: ServiceNameBaseFormProvider,
        |  view: views.html.ServiceNameView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends ServiceNameBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, page.submitRoute(mode), mode))),
        |      answer =>
        |        for {
        |          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
        |          _ <- sessionRepository.set(updatedAnswers)
        |        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
        |    )
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller without a default implementation if requested" in {
    val serviceName = journeyPage("serviceName", FieldType.STRING, withDefaultController = false)

    val journey = Journey(
      Map(serviceName.pageKey -> serviceName),
      List(SinglePagePart(serviceName.pageKey, None))
    )

    JourneyPageController.render(
      basePackage,
      requiresData = false,
      journey,
      "serviceName",
      serviceName
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |trait ServiceNameBaseController extends FrontendBaseController with I18nSupport {
        |  def onPageLoad(mode: Mode): Action[AnyContent]
        |  def onSubmit(mode: Mode): Action[AnyContent]
        |}
        |""".stripMargin
  }

  it should "generate a controller for a top-level if-then choice page" in {
    val addATaxRegime =
      journeyPage("addATaxRegime", ClassType(basePackage / "Choice"))
    val taxRegime =
      journeyPage("taxRegime", FieldType.STRING)

    val ifThenPart = IfThenPart(
      addATaxRegime.pageKey,
      List(SinglePagePart(taxRegime.pageKey, None)),
      None
    )

    val journey = Journey(
      pages = Map(
        addATaxRegime.pageKey -> addATaxRegime,
        taxRegime.pageKey     -> taxRegime
      ),
      journey = List(ifThenPart)
    )

    JourneyPageController.render(
      basePackage,
      requiresData = true,
      journey,
      "addATaxRegime",
      addATaxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultAddATaxRegimeController])
        |trait AddATaxRegimeBaseController extends FrontendBaseController with I18nSupport {
        |  def onPageLoad(mode: Mode): Action[AnyContent]
        |  def onSubmit(mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultAddATaxRegimeController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: AddATaxRegimeBaseFormProvider,
        |  view: views.html.AddATaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends AddATaxRegimeBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = AddATaxRegimePage
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val page = AddATaxRegimePage
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, page.submitRoute(mode), mode))),
        |      answer =>
        |        for {
        |          updatedAnswers <- Future.fromTry(userAnswers.set(page, answer))
        |          _ <- sessionRepository.set(updatedAnswers)
        |        } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, answer))
        |    )
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a top-level switch-case choice page" in {}

  it should "generate a controller for a top-level do-while choice page" in {}
}
