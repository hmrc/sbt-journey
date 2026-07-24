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

  "JourneyPageController.journeyController" should "generate a controller for a top-level journey page that doesn't require data" in {
    val serviceName = journeyPage("serviceName", FieldType.STRING)

    val journey = Journey(
      Map(serviceName.pageKey -> serviceName),
      List(SinglePagePart(serviceName.pageKey, None))
    )

    JourneyPageController.journeyController(
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
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.ServiceNameBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.ServiceNameView
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
        |trait ServiceNameBaseController extends FrontendBaseController, I18nSupport {
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
        |  view: ServiceNameView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends ServiceNameBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(mode)
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |      .getOrElse(UserAnswers(request.userId))
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, submitRoute, mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(mode)
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |      .getOrElse(UserAnswers(request.userId))
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
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

  it should "generate a controller for a top-level journey page that does require data" in {
    val serviceName = journeyPage("serviceName", FieldType.STRING)

    val journey = Journey(
      Map(serviceName.pageKey -> serviceName),
      List(SinglePagePart(serviceName.pageKey, None))
    )

    JourneyPageController.journeyController(
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
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.ServiceNameBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.ServiceNameView
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
        |trait ServiceNameBaseController extends FrontendBaseController, I18nSupport {
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
        |  view: ServiceNameView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends ServiceNameBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(mode)
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, submitRoute, mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(mode)
        |    val page = ServiceNamePage
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
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

    JourneyPageController.journeyController(
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
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.ServiceNameBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.ServiceNameView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |trait ServiceNameBaseController extends FrontendBaseController, I18nSupport {
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

    JourneyPageController.journeyController(
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
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.AddATaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddATaxRegimeView
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
        |trait AddATaxRegimeBaseController extends FrontendBaseController, I18nSupport {
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
        |  view: AddATaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends AddATaxRegimeBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AddATaxRegimeBaseController.onSubmit(mode)
        |    val page = AddATaxRegimePage
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, submitRoute, mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.AddATaxRegimeBaseController.onSubmit(mode)
        |    val page = AddATaxRegimePage
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
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

  it should "generate a controller for a journey page nested within an if-then subjourney" in {
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

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "taxRegime",
      taxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.TaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.TaxRegimeView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultTaxRegimeController])
        |trait TaxRegimeBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(mode: Mode): Action[AnyContent]
        |  def onSubmit(mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultTaxRegimeController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: TaxRegimeBaseFormProvider,
        |  view: TaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends TaxRegimeBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime)
        |      preparedForm = userAnswers.get(page)
        |        .map(form().fill)
        |        .getOrElse(form())
        |    } yield Ok(view(preparedForm, submitRoute, mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime)
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
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a top-level switch-case choice page" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val journey = Journey(
      pages = Map(
        whichTaxRegime.pageKey -> whichTaxRegime,
        saInfo.pageKey         -> saInfo,
        vatInfo.pageKey        -> vatInfo
      ),
      journey = List(
        SwitchCasePart(
          whichTaxRegime.pageKey,
          Map(
            "SA"  -> List(SinglePagePart(saInfo.pageKey, None)),
            "VAT" -> List(SinglePagePart(vatInfo.pageKey, None))
          ),
          None
        )
      )
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "whichTaxRegime",
      whichTaxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.WhichTaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.WhichTaxRegimeView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultWhichTaxRegimeController])
        |trait WhichTaxRegimeBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(mode: Mode): Action[AnyContent]
        |  def onSubmit(mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultWhichTaxRegimeController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: WhichTaxRegimeBaseFormProvider,
        |  view: WhichTaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends WhichTaxRegimeBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(mode)
        |    val page = WhichTaxRegimePage
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, submitRoute, mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(mode)
        |    val page = WhichTaxRegimePage
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
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

  it should "generate a controller for a journey page nested within a switch-case subjourney" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val journey = Journey(
      pages = Map(
        whichTaxRegime.pageKey -> whichTaxRegime,
        saInfo.pageKey         -> saInfo,
        vatInfo.pageKey        -> vatInfo
      ),
      journey = List(
        SwitchCasePart(
          whichTaxRegime.pageKey,
          Map(
            "SA"  -> List(SinglePagePart(saInfo.pageKey, None)),
            "VAT" -> List(SinglePagePart(vatInfo.pageKey, None))
          ),
          None
        )
      )
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "vatInfo",
      vatInfo
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.VatInfoBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.VatInfoView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultVatInfoController])
        |trait VatInfoBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(mode: Mode): Action[AnyContent]
        |  def onSubmit(mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultVatInfoController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: VatInfoBaseFormProvider,
        |  view: VatInfoView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends VatInfoBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.VatInfoBaseController.onSubmit(mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      whichTaxRegime <- userAnswers.get(WhichTaxRegimePage)
        |      page = VatInfoPage(whichTaxRegime)
        |      preparedForm = userAnswers.get(page)
        |        .map(form().fill)
        |        .getOrElse(form())
        |    } yield Ok(view(preparedForm, submitRoute, mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.VatInfoBaseController.onSubmit(mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      whichTaxRegime <- userAnswers.get(WhichTaxRegimePage)
        |      page = VatInfoPage(whichTaxRegime)
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
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a do-while choice page nested within a switch-case subjourney" in {
    val addATaxRegime       = journeyPage("addATaxRegime", FieldType.BOOLEAN)
    val addAnotherTaxRegime = journeyPage("addAnotherTaxRegime", FieldType.BOOLEAN)
    val taxRegime = journeyPage("taxRegime", ClassType(basePackage / "models" / "TaxRegime"))

    val journey = Journey(
      pages = Map(
        addATaxRegime.pageKey       -> addATaxRegime,
        addAnotherTaxRegime.pageKey -> addAnotherTaxRegime,
        taxRegime.pageKey           -> taxRegime
      ),
      journey = List(
        IfThenPart(
          addATaxRegime.pageKey,
          List(
            DoWhilePart(
              addAnotherTaxRegime.pageKey,
              List(SinglePagePart(taxRegime.pageKey, None)),
              "taxRegimes"
            )
          ),
          None
        )
      )
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "addAnotherTaxRegime",
      addAnotherTaxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.AddAnotherTaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddAnotherTaxRegimeView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultAddAnotherTaxRegimeController])
        |trait AddAnotherTaxRegimeBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent]
        |  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultAddAnotherTaxRegimeController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: AddAnotherTaxRegimeBaseFormProvider,
        |  view: AddAnotherTaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends AddAnotherTaxRegimeBaseController {
        |
        |  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(taxRegimesIndex, mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = AddAnotherTaxRegimePage(addATaxRegime, taxRegimesIndex)
        |    } yield Ok(view(form(), submitRoute, mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(taxRegimesIndex, mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = AddAnotherTaxRegimePage(addATaxRegime, taxRegimesIndex)
        |    } yield form().bindFromRequest().fold(
        |      formWithErrors =>
        |        BadRequest(view(formWithErrors, submitRoute, mode)),
        |      answer =>
        |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
        |    )
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a journey page nested within a switch-case subjourney and a do-while subjourney" in {
    val addATaxRegime       = journeyPage("addATaxRegime", FieldType.BOOLEAN)
    val addAnotherTaxRegime = journeyPage("addAnotherTaxRegime", FieldType.BOOLEAN)
    val taxRegime = journeyPage("taxRegime", ClassType(basePackage / "models" / "TaxRegime"))

    val journey = Journey(
      pages = Map(
        addATaxRegime.pageKey       -> addATaxRegime,
        addAnotherTaxRegime.pageKey -> addAnotherTaxRegime,
        taxRegime.pageKey           -> taxRegime
      ),
      journey = List(
        IfThenPart(
          addATaxRegime.pageKey,
          List(
            DoWhilePart(
              addAnotherTaxRegime.pageKey,
              List(SinglePagePart(taxRegime.pageKey, None)),
              "taxRegimes"
            )
          ),
          None
        )
      )
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "taxRegime",
      taxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.TaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.TaxRegimeView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultTaxRegimeController])
        |trait TaxRegimeBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent]
        |  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultTaxRegimeController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: TaxRegimeBaseFormProvider,
        |  view: TaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends TaxRegimeBaseController {
        |
        |  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(taxRegimesIndex, mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime, taxRegimesIndex)
        |      preparedForm = userAnswers.get(page)
        |        .map(form().fill)
        |        .getOrElse(form())
        |    } yield Ok(view(preparedForm, submitRoute, mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(taxRegimesIndex, mode)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime, taxRegimesIndex)
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
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a top-level do-while choice page" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val journey = Journey(
      pages = Map(
        "auditEvent"           -> auditEvent,
        "addAnotherAuditEvent" -> addAnotherAuditEvent
      ),
      journey = List(
        DoWhilePart(
          "addAnotherAuditEvent",
          List(SinglePagePart("auditEvent", None)),
          "auditEvents"
        )
      )
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "addAnotherAuditEvent",
      addAnotherAuditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.AddAnotherAuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddAnotherAuditEventView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultAddAnotherAuditEventController])
        |trait AddAnotherAuditEventBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |  def onSubmit(auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultAddAnotherAuditEventController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: AddAnotherAuditEventBaseFormProvider,
        |  view: AddAnotherAuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends AddAnotherAuditEventBaseController {
        |
        |  def onPageLoad(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(auditEventsIndex, mode)
        |    val page = AddAnotherAuditEventPage(auditEventsIndex)
        |    Ok(view(form(), submitRoute, mode))
        |  }
        |
        |  def onSubmit(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(auditEventsIndex, mode)
        |    val page = AddAnotherAuditEventPage(auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        BadRequest(view(formWithErrors, submitRoute, mode)),
        |      answer =>
        |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
        |    )
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a journey page nested within a do-while subjourney" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val journey = Journey(
      pages = Map(
        "auditEvent"           -> auditEvent,
        "addAnotherAuditEvent" -> addAnotherAuditEvent
      ),
      journey = List(
        DoWhilePart(
          "addAnotherAuditEvent",
          List(SinglePagePart("auditEvent", None)),
          "auditEvents"
        )
      )
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "auditEvent",
      auditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.AuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AuditEventView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultAuditEventController])
        |trait AuditEventBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |  def onSubmit(auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultAuditEventController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: AuditEventBaseFormProvider,
        |  view: AuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends AuditEventBaseController {
        |
        |  def onPageLoad(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(auditEventsIndex, mode)
        |    val page = AuditEventPage(auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, submitRoute, mode))
        |  }
        |
        |  def onSubmit(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(auditEventsIndex, mode)
        |    val page = AuditEventPage(auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
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

  it should "generate a controller for a choice page nested within two do-while subjourneys" in {
    val auditSource           = journeyPage("auditSource", FieldType.STRING)
    val auditEvent            = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)
    val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val innerDoWhilePart = DoWhilePart(
      addAnotherAuditEvent.pageKey,
      List(SinglePagePart(auditEvent.pageKey, None)),
      "auditEvents"
    )

    val outerDoWhilePart = DoWhilePart(
      addAnotherAuditSource.pageKey,
      List(SinglePagePart(auditSource.pageKey, None), innerDoWhilePart),
      "auditSources"
    )

    val journey = Journey(
      pages = Map(
        auditSource.pageKey           -> auditSource,
        addAnotherAuditSource.pageKey -> addAnotherAuditSource,
        auditEvent.pageKey            -> auditEvent,
        addAnotherAuditEvent.pageKey  -> addAnotherAuditEvent
      ),
      journey = List(outerDoWhilePart)
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "addAnotherAuditEvent",
      addAnotherAuditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.AddAnotherAuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddAnotherAuditEventView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultAddAnotherAuditEventController])
        |trait AddAnotherAuditEventBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultAddAnotherAuditEventController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: AddAnotherAuditEventBaseFormProvider,
        |  view: AddAnotherAuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends AddAnotherAuditEventBaseController {
        |
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
        |    val page = AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex)
        |    Ok(view(form(), submitRoute, mode))
        |  }
        |
        |  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
        |    val page = AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        BadRequest(view(formWithErrors, submitRoute, mode)),
        |      answer =>
        |        Redirect(navigator.nextPage(page, mode, request.userAnswers, answer))
        |    )
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a journey page nested within two do-while subjourneys" in {
    val auditSource           = journeyPage("auditSource", FieldType.STRING)
    val auditEvent            = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)
    val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val innerDoWhilePart = DoWhilePart(
      addAnotherAuditEvent.pageKey,
      List(SinglePagePart(auditEvent.pageKey, None)),
      "auditEvents"
    )

    val outerDoWhilePart = DoWhilePart(
      addAnotherAuditSource.pageKey,
      List(SinglePagePart(auditSource.pageKey, None), innerDoWhilePart),
      "auditSources"
    )

    val journey = Journey(
      pages = Map(
        auditSource.pageKey           -> auditSource,
        addAnotherAuditSource.pageKey -> addAnotherAuditSource,
        auditEvent.pageKey            -> auditEvent,
        addAnotherAuditEvent.pageKey  -> addAnotherAuditEvent
      ),
      journey = List(outerDoWhilePart)
    )

    JourneyPageController.journeyController(
      basePackage,
      requiresData = true,
      journey,
      "auditEvent",
      auditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.forms.AuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AuditEventView
        |
        |import play.api.i18n.I18nSupport
        |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
        |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
        |
        |import com.google.inject.ImplementedBy
        |import javax.inject.{Inject, Singleton}
        |import scala.concurrent.{ExecutionContext, Future}
        |
        |@ImplementedBy(classOf[DefaultAuditEventController])
        |trait AuditEventBaseController extends FrontendBaseController, I18nSupport {
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultAuditEventController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  form: AuditEventBaseFormProvider,
        |  view: AuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends AuditEventBaseController {
        |
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
        |    val page = AuditEventPage(auditSourcesIndex, auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, submitRoute, mode))
        |  }
        |
        |  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(auditSourcesIndex, auditEventsIndex, mode)
        |    val page = AuditEventPage(auditSourcesIndex, auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        Future.successful(BadRequest(view(formWithErrors, submitRoute, mode))),
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

  "JourneyPageController.journeyControllerSpec" should "generate a controller spec for a top-level journey page that doesn't require data" in {
    val serviceName = journeyPage("serviceName", FieldType.STRING)

    val journey = Journey(
      Map(serviceName.pageKey -> serviceName),
      List(SinglePagePart(serviceName.pageKey, None))
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = false,
      journey,
      "serviceName",
      serviceName
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.ServiceNameBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.ServiceNameView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultServiceNameControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultServiceNameFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: ServiceNameBaseFormProvider = mock[ServiceNameBaseFormProvider],
        |    view: ServiceNameView = mock[ServiceNameView]
        |  ) = new DefaultServiceNameController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultServiceNameController.onPageLoad" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[ServiceNameView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.ServiceNameBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val view = mock[ServiceNameView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      answers <- answers.set(ServiceNamePage, answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.ServiceNameBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "create an empty user answers object when the user's answers can't be retrieved" in {
        |    val view = mock[ServiceNameView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val controller = makeController(userAnswers = None, view = view)
        |    val route = journeyRoutes.ServiceNameBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  "DefaultServiceNameController.onSubmit" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "create an empty user answers object when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: String) =>
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
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[ServiceNameView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a top-level journey page that does require data" in {
    val serviceName = journeyPage("serviceName", FieldType.STRING)

    val journey = Journey(
      Map(serviceName.pageKey -> serviceName),
      List(SinglePagePart(serviceName.pageKey, None))
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "serviceName",
      serviceName
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.ServiceNameBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.ServiceNameView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultServiceNameControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultServiceNameFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: ServiceNameBaseFormProvider = mock[ServiceNameBaseFormProvider],
        |    view: ServiceNameView = mock[ServiceNameView]
        |  ) = new DefaultServiceNameController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultServiceNameController.onPageLoad" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[ServiceNameView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.ServiceNameBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val view = mock[ServiceNameView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      answers <- answers.set(ServiceNamePage, answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.ServiceNameBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.ServiceNameBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  "DefaultServiceNameController.onSubmit" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[ServiceNameView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.ServiceNameBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a top-level if-then choice page" in {
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

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "addATaxRegime",
      addATaxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.AddATaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddATaxRegimeView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultAddATaxRegimeControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultAddATaxRegimeFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: AddATaxRegimeBaseFormProvider = mock[AddATaxRegimeBaseFormProvider],
        |    view: AddATaxRegimeView = mock[AddATaxRegimeView]
        |  ) = new DefaultAddATaxRegimeController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultAddATaxRegimeController.onPageLoad" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[AddATaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.AddATaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddATaxRegimeBaseController.onSubmit(NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: Choice) =>
        |    val view = mock[AddATaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      answers <- answers.set(AddATaxRegimePage, answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AddATaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddATaxRegimeBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Choice]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddATaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  "DefaultAddATaxRegimeController.onSubmit" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: Choice) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.AddATaxRegimeBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: Choice) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddATaxRegimeBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[AddATaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AddATaxRegimeBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddATaxRegimeBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Choice]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.AddATaxRegimeBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a journey page nested within an if-then subjourney" in {
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

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "taxRegime",
      taxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.TaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.TaxRegimeView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultTaxRegimeControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultTaxRegimeFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: TaxRegimeBaseFormProvider = mock[TaxRegimeBaseFormProvider],
        |    view: TaxRegimeView = mock[TaxRegimeView]
        |  ) = new DefaultTaxRegimeController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultTaxRegimeController.onPageLoad(Yes)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[TaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Choice.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val view = mock[TaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Choice.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |      answers <- answers.set(TaxRegimePage(addATaxRegime), answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Choice.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.get(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultTaxRegimeController.onSubmit(Yes)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Choice.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[TaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Choice.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Choice.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a top-level switch-case choice page" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val journey = Journey(
      pages = Map(
        whichTaxRegime.pageKey -> whichTaxRegime,
        saInfo.pageKey         -> saInfo,
        vatInfo.pageKey        -> vatInfo
      ),
      journey = List(
        SwitchCasePart(
          whichTaxRegime.pageKey,
          Map(
            "SA"  -> List(SinglePagePart(saInfo.pageKey, None)),
            "VAT" -> List(SinglePagePart(vatInfo.pageKey, None))
          ),
          None
        )
      )
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "whichTaxRegime",
      whichTaxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.WhichTaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.WhichTaxRegimeView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultWhichTaxRegimeControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultWhichTaxRegimeFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: WhichTaxRegimeBaseFormProvider = mock[WhichTaxRegimeBaseFormProvider],
        |    view: WhichTaxRegimeView = mock[WhichTaxRegimeView]
        |  ) = new DefaultWhichTaxRegimeController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultWhichTaxRegimeController.onPageLoad" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[WhichTaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.WhichTaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
        |    val view = mock[WhichTaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      answers <- answers.set(WhichTaxRegimePage, answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.WhichTaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[TaxRegime]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.WhichTaxRegimeBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  "DefaultWhichTaxRegimeController.onSubmit" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[WhichTaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[TaxRegime]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.WhichTaxRegimeBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a journey page nested within a switch-case subjourney" in {
    val whichTaxRegime =
      journeyPage("whichTaxRegime", ClassType(basePackage / "models" / "TaxRegime"))
    val vatInfo =
      journeyPage("vatInfo", FieldType.STRING)
    val saInfo =
      journeyPage("saInfo", FieldType.STRING)

    val journey = Journey(
      pages = Map(
        whichTaxRegime.pageKey -> whichTaxRegime,
        saInfo.pageKey         -> saInfo,
        vatInfo.pageKey        -> vatInfo
      ),
      journey = List(
        SwitchCasePart(
          whichTaxRegime.pageKey,
          Map(
            "SA"  -> List(SinglePagePart(saInfo.pageKey, None)),
            "VAT" -> List(SinglePagePart(vatInfo.pageKey, None))
          ),
          None
        )
      )
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "vatInfo",
      vatInfo
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.VatInfoBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.VatInfoView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultVatInfoControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultVatInfoFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: VatInfoBaseFormProvider = mock[VatInfoBaseFormProvider],
        |    view: VatInfoView = mock[VatInfoView]
        |  ) = new DefaultVatInfoController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultVatInfoController.onPageLoad(VAT)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[VatInfoView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      whichTaxRegime = TaxRegime.VAT
        |      answers <- answers.set(WhichTaxRegimePage, whichTaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.VatInfoBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val view = mock[VatInfoView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      whichTaxRegime = TaxRegime.VAT
        |      answers <- answers.set(WhichTaxRegimePage, whichTaxRegime)
        |      answers <- answers.set(VatInfoPage(whichTaxRegime), answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.VatInfoBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.VatInfoBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.VatInfoBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      whichTaxRegime = TaxRegime.VAT
        |      answers <- answers.set(WhichTaxRegimePage, whichTaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.get(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.VatInfoBaseController.onPageLoad(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultVatInfoController.onSubmit(VAT)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      whichTaxRegime = TaxRegime.VAT
        |      answers <- answers.set(WhichTaxRegimePage, whichTaxRegime)
        |    } yield answers
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[VatInfoView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      whichTaxRegime = TaxRegime.VAT
        |      answers <- answers.set(WhichTaxRegimePage, whichTaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      whichTaxRegime = TaxRegime.VAT
        |      answers <- answers.set(WhichTaxRegimePage, whichTaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.VatInfoBaseController.onSubmit(NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a do-while choice page nested within a switch-case subjourney" in {
    val addATaxRegime       = journeyPage("addATaxRegime", FieldType.BOOLEAN)
    val addAnotherTaxRegime = journeyPage("addAnotherTaxRegime", FieldType.BOOLEAN)
    val taxRegime = journeyPage("taxRegime", ClassType(basePackage / "models" / "TaxRegime"))

    val journey = Journey(
      pages = Map(
        addATaxRegime.pageKey       -> addATaxRegime,
        addAnotherTaxRegime.pageKey -> addAnotherTaxRegime,
        taxRegime.pageKey           -> taxRegime
      ),
      journey = List(
        IfThenPart(
          addATaxRegime.pageKey,
          List(
            DoWhilePart(
              addAnotherTaxRegime.pageKey,
              List(SinglePagePart(taxRegime.pageKey, None)),
              "taxRegimes"
            )
          ),
          None
        )
      )
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "addAnotherTaxRegime",
      addAnotherTaxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.AddAnotherTaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddAnotherTaxRegimeView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultAddAnotherTaxRegimeControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultAddAnotherTaxRegimeFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: AddAnotherTaxRegimeBaseFormProvider = mock[AddAnotherTaxRegimeBaseFormProvider],
        |    view: AddAnotherTaxRegimeView = mock[AddAnotherTaxRegimeView]
        |  ) = new DefaultAddAnotherTaxRegimeController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultAddAnotherTaxRegimeController.onPageLoad(Yes, i)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[AddAnotherTaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(0, NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.get(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultAddAnotherTaxRegimeController.onSubmit(Yes, i)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[AddAnotherTaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Boolean]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.AddAnotherTaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a journey page nested within a switch-case subjourney and a do-while subjourney" in {
    val addATaxRegime       = journeyPage("addATaxRegime", FieldType.BOOLEAN)
    val addAnotherTaxRegime = journeyPage("addAnotherTaxRegime", FieldType.BOOLEAN)
    val taxRegime = journeyPage("taxRegime", ClassType(basePackage / "models" / "TaxRegime"))

    val journey = Journey(
      pages = Map(
        addATaxRegime.pageKey       -> addATaxRegime,
        addAnotherTaxRegime.pageKey -> addAnotherTaxRegime,
        taxRegime.pageKey           -> taxRegime
      ),
      journey = List(
        IfThenPart(
          addATaxRegime.pageKey,
          List(
            DoWhilePart(
              addAnotherTaxRegime.pageKey,
              List(SinglePagePart(taxRegime.pageKey, None)),
              "taxRegimes"
            )
          ),
          None
        )
      )
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "taxRegime",
      taxRegime
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.TaxRegimeBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.TaxRegimeView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultTaxRegimeControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultTaxRegimeFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: TaxRegimeBaseFormProvider = mock[TaxRegimeBaseFormProvider],
        |    view: TaxRegimeView = mock[TaxRegimeView]
        |  ) = new DefaultTaxRegimeController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultTaxRegimeController.onPageLoad(Yes, i)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[TaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
        |    val view = mock[TaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |      answers <- answers.set(TaxRegimePage(addATaxRegime, 0), answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[TaxRegime]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.get(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.TaxRegimeBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultTaxRegimeController.onSubmit(Yes, i)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in forAll(minSuccessful(5)) { (answer: TaxRegime) =>
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[TaxRegimeView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[TaxRegime]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      addATaxRegime = Boolean.Yes
        |      answers <- answers.set(AddATaxRegimePage, addATaxRegime)
        |    } yield answers
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.TaxRegimeBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a top-level do-while choice page" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val journey = Journey(
      pages = Map(
        "auditEvent"           -> auditEvent,
        "addAnotherAuditEvent" -> addAnotherAuditEvent
      ),
      journey = List(
        DoWhilePart(
          "addAnotherAuditEvent",
          List(SinglePagePart("auditEvent", None)),
          "auditEvents"
        )
      )
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "addAnotherAuditEvent",
      addAnotherAuditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.AddAnotherAuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddAnotherAuditEventView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultAddAnotherAuditEventControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultAddAnotherAuditEventFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: AddAnotherAuditEventBaseFormProvider = mock[AddAnotherAuditEventBaseFormProvider],
        |    view: AddAnotherAuditEventView = mock[AddAnotherAuditEventView]
        |  ) = new DefaultAddAnotherAuditEventController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultAddAnotherAuditEventController.onPageLoad(i)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[AddAnotherAuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  "DefaultAddAnotherAuditEventController.onSubmit(i)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[AddAnotherAuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Boolean]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a journey page nested within a do-while subjourney" in {
    val auditEvent = journeyPage("auditEvent", FieldType.STRING)

    val addAnotherAuditEvent = journeyPage(
      "addAnotherAuditEvent",
      FieldType.BOOLEAN
    )

    val journey = Journey(
      pages = Map(
        "auditEvent"           -> auditEvent,
        "addAnotherAuditEvent" -> addAnotherAuditEvent
      ),
      journey = List(
        DoWhilePart(
          "addAnotherAuditEvent",
          List(SinglePagePart("auditEvent", None)),
          "auditEvents"
        )
      )
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "auditEvent",
      auditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.AuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AuditEventView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultAuditEventControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultAuditEventFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: AuditEventBaseFormProvider = mock[AuditEventBaseFormProvider],
        |    view: AuditEventView = mock[AuditEventView]
        |  ) = new DefaultAuditEventController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultAuditEventController.onPageLoad(i)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[AuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.AuditEventBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(0, NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val view = mock[AuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      answers <- answers.set(AuditEventPage(0), answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AuditEventBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AuditEventBaseController.onPageLoad(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  "DefaultAuditEventController.onSubmit(i)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[AuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(0, NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a choice page nested within two do-while subjourneys" in {
    val auditSource           = journeyPage("auditSource", FieldType.STRING)
    val auditEvent            = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)
    val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val innerDoWhilePart = DoWhilePart(
      addAnotherAuditEvent.pageKey,
      List(SinglePagePart(auditEvent.pageKey, None)),
      "auditEvents"
    )

    val outerDoWhilePart = DoWhilePart(
      addAnotherAuditSource.pageKey,
      List(SinglePagePart(auditSource.pageKey, None), innerDoWhilePart),
      "auditSources"
    )

    val journey = Journey(
      pages = Map(
        auditSource.pageKey           -> auditSource,
        addAnotherAuditSource.pageKey -> addAnotherAuditSource,
        auditEvent.pageKey            -> auditEvent,
        addAnotherAuditEvent.pageKey  -> addAnotherAuditEvent
      ),
      journey = List(outerDoWhilePart)
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "addAnotherAuditEvent",
      addAnotherAuditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.AddAnotherAuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AddAnotherAuditEventView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultAddAnotherAuditEventControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultAddAnotherAuditEventFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: AddAnotherAuditEventBaseFormProvider = mock[AddAnotherAuditEventBaseFormProvider],
        |    view: AddAnotherAuditEventView = mock[AddAnotherAuditEventView]
        |  ) = new DefaultAddAnotherAuditEventController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultAddAnotherAuditEventController.onPageLoad(i, j)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[AddAnotherAuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onPageLoad(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onPageLoad(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  "DefaultAddAnotherAuditEventController.onSubmit(i, j)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, 0, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: Boolean) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, 0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[AddAnotherAuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(0, 0, NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[Boolean]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.AddAnotherAuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, 0, NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a journey page nested within two do-while subjourneys" in {
    val auditSource           = journeyPage("auditSource", FieldType.STRING)
    val auditEvent            = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditSource = journeyPage("addAnotherAuditSource", FieldType.BOOLEAN)
    val addAnotherAuditEvent  = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)

    val innerDoWhilePart = DoWhilePart(
      addAnotherAuditEvent.pageKey,
      List(SinglePagePart(auditEvent.pageKey, None)),
      "auditEvents"
    )

    val outerDoWhilePart = DoWhilePart(
      addAnotherAuditSource.pageKey,
      List(SinglePagePart(auditSource.pageKey, None), innerDoWhilePart),
      "auditSources"
    )

    val journey = Journey(
      pages = Map(
        auditSource.pageKey           -> auditSource,
        addAnotherAuditSource.pageKey -> addAnotherAuditSource,
        auditEvent.pageKey            -> auditEvent,
        addAnotherAuditEvent.pageKey  -> addAnotherAuditEvent
      ),
      journey = List(outerDoWhilePart)
    )

    JourneyPageController.journeyControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "auditEvent",
      auditEvent
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.AuditEventBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.AuditEventView
        |
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultAuditEventControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultAuditEventFormProvider()
        |  private val mockViewResponse    = "<html>Hello</html>"
        |
        |  private def makeController(
        |    userAnswers: Option[UserAnswers],
        |    navigator: JourneyNavigator = mock[JourneyNavigator],
        |    repository: SessionRepository = mock[SessionRepository],
        |    formProvider: AuditEventBaseFormProvider = mock[AuditEventBaseFormProvider],
        |    view: AuditEventView = mock[AuditEventView]
        |  ) = new DefaultAuditEventController(
        |    new FakeIdentifierAction(stubPlayBodyParsers),
        |    new FakeDataRetrievalAction(userAnswers),
        |    new DataRequiredActionImpl(),
        |    navigator,
        |    repository,
        |    formProvider,
        |    view,
        |    stubMessagesControllerComponents()
        |  )
        |
        |  "DefaultAuditEventController.onPageLoad(i, j)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val view = mock[AuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, view = view)
        |    val route = journeyRoutes.AuditEventBaseController.onPageLoad(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    verify(view)(any(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |  }
        |
        |  it should "pre-fill the form if the user has already supplied an answer" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val view = mock[AuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = for {
        |      answers <- Success(UserAnswers("id"))
        |      answers <- answers.set(AuditEventPage(0, 0), answer)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AuditEventBaseController.onPageLoad(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().value shouldBe Some(answer)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AuditEventBaseController.onPageLoad(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  "DefaultAuditEventController.onSubmit(i, j)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      formProvider = defaultFormProvider
        |    )
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, 0, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: String) =>
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val form = defaultFormProvider().fill(answer)
        |    val request = FakeRequest(route).withFormUrlEncodedBody(form.data.toSeq*)
        |    val response = controller.onSubmit(0, 0, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "return a 400 Bad Request response containing the view HTML when there is a problem with the user's request" in {
        |    val view = mock[AuditEventView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, view = view)
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onSubmit(0, 0, NormalMode)(request)
        |    status(response) shouldBe BAD_REQUEST
        |    contentAsString(response) shouldBe mockViewResponse
        |    val submitRoute = journeyRoutes.AuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[String]])
        |    verify(view)(formCaptor.capture(), eqTo(submitRoute), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should not be(empty)
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val controller = makeController(userAnswers = userAnswers.toOption, formProvider = defaultFormProvider, repository = repository)
        |    val route = journeyRoutes.AuditEventBaseController.onSubmit(0, 0, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onPageLoad(0, 0, NormalMode)(request)
        |    }
        |  }
        |}
        |""".stripMargin
  }

  "JourneyPageController.fileUploadController" should "generate a controller for a top-level file upload journey page that doesn't require data" in {
    val uploadWillDocument =
      journeyPage("uploadWillDocument", ClassType(basePackage / "models" / "UploadId"))

    val journey = Journey(
      Map(uploadWillDocument.pageKey -> uploadWillDocument),
      List(SinglePagePart(uploadWillDocument.pageKey, None))
    )

    JourneyPageController.fileUploadController(
      basePackage,
      requiresData = false,
      journey,
      "uploadWillDocument",
      uploadWillDocument
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.controllers.upscan.{routes as upscanRoutes}
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.forms.UploadWillDocumentBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.UploadWillDocumentView
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
        |@ImplementedBy(classOf[DefaultUploadWillDocumentController])
        |trait UploadWillDocumentBaseController extends FrontendBaseController, I18nSupport, Logging {
        |  def onPageLoad(mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |  def onUploadSuccess(id: UUID, mode: Mode): Action[AnyContent]
        |  def onUploadFailure(id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultUploadWillDocumentController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  upscanConnector: UpscanConnector,
        |  fileUploadRepository: FileUploadRepository,
        |  form: UploadWillDocumentBaseFormProvider,
        |  view: UploadWillDocumentView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends UploadWillDocumentBaseController {
        |
        |  def onPageLoad(mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData).async { implicit request =>
        |    val uploadId = UploadId.next()
        |    for {
        |      initiateResponse <- upscanConnector.initiate(
        |        callbackUrl = upscanRoutes.UpscanNotificationBaseController.onNotificationReceived(uploadId.id),
        |        successRedirect = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId.id, mode),
        |        errorRedirect = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(uploadId.id, mode, None, None, None)
        |      )
        |      uploadId <- fileUploadRepository.initiate(uploadId, request.userId, initiateResponse.reference)
        |      formTemplate = initiateResponse.uploadRequest
        |      preparedForm <- upscanReference.fold(Future.successful(form())) { ref =>
        |        val reference = UpscanReference(ref)
        |        fileUploadRepository.setRejected(request.userId, reference).map { _ =>
        |          val errorCode = upscanErrorCode.orNull
        |          val errorMessage = upscanErrorMessage.orNull
        |          logger.error(s"File upload with reference $reference failed with error code $errorCode: $errorMessage")
        |          val uploadError = UploadError.fromErrorCode(errorCode)
        |          form().withError("file", uploadError.messageKey)
        |        }
        |      }
        |    } yield Ok(view(preparedForm, formTemplate, mode))
        |  }
        |
        |  def onUploadSuccess(id: UUID, mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
        |    val uploadId = UploadId(id)
        |    val page = UploadWillDocumentPage
        |    val userAnswers = request.userAnswers
        |      .getOrElse(UserAnswers(request.userId))
        |    for {
        |      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
        |      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
        |      _ <- sessionRepository.set(updatedAnswers)
        |    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
        |  }
        |
        |  def onUploadFailure(id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData) { implicit request =>
        |    Redirect(journeyRoutes.UploadWillDocumentBaseController.onPageLoad(mode, upscanReference, upscanErrorCode, upscanErrorMessage))
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a top-level file upload journey page that does require data" in {
    val uploadWillDocument =
      journeyPage("uploadWillDocument", ClassType(basePackage / "models" / "UploadId"))

    val journey = Journey(
      Map(uploadWillDocument.pageKey -> uploadWillDocument),
      List(SinglePagePart(uploadWillDocument.pageKey, None))
    )

    JourneyPageController.fileUploadController(
      basePackage,
      requiresData = true,
      journey,
      "uploadWillDocument",
      uploadWillDocument
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.controllers.upscan.{routes as upscanRoutes}
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.forms.UploadWillDocumentBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.UploadWillDocumentView
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
        |@ImplementedBy(classOf[DefaultUploadWillDocumentController])
        |trait UploadWillDocumentBaseController extends FrontendBaseController, I18nSupport, Logging {
        |  def onPageLoad(mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |  def onUploadSuccess(id: UUID, mode: Mode): Action[AnyContent]
        |  def onUploadFailure(id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultUploadWillDocumentController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  upscanConnector: UpscanConnector,
        |  fileUploadRepository: FileUploadRepository,
        |  form: UploadWillDocumentBaseFormProvider,
        |  view: UploadWillDocumentView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends UploadWillDocumentBaseController {
        |
        |  def onPageLoad(mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId.next()
        |    for {
        |      initiateResponse <- upscanConnector.initiate(
        |        callbackUrl = upscanRoutes.UpscanNotificationBaseController.onNotificationReceived(uploadId.id),
        |        successRedirect = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId.id, mode),
        |        errorRedirect = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(uploadId.id, mode, None, None, None)
        |      )
        |      uploadId <- fileUploadRepository.initiate(uploadId, request.userId, initiateResponse.reference)
        |      formTemplate = initiateResponse.uploadRequest
        |      preparedForm <- upscanReference.fold(Future.successful(form())) { ref =>
        |        val reference = UpscanReference(ref)
        |        fileUploadRepository.setRejected(request.userId, reference).map { _ =>
        |          val errorCode = upscanErrorCode.orNull
        |          val errorMessage = upscanErrorMessage.orNull
        |          logger.error(s"File upload with reference $reference failed with error code $errorCode: $errorMessage")
        |          val uploadError = UploadError.fromErrorCode(errorCode)
        |          form().withError("file", uploadError.messageKey)
        |        }
        |      }
        |    } yield Ok(view(preparedForm, formTemplate, mode))
        |  }
        |
        |  def onUploadSuccess(id: UUID, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId(id)
        |    val page = UploadWillDocumentPage
        |    val userAnswers = request.userAnswers
        |    for {
        |      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
        |      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
        |      _ <- sessionRepository.set(updatedAnswers)
        |    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
        |  }
        |
        |  def onUploadFailure(id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    Redirect(journeyRoutes.UploadWillDocumentBaseController.onPageLoad(mode, upscanReference, upscanErrorCode, upscanErrorMessage))
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a file upload journey page nested within a do-while subjourney" in {
    val uploadWillDocument =
      journeyPage("uploadWillDocument", ClassType(basePackage / "models" / "UploadId"))

    val addAnotherWillDocument = journeyPage(
      "addAnotherWillDocument",
      FieldType.BOOLEAN
    )

    val journey = Journey(
      pages = Map(
        "uploadWillDocument"     -> uploadWillDocument,
        "addAnotherWillDocument" -> addAnotherWillDocument
      ),
      journey = List(
        DoWhilePart(
          "addAnotherWillDocument",
          List(SinglePagePart("uploadWillDocument", None)),
          "willDocuments"
        )
      )
    )

    JourneyPageController.fileUploadController(
      basePackage,
      requiresData = true,
      journey,
      "uploadWillDocument",
      uploadWillDocument
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.controllers.upscan.{routes as upscanRoutes}
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.forms.UploadWillDocumentBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.UploadWillDocumentView
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
        |@ImplementedBy(classOf[DefaultUploadWillDocumentController])
        |trait UploadWillDocumentBaseController extends FrontendBaseController, I18nSupport, Logging {
        |  def onPageLoad(willDocumentsIndex: Int, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |  def onUploadSuccess(willDocumentsIndex: Int, id: UUID, mode: Mode): Action[AnyContent]
        |  def onUploadFailure(willDocumentsIndex: Int, id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultUploadWillDocumentController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  upscanConnector: UpscanConnector,
        |  fileUploadRepository: FileUploadRepository,
        |  form: UploadWillDocumentBaseFormProvider,
        |  view: UploadWillDocumentView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends UploadWillDocumentBaseController {
        |
        |  def onPageLoad(willDocumentsIndex: Int, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId.next()
        |    for {
        |      initiateResponse <- upscanConnector.initiate(
        |        callbackUrl = upscanRoutes.UpscanNotificationBaseController.onNotificationReceived(uploadId.id),
        |        successRedirect = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(willDocumentsIndex: Int, uploadId.id, mode),
        |        errorRedirect = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(willDocumentsIndex: Int, uploadId.id, mode, None, None, None)
        |      )
        |      uploadId <- fileUploadRepository.initiate(uploadId, request.userId, initiateResponse.reference)
        |      formTemplate = initiateResponse.uploadRequest
        |      preparedForm <- upscanReference.fold(Future.successful(form())) { ref =>
        |        val reference = UpscanReference(ref)
        |        fileUploadRepository.setRejected(request.userId, reference).map { _ =>
        |          val errorCode = upscanErrorCode.orNull
        |          val errorMessage = upscanErrorMessage.orNull
        |          logger.error(s"File upload with reference $reference failed with error code $errorCode: $errorMessage")
        |          val uploadError = UploadError.fromErrorCode(errorCode)
        |          form().withError("file", uploadError.messageKey)
        |        }
        |      }
        |    } yield Ok(view(preparedForm, formTemplate, mode))
        |  }
        |
        |  def onUploadSuccess(willDocumentsIndex: Int, id: UUID, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId(id)
        |    val page = UploadWillDocumentPage(willDocumentsIndex)
        |    val userAnswers = request.userAnswers
        |    for {
        |      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
        |      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
        |      _ <- sessionRepository.set(updatedAnswers)
        |    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
        |  }
        |
        |  def onUploadFailure(willDocumentsIndex: Int, id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    Redirect(journeyRoutes.UploadWillDocumentBaseController.onPageLoad(willDocumentsIndex, mode, upscanReference, upscanErrorCode, upscanErrorMessage))
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a file upload journey page nested within two do-while subjourneys" in {
    val auditSource          = journeyPage("auditSource", FieldType.STRING)
    val auditEvent           = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditEvent = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)
    val evidenceFromQA =
      journeyPage("evidenceFromQA", ClassType(basePackage / "models" / "UploadId"))

    val journey = Journey(
      pages = Map(
        "auditEvent"           -> auditEvent,
        "auditSource"          -> auditSource,
        "evidenceFromQA"       -> evidenceFromQA,
        "addAnotherAuditEvent" -> addAnotherAuditEvent
      ),
      journey = List(
        DoWhilePart(
          "addAnotherAuditSource",
          List(
            SinglePagePart("auditSource", None),
            DoWhilePart(
              "addAnotherAuditEvent",
              List(SinglePagePart("auditEvent", None), SinglePagePart("evidenceFromQA", None)),
              "auditEvents"
            )
          ),
          "auditSources"
        )
      )
    )

    JourneyPageController.fileUploadController(
      basePackage,
      requiresData = true,
      journey,
      "evidenceFromQA",
      evidenceFromQA
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.controllers.upscan.{routes as upscanRoutes}
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.forms.EvidenceFromQaBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.EvidenceFromQaView
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
        |@ImplementedBy(classOf[DefaultEvidenceFromQaController])
        |trait EvidenceFromQaBaseController extends FrontendBaseController, I18nSupport, Logging {
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |  def onUploadSuccess(auditSourcesIndex: Int, auditEventsIndex: Int, id: UUID, mode: Mode): Action[AnyContent]
        |  def onUploadFailure(auditSourcesIndex: Int, auditEventsIndex: Int, id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultEvidenceFromQaController @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  upscanConnector: UpscanConnector,
        |  fileUploadRepository: FileUploadRepository,
        |  form: EvidenceFromQaBaseFormProvider,
        |  view: EvidenceFromQaView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends EvidenceFromQaBaseController {
        |
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId.next()
        |    for {
        |      initiateResponse <- upscanConnector.initiate(
        |        callbackUrl = upscanRoutes.UpscanNotificationBaseController.onNotificationReceived(uploadId.id),
        |        successRedirect = journeyRoutes.EvidenceFromQaBaseController.onUploadSuccess(auditSourcesIndex: Int, auditEventsIndex: Int, uploadId.id, mode),
        |        errorRedirect = journeyRoutes.EvidenceFromQaBaseController.onUploadFailure(auditSourcesIndex: Int, auditEventsIndex: Int, uploadId.id, mode, None, None, None)
        |      )
        |      uploadId <- fileUploadRepository.initiate(uploadId, request.userId, initiateResponse.reference)
        |      formTemplate = initiateResponse.uploadRequest
        |      preparedForm <- upscanReference.fold(Future.successful(form())) { ref =>
        |        val reference = UpscanReference(ref)
        |        fileUploadRepository.setRejected(request.userId, reference).map { _ =>
        |          val errorCode = upscanErrorCode.orNull
        |          val errorMessage = upscanErrorMessage.orNull
        |          logger.error(s"File upload with reference $reference failed with error code $errorCode: $errorMessage")
        |          val uploadError = UploadError.fromErrorCode(errorCode)
        |          form().withError("file", uploadError.messageKey)
        |        }
        |      }
        |    } yield Ok(view(preparedForm, formTemplate, mode))
        |  }
        |
        |  def onUploadSuccess(auditSourcesIndex: Int, auditEventsIndex: Int, id: UUID, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId(id)
        |    val page = EvidenceFromQaPage(auditSourcesIndex, auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    for {
        |      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
        |      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
        |      _ <- sessionRepository.set(updatedAnswers)
        |    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
        |  }
        |
        |  def onUploadFailure(auditSourcesIndex: Int, auditEventsIndex: Int, id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    Redirect(journeyRoutes.EvidenceFromQaBaseController.onPageLoad(auditSourcesIndex, auditEventsIndex, mode, upscanReference, upscanErrorCode, upscanErrorMessage))
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller for a file upload journey page nested within a switch-case subjourney" in {
    val iht401 =
      journeyPage("iht401", ClassType(basePackage / "models" / "UploadId"))
    val whereDomiciled =
      journeyPage("whereDomiciled", ClassType(basePackage / "models" / "Domicile"))

    val journey = Journey(
      Map(
        "iht401"         -> iht401,
        "whereDomiciled" -> whereDomiciled
      ),
      List(
        SwitchCasePart(
          "whereDomiciled",
          Map(
            "OTHER"    -> List(SinglePagePart("iht401", None)),
            "SCOTLAND" -> List(SinglePagePart("legitimFundDischarged", None))
          ),
          None
        )
      )
    )

    JourneyPageController.fileUploadController(
      basePackage,
      requiresData = true,
      journey,
      "iht401",
      iht401
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import controllers.actions.*  // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import models.Mode // uk.gov.hmrc.sbtjourneytest.models.Mode
        |import models.UserAnswers // uk.gov.hmrc.sbtjourneytest.models.UserAnswers
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.sbtjourneytest.controllers.{routes as journeyRoutes}
        |import uk.gov.hmrc.sbtjourneytest.controllers.upscan.{routes as upscanRoutes}
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.forms.Iht401BaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.navigation.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import views.html.Iht401View
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
        |@ImplementedBy(classOf[DefaultIht401Controller])
        |trait Iht401BaseController extends FrontendBaseController, I18nSupport, Logging {
        |  def onPageLoad(mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |  def onUploadSuccess(id: UUID, mode: Mode): Action[AnyContent]
        |  def onUploadFailure(id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent]
        |}
        |
        |@Singleton
        |class DefaultIht401Controller @Inject() (
        |  identify: IdentifierAction,
        |  getData: DataRetrievalAction,
        |  requireData: DataRequiredAction,
        |  navigator: JourneyNavigator,
        |  sessionRepository: SessionRepository,
        |  upscanConnector: UpscanConnector,
        |  fileUploadRepository: FileUploadRepository,
        |  form: Iht401BaseFormProvider,
        |  view: Iht401View,
        |  override val controllerComponents: MessagesControllerComponents
        |)(using ExecutionContext) extends Iht401BaseController {
        |
        |  def onPageLoad(mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId.next()
        |    for {
        |      initiateResponse <- upscanConnector.initiate(
        |        callbackUrl = upscanRoutes.UpscanNotificationBaseController.onNotificationReceived(uploadId.id),
        |        successRedirect = journeyRoutes.Iht401BaseController.onUploadSuccess(uploadId.id, mode),
        |        errorRedirect = journeyRoutes.Iht401BaseController.onUploadFailure(uploadId.id, mode, None, None, None)
        |      )
        |      uploadId <- fileUploadRepository.initiate(uploadId, request.userId, initiateResponse.reference)
        |      formTemplate = initiateResponse.uploadRequest
        |      preparedForm <- upscanReference.fold(Future.successful(form())) { ref =>
        |        val reference = UpscanReference(ref)
        |        fileUploadRepository.setRejected(request.userId, reference).map { _ =>
        |          val errorCode = upscanErrorCode.orNull
        |          val errorMessage = upscanErrorMessage.orNull
        |          logger.error(s"File upload with reference $reference failed with error code $errorCode: $errorMessage")
        |          val uploadError = UploadError.fromErrorCode(errorCode)
        |          form().withError("file", uploadError.messageKey)
        |        }
        |      }
        |    } yield Ok(view(preparedForm, formTemplate, mode))
        |  }
        |
        |  def onUploadSuccess(id: UUID, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val uploadId = UploadId(id)
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      whereDomiciled <- userAnswers.get(WhereDomiciledPage)
        |      page = Iht401Page(whereDomiciled)
        |    } yield for {
        |      updatedAnswers <- Future.fromTry(userAnswers.set(page, uploadId))
        |      _ <- fileUploadRepository.setProcessing(uploadId, request.userId)
        |      _ <- sessionRepository.set(updatedAnswers)
        |    } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, uploadId))
        |    result.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
        |  }
        |
        |  def onUploadFailure(id: UUID, mode: Mode, upscanReference: Option[String], upscanErrorCode: Option[String], upscanErrorMessage: Option[String]): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    Redirect(journeyRoutes.Iht401BaseController.onPageLoad(mode, upscanReference, upscanErrorCode, upscanErrorMessage))
        |  }
        |}
        |""".stripMargin
  }

  "JourneyPageController.fileUploadControllerSpec" should "generate a controller spec for a top-level file upload journey page that doesn't require data" in {
    val uploadWillDocument =
      journeyPage("uploadWillDocument", ClassType(basePackage / "models" / "UploadId"))

    val journey = Journey(
      Map(uploadWillDocument.pageKey -> uploadWillDocument),
      List(SinglePagePart(uploadWillDocument.pageKey, None))
    )

    JourneyPageController.fileUploadControllerSpec(
      basePackage,
      requiresData = false,
      journey,
      "uploadWillDocument",
      uploadWillDocument
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.http.UpstreamErrorResponse
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.UploadWillDocumentBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import views.html.UploadWillDocumentView
        |
        |import java.time.{Instant,ZoneOffset}
        |import java.time.format.DateTimeFormatter
        |import java.util.UUID
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultUploadWillDocumentControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultUploadWillDocumentFormProvider()
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
        |        "x-amz-meta-original-filename"        -> "${filename}",
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
        |    formProvider: UploadWillDocumentBaseFormProvider = mock[UploadWillDocumentBaseFormProvider],
        |    view: views.html.UploadWillDocumentView = mock[views.html.UploadWillDocumentView]
        |  ) = new DefaultUploadWillDocumentController(
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
        |
        |  "DefaultUploadWillDocumentController.onPageLoad" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    val view = mock[UploadWillDocumentView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val controller = makeController(
        |      userAnswers = Some(UserAnswers("id")),
        |      upscanConnector = upscanConnector,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider,
        |      view = view
        |    )
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, None, None, None)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should be(empty)
        |  }
        |
        |  it should "create an empty user answers object when the user's answers can't be retrieved" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    val view = mock[UploadWillDocumentView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val controller = makeController(
        |      userAnswers = None,
        |      upscanConnector = upscanConnector,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider,
        |      view = view
        |    )
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, None, None, None)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should be(empty)
        |  }
        |
        |  it should "render the upload form with errors when the user is redirected to the page with error detail parameters" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    when(fileUploadRepository.setRejected(any(), UpscanReference(any()))).thenReturn(Future.unit)
        |    val view = mock[UploadWillDocumentView]
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
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, key, errorCode, errorMessage)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should contain(FormError("file", "upload.error.fileTooSmall"))
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")), upscanConnector = upscanConnector)
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[UpstreamErrorResponse] {
        |      controller.onPageLoad(NormalMode, None, None, None)(request)
        |    }
        |  }
        |
        |  "DefaultUploadWillDocumentController.onUploadSuccess" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider
        |    )
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "create an empty user answers object when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: UploadId) =>
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
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, fileUploadRepository = fileUploadRepository)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultUploadWillDocumentController.onUploadFailure" should "redirect to onPageLoad preserving the query string" in {
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")))
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val nextPage = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, key, errorCode, errorMessage)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "create an empty user answers object when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val nextPage = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, key, errorCode, errorMessage)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a top-level file upload journey page that does require data" in {
    val uploadWillDocument =
      journeyPage("uploadWillDocument", ClassType(basePackage / "models" / "UploadId"))

    val journey = Journey(
      Map(uploadWillDocument.pageKey -> uploadWillDocument),
      List(SinglePagePart(uploadWillDocument.pageKey, None))
    )

    JourneyPageController.fileUploadControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "uploadWillDocument",
      uploadWillDocument
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.http.UpstreamErrorResponse
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.UploadWillDocumentBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import views.html.UploadWillDocumentView
        |
        |import java.time.{Instant,ZoneOffset}
        |import java.time.format.DateTimeFormatter
        |import java.util.UUID
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultUploadWillDocumentControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultUploadWillDocumentFormProvider()
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
        |        "x-amz-meta-original-filename"        -> "${filename}",
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
        |    formProvider: UploadWillDocumentBaseFormProvider = mock[UploadWillDocumentBaseFormProvider],
        |    view: views.html.UploadWillDocumentView = mock[views.html.UploadWillDocumentView]
        |  ) = new DefaultUploadWillDocumentController(
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
        |
        |  "DefaultUploadWillDocumentController.onPageLoad" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    val view = mock[UploadWillDocumentView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val controller = makeController(
        |      userAnswers = Some(UserAnswers("id")),
        |      upscanConnector = upscanConnector,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider,
        |      view = view
        |    )
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, None, None, None)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should be(empty)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, None, None, None)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "render the upload form with errors when the user is redirected to the page with error detail parameters" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    when(fileUploadRepository.setRejected(any(), UpscanReference(any()))).thenReturn(Future.unit)
        |    val view = mock[UploadWillDocumentView]
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
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, key, errorCode, errorMessage)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should contain(FormError("file", "upload.error.fileTooSmall"))
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")), upscanConnector = upscanConnector)
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[UpstreamErrorResponse] {
        |      controller.onPageLoad(NormalMode, None, None, None)(request)
        |    }
        |  }
        |
        |  "DefaultUploadWillDocumentController.onUploadSuccess" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider
        |    )
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, fileUploadRepository = fileUploadRepository)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultUploadWillDocumentController.onUploadFailure" should "redirect to onPageLoad preserving the query string" in {
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")))
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val nextPage = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(NormalMode, key, errorCode, errorMessage)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a file upload journey page nested within a do-while subjourney" in {
    val uploadWillDocument =
      journeyPage("uploadWillDocument", ClassType(basePackage / "models" / "UploadId"))

    val addAnotherWillDocument = journeyPage(
      "addAnotherWillDocument",
      FieldType.BOOLEAN
    )

    val journey = Journey(
      pages = Map(
        "uploadWillDocument"     -> uploadWillDocument,
        "addAnotherWillDocument" -> addAnotherWillDocument
      ),
      journey = List(
        DoWhilePart(
          "addAnotherWillDocument",
          List(SinglePagePart("uploadWillDocument", None)),
          "willDocuments"
        )
      )
    )

    JourneyPageController.fileUploadControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "uploadWillDocument",
      uploadWillDocument
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.http.UpstreamErrorResponse
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.UploadWillDocumentBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import views.html.UploadWillDocumentView
        |
        |import java.time.{Instant,ZoneOffset}
        |import java.time.format.DateTimeFormatter
        |import java.util.UUID
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultUploadWillDocumentControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultUploadWillDocumentFormProvider()
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
        |        "x-amz-meta-original-filename"        -> "${filename}",
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
        |    formProvider: UploadWillDocumentBaseFormProvider = mock[UploadWillDocumentBaseFormProvider],
        |    view: views.html.UploadWillDocumentView = mock[views.html.UploadWillDocumentView]
        |  ) = new DefaultUploadWillDocumentController(
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
        |
        |  "DefaultUploadWillDocumentController.onPageLoad(i)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    val view = mock[UploadWillDocumentView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val controller = makeController(
        |      userAnswers = Some(UserAnswers("id")),
        |      upscanConnector = upscanConnector,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider,
        |      view = view
        |    )
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(0, NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode, None, None, None)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should be(empty)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(0, NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode, None, None, None)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "render the upload form with errors when the user is redirected to the page with error detail parameters" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    when(fileUploadRepository.setRejected(any(), UpscanReference(any()))).thenReturn(Future.unit)
        |    val view = mock[UploadWillDocumentView]
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
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(0, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, NormalMode, key, errorCode, errorMessage)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should contain(FormError("file", "upload.error.fileTooSmall"))
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")), upscanConnector = upscanConnector)
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(0, NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[UpstreamErrorResponse] {
        |      controller.onPageLoad(0, NormalMode, None, None, None)(request)
        |    }
        |  }
        |
        |  "DefaultUploadWillDocumentController.onUploadSuccess(i)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider
        |    )
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(0, uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(0, uploadId, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(0, uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(0, uploadId, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, fileUploadRepository = fileUploadRepository)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadSuccess(0, uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onUploadSuccess(0, uploadId, NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultUploadWillDocumentController.onUploadFailure(i)" should "redirect to onPageLoad preserving the query string" in {
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")))
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val nextPage = journeyRoutes.UploadWillDocumentBaseController.onPageLoad(0, NormalMode, key, errorCode, errorMessage)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.UploadWillDocumentBaseController.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(0, uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a file upload journey page nested within two do-while subjourneys" in {
    val auditSource          = journeyPage("auditSource", FieldType.STRING)
    val auditEvent           = journeyPage("auditEvent", FieldType.STRING)
    val addAnotherAuditEvent = journeyPage("addAnotherAuditEvent", FieldType.BOOLEAN)
    val evidenceFromQA =
      journeyPage("evidenceFromQA", ClassType(basePackage / "models" / "UploadId"))

    val journey = Journey(
      pages = Map(
        "auditEvent"           -> auditEvent,
        "auditSource"          -> auditSource,
        "evidenceFromQA"       -> evidenceFromQA,
        "addAnotherAuditEvent" -> addAnotherAuditEvent
      ),
      journey = List(
        DoWhilePart(
          "addAnotherAuditSource",
          List(
            SinglePagePart("auditSource", None),
            DoWhilePart(
              "addAnotherAuditEvent",
              List(SinglePagePart("auditEvent", None), SinglePagePart("evidenceFromQA", None)),
              "auditEvents"
            )
          ),
          "auditSources"
        )
      )
    )

    JourneyPageController.fileUploadControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "evidenceFromQA",
      evidenceFromQA
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.http.UpstreamErrorResponse
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.EvidenceFromQaBaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import views.html.EvidenceFromQaView
        |
        |import java.time.{Instant,ZoneOffset}
        |import java.time.format.DateTimeFormatter
        |import java.util.UUID
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultEvidenceFromQaControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultEvidenceFromQaFormProvider()
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
        |        "x-amz-meta-original-filename"        -> "${filename}",
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
        |    formProvider: EvidenceFromQaBaseFormProvider = mock[EvidenceFromQaBaseFormProvider],
        |    view: views.html.EvidenceFromQaView = mock[views.html.EvidenceFromQaView]
        |  ) = new DefaultEvidenceFromQaController(
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
        |
        |  "DefaultEvidenceFromQaController.onPageLoad(i, j)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    val view = mock[EvidenceFromQaView]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val controller = makeController(
        |      userAnswers = Some(UserAnswers("id")),
        |      upscanConnector = upscanConnector,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider,
        |      view = view
        |    )
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onPageLoad(0, 0, NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode, None, None, None)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should be(empty)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onPageLoad(0, 0, NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode, None, None, None)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "render the upload form with errors when the user is redirected to the page with error detail parameters" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    when(fileUploadRepository.setRejected(any(), UpscanReference(any()))).thenReturn(Future.unit)
        |    val view = mock[EvidenceFromQaView]
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
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onPageLoad(0, 0, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(0, 0, NormalMode, key, errorCode, errorMessage)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should contain(FormError("file", "upload.error.fileTooSmall"))
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")), upscanConnector = upscanConnector)
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onPageLoad(0, 0, NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[UpstreamErrorResponse] {
        |      controller.onPageLoad(0, 0, NormalMode, None, None, None)(request)
        |    }
        |  }
        |
        |  "DefaultEvidenceFromQaController.onUploadSuccess(i, j)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider
        |    )
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onUploadSuccess(0, 0, uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(0, 0, uploadId, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onUploadSuccess(0, 0, uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(0, 0, uploadId, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption, fileUploadRepository = fileUploadRepository)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onUploadSuccess(0, 0, uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onUploadSuccess(0, 0, uploadId, NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultEvidenceFromQaController.onUploadFailure(i, j)" should "redirect to onPageLoad preserving the query string" in {
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")))
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onUploadFailure(0, 0, uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(0, 0, uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val nextPage = journeyRoutes.EvidenceFromQaBaseController.onPageLoad(0, 0, NormalMode, key, errorCode, errorMessage)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.EvidenceFromQaBaseController.onUploadFailure(0, 0, uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(0, 0, uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |}
        |""".stripMargin
  }

  it should "generate a controller spec for a file upload journey page nested within a switch-case subjourney" in {
    val iht401 =
      journeyPage("iht401", ClassType(basePackage / "models" / "UploadId"))
    val whereDomiciled =
      journeyPage("whereDomiciled", ClassType(basePackage / "models" / "Domicile"))

    val journey = Journey(
      Map(
        "iht401"         -> iht401,
        "whereDomiciled" -> whereDomiciled
      ),
      List(
        SwitchCasePart(
          "whereDomiciled",
          Map(
            "OTHER"    -> List(SinglePagePart("iht401", None)),
            "SCOTLAND" -> List(SinglePagePart("legitimFundDischarged", None))
          ),
          None
        )
      )
    )

    JourneyPageController.fileUploadControllerSpec(
      basePackage,
      requiresData = true,
      journey,
      "iht401",
      iht401
    ) shouldBe
      """package uk.gov.hmrc.sbtjourneytest.controllers
        |
        |import _root_.controllers.actions.* // uk.gov.hmrc.sbtjourneytest.controllers.actions.*
        |import _root_.controllers.routes // uk.gov.hmrc.sbtjourneytest.controllers.routes
        |import _root_.models.{NormalMode, UserAnswers} // uk.gov.hmrc.sbtjourneytest.models.{NormalMode,UserAnswers}
        |import _root_.generators.Generators // uk.gov.hmrc.sbtjourneytest.generators.Generators
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
        |import repositories.SessionRepository // uk.gov.hmrc.sbtjourneytest.repositories.SessionRepository
        |import uk.gov.hmrc.http.UpstreamErrorResponse
        |import uk.gov.hmrc.sbtjourneytest.connectors.UpscanConnector
        |import uk.gov.hmrc.sbtjourneytest.controllers.routes as journeyRoutes
        |import uk.gov.hmrc.sbtjourneytest.forms.Iht401BaseFormProvider
        |import uk.gov.hmrc.sbtjourneytest.forms.*
        |import uk.gov.hmrc.sbtjourneytest.models.*
        |import uk.gov.hmrc.sbtjourneytest.models.upscan.*
        |import uk.gov.hmrc.sbtjourneytest.navigation.JourneyNavigator
        |import uk.gov.hmrc.sbtjourneytest.pages.*
        |import uk.gov.hmrc.sbtjourneytest.repositories.FileUploadRepository
        |import views.html.Iht401View
        |
        |import java.time.{Instant,ZoneOffset}
        |import java.time.format.DateTimeFormatter
        |import java.util.UUID
        |import scala.concurrent.{ExecutionContext,Future}
        |import scala.util.{Success,Try}
        |
        |class DefaultIht401ControllerSpec extends AnyFlatSpec, Matchers, ScalaCheckPropertyChecks, RecoverMethods, TryValues, MockitoSugar, Generators {
        |  given system: ActorSystem = ActorSystem("test")
        |  given ExecutionContext    = system.dispatcher
        |  given Messages            = stubMessages()
        |
        |  private val defaultFormProvider = new DefaultIht401FormProvider()
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
        |        "x-amz-meta-original-filename"        -> "${filename}",
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
        |    formProvider: Iht401BaseFormProvider = mock[Iht401BaseFormProvider],
        |    view: views.html.Iht401View = mock[views.html.Iht401View]
        |  ) = new DefaultIht401Controller(
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
        |
        |  "DefaultIht401Controller.onPageLoad(OTHER)" should "return a 200 OK response containing the view HTML when everything is successful" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    val view = mock[Iht401View]
        |    when(view(any(), any(), any())(any(), any())).thenReturn(raw(mockViewResponse))
        |    val controller = makeController(
        |      userAnswers = Some(UserAnswers("id")),
        |      upscanConnector = upscanConnector,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider,
        |      view = view
        |    )
        |    val route = journeyRoutes.Iht401BaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, None, None, None)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should be(empty)
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val route = journeyRoutes.Iht401BaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, None, None, None)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "render the upload form with errors when the user is redirected to the page with error detail parameters" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.successful(initiateResponse))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.initiate(any(), any(), any())).thenAnswer(i => Future.successful(i.getArgument[UploadId](0)))
        |    when(fileUploadRepository.setRejected(any(), UpscanReference(any()))).thenReturn(Future.unit)
        |    val view = mock[Iht401View]
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
        |    val route = journeyRoutes.Iht401BaseController.onPageLoad(NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onPageLoad(NormalMode, key, errorCode, errorMessage)(request)
        |    status(response) shouldBe OK
        |    contentAsString(response) shouldBe mockViewResponse
        |    val formCaptor = ArgumentCaptor.forClass(classOf[Form[_]])
        |    verify(view)(formCaptor.capture(), eqTo(initiateResponse.uploadRequest), eqTo(NormalMode))(any(), any())
        |    formCaptor.getValue().errors should contain(FormError("file", "upload.error.fileTooSmall"))
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val upscanConnector = mock[UpscanConnector]
        |    when(upscanConnector.initiate(any(), any(), any())(using any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")), upscanConnector = upscanConnector)
        |    val route = journeyRoutes.Iht401BaseController.onPageLoad(NormalMode, None, None, None)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[UpstreamErrorResponse] {
        |      controller.onPageLoad(NormalMode, None, None, None)(request)
        |    }
        |  }
        |
        |  "DefaultIht401Controller.onUploadSuccess(OTHER)" should "redirect to the next page when everything is successful" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val navigator = mock[JourneyNavigator]
        |    val nextPage = routes.CheckYourAnswersController.onPageLoad()
        |    when(navigator.nextPage(any(), any(), any(), any())).thenReturn(nextPage)
        |    val repository = mock[SessionRepository]
        |    when(repository.set(any())).thenReturn(Future.successful(true))
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.unit)
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      whereDomiciled = Domicile.OTHER
        |      answers <- answers.set(WhereDomiciledPage, whereDomiciled)
        |    } yield answers
        |    val controller = makeController(
        |      userAnswers = userAnswers.toOption,
        |      navigator = navigator,
        |      repository = repository,
        |      fileUploadRepository = fileUploadRepository,
        |      formProvider = defaultFormProvider
        |    )
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.Iht401BaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in forAll(minSuccessful(5)) { (answer: UploadId) =>
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.Iht401BaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "redirect to journey recovery when a prerequisite answer is missing" in {
        |    val userAnswers = Success(UserAnswers("id"))
        |    val controller = makeController(userAnswers = userAnswers.toOption)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.Iht401BaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |
        |  it should "throw exceptions to the top level error handler" in {
        |    val fileUploadRepository = mock[FileUploadRepository]
        |    when(fileUploadRepository.setProcessing(any(), any())).thenReturn(Future.failed(new MongoException("Error")))
        |    val userAnswers = for {
        |      answers <- Try(UserAnswers("id"))
        |      whereDomiciled = Domicile.OTHER
        |      answers <- answers.set(WhereDomiciledPage, whereDomiciled)
        |    } yield answers
        |    val controller = makeController(userAnswers = userAnswers.toOption, fileUploadRepository = fileUploadRepository)
        |    val uploadId = UUID.randomUUID()
        |    val route = journeyRoutes.Iht401BaseController.onUploadSuccess(uploadId, NormalMode)
        |    val request = FakeRequest(route)
        |    recoverToSucceededIf[MongoException] {
        |      controller.onUploadSuccess(uploadId, NormalMode)(request)
        |    }
        |  }
        |
        |  "DefaultIht401Controller.onUploadFailure(OTHER)" should "redirect to onPageLoad preserving the query string" in {
        |    val controller = makeController(userAnswers = Some(UserAnswers("id")))
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.Iht401BaseController.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val nextPage = journeyRoutes.Iht401BaseController.onPageLoad(NormalMode, key, errorCode, errorMessage)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(nextPage.path())
        |  }
        |
        |  it should "redirect to journey recovery when the user's answers can't be retrieved" in {
        |    val controller = makeController(userAnswers = None)
        |    val uploadId = UUID.randomUUID()
        |    val key = Some(fileReference)
        |    val errorCode = Some("EntityTooSmall")
        |    val errorMessage = Some("we were instructed to reject this upload")
        |    val route = journeyRoutes.Iht401BaseController.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)
        |    val request = FakeRequest(route)
        |    val response = controller.onUploadFailure(uploadId, NormalMode, key, errorCode, errorMessage)(request)
        |    val journeyRecovery = routes.JourneyRecoveryController.onPageLoad(None)
        |    status(response) shouldBe SEE_OTHER
        |    redirectLocation(response) shouldBe Some(journeyRecovery.path())
        |  }
        |}
        |""".stripMargin
  }
}
