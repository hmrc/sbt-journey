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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultTaxRegimeController])
        |trait TaxRegimeBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.TaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends TaxRegimeBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime)
        |      preparedForm = userAnswers.get(page)
        |        .map(form().fill)
        |        .getOrElse(form())
        |    } yield Ok(view(preparedForm, page.submitRoute(mode), mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime)
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultWhichTaxRegimeController])
        |trait WhichTaxRegimeBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.WhichTaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends WhichTaxRegimeBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = WhichTaxRegimePage
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val page = WhichTaxRegimePage
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultVatInfoController])
        |trait VatInfoBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.VatInfoView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends VatInfoBaseController {
        |
        |  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      whichTaxRegime <- userAnswers.get(WhichTaxRegimePage)
        |      page = VatInfoPage(whichTaxRegime)
        |      preparedForm = userAnswers.get(page)
        |        .map(form().fill)
        |        .getOrElse(form())
        |    } yield Ok(view(preparedForm, page.submitRoute(mode), mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      whichTaxRegime <- userAnswers.get(WhichTaxRegimePage)
        |      page = VatInfoPage(whichTaxRegime)
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultAddAnotherTaxRegimeController])
        |trait AddAnotherTaxRegimeBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.AddAnotherTaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends AddAnotherTaxRegimeBaseController {
        |
        |  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = AddAnotherTaxRegimePage(addATaxRegime, taxRegimesIndex)
        |    } yield Ok(view(form(), page.submitRoute(mode), mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = AddAnotherTaxRegimePage(addATaxRegime, taxRegimesIndex)
        |    } yield form().bindFromRequest().fold(
        |      formWithErrors =>
        |        BadRequest(view(formWithErrors, page.submitRoute(mode), mode)),
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultTaxRegimeController])
        |trait TaxRegimeBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.TaxRegimeView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends TaxRegimeBaseController {
        |
        |  def onPageLoad(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime, taxRegimesIndex)
        |      preparedForm = userAnswers.get(page)
        |        .map(form().fill)
        |        .getOrElse(form())
        |    } yield Ok(view(preparedForm, page.submitRoute(mode), mode))
        |    result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        |  }
        |
        |  def onSubmit(taxRegimesIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val userAnswers = request.userAnswers
        |    val result = for {
        |      addATaxRegime <- userAnswers.get(AddATaxRegimePage)
        |      page = TaxRegimePage(addATaxRegime, taxRegimesIndex)
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultAddAnotherAuditEventController])
        |trait AddAnotherAuditEventBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.AddAnotherAuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends AddAnotherAuditEventBaseController {
        |
        |  def onPageLoad(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = AddAnotherAuditEventPage(auditEventsIndex)
        |    Ok(view(form(), page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = AddAnotherAuditEventPage(auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        BadRequest(view(formWithErrors, page.submitRoute(mode), mode)),
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultAuditEventController])
        |trait AuditEventBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.AuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends AuditEventBaseController {
        |
        |  def onPageLoad(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = AuditEventPage(auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val page = AuditEventPage(auditEventsIndex)
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultAddAnotherAuditEventController])
        |trait AddAnotherAuditEventBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.AddAnotherAuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends AddAnotherAuditEventBaseController {
        |
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex)
        |    Ok(view(form(), page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = AddAnotherAuditEventPage(auditSourcesIndex, auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    form().bindFromRequest().fold(
        |      formWithErrors =>
        |        BadRequest(view(formWithErrors, page.submitRoute(mode), mode)),
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

    JourneyPageController.render(
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
        |@ImplementedBy(classOf[DefaultAuditEventController])
        |trait AuditEventBaseController extends FrontendBaseController with I18nSupport {
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
        |  view: views.html.AuditEventView,
        |  override val controllerComponents: MessagesControllerComponents
        |)(implicit ec: ExecutionContext) extends AuditEventBaseController {
        |
        |  def onPageLoad(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
        |    val page = AuditEventPage(auditSourcesIndex, auditEventsIndex)
        |    val userAnswers = request.userAnswers
        |    val preparedForm = userAnswers
        |      .get(page)
        |      .map(form().fill)
        |      .getOrElse(form())
        |    Ok(view(preparedForm, page.submitRoute(mode), mode))
        |  }
        |
        |  def onSubmit(auditSourcesIndex: Int, auditEventsIndex: Int, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
        |    val page = AuditEventPage(auditSourcesIndex, auditEventsIndex)
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
}
