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

import uk.gov.hmrc.sbt.journey.models.{QualifiedName, RootPage}
import uk.gov.hmrc.sbt.journey.utils.StringCaseUtils.pascalCase

object RootPageController extends Template {
  def render(
    basePackage: QualifiedName,
    pageName: String,
    rootPage: RootPage
  ): String = {
    val capitalPageName = pascalCase(pageName)
    val withDefault     = rootPage.withDefaultController
    val interfaceName   = s"${capitalPageName}BaseController"
    val defaultImplName = s"Default${capitalPageName}Controller"
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
           |  view: ${rootPage.viewClass},
           |  override val controllerComponents: MessagesControllerComponents
           |)(implicit ec: ExecutionContext) extends $interfaceName {
           |  def onPageLoad: Action[AnyContent] = identify { implicit request =>
           |    Ok(view())
           |  }
           |}
           |""".stripMargin

    s"""package ${basePackage / "controllers"}
       |
       |import controllers.actions.* // ${basePackage / "controllers.actions.*"}
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
       |  def onPageLoad: Action[AnyContent]
       |}
       |$defaultImpl""".stripMargin
  }
}
