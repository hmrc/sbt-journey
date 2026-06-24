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

import uk.gov.hmrc.sbt.journey.models.QualifiedName

object UpscanNotificationController {
  def render(basePackage: QualifiedName): String = {
    s"""package ${basePackage / "controllers" / "upscan"}
       |
       |import play.api.libs.json.JsValue
       |import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
       |import ${basePackage / "models" / "UploadId"}
       |import ${basePackage / "models" / "upscan" / "UpscanNotification"}
       |import ${basePackage / "repositories" / "FileUploadRepository"}
       |import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
       |
       |import com.google.inject.ImplementedBy
       |import java.util.UUID
       |import javax.inject.{Inject, Singleton}
       |import scala.concurrent.{ExecutionContext, Future}
       |
       |@ImplementedBy(classOf[DefaultUpscanNotificationController])
       |trait UpscanNotificationBaseController extends FrontendBaseController {
       |  def onNotificationReceived(id: UUID): Action[JsValue]
       |}
       |
       |@Singleton
       |class DefaultUpscanNotificationController @Inject() (
       |  fileUploadRepository: FileUploadRepository,
       |  override val controllerComponents: MessagesControllerComponents
       |)(using ec: ExecutionContext) extends UpscanNotificationBaseController {
       |  def onNotificationReceived(id: UUID) = Action.async(parse.json) { implicit request =>
       |    withJsonBody[UpscanNotification] { notification =>
       |      fileUploadRepository
       |        .handleNotification(UploadId(id), notification)
       |        .map { _ => NoContent }
       |    }
       |  }
       |}
       |""".stripMargin
  }
}
