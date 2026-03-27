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

package uk.gov.hmrc.sbt.journey.models

sealed trait Page extends Product with Serializable {
  def titleKey: String
  def headingKey: String
  def viewRoute: String
  def controllerClass: String
  def viewClass: String
}

/** A top-level page of the application.
  *
  * @param titleKey
  *   The key of the title text in the message file. Defaults to <code>${pageKey}.title</code>.
  * @param headingKey
  *   The key of the heading text in the message file. Defaults to <code>${pageKey}.heading</code>.
  * @param viewRoute
  *   The route used for rendering the page. Defaults to <code>/${page-key}</code>.
  * @param controllerClass
  *   The fully-qualified class name of the controller to use to respond to this route. Defaults to
  *   <code>${basepackage}.controllers.${PageKey}BaseController</code>.
  * @param viewClass
  *   The fully-qualified class name of the view to use to render this route. Defaults to
  *   <code>views.html.${PageKey}View</code>.
  * @param withDefaultController
  *   Whether to generate a default controller implementation for this page.
  */
case class RootPage(
  titleKey: String,
  headingKey: String,
  viewRoute: String,
  controllerClass: String,
  viewClass: String,
  withDefaultController: Boolean
) extends Page

/** A page of a journey.
  *
  * In the documentation below, the casing of default values illustrates the case transformations
  * that are applied to an input <code>pageKey</code>.
  *
  * Page keys are presumed to be in camel case, so the value <code>${page-key}</code> illustrates
  * that the camel case value will be translated to kebab case.
  *
  * @param pageKey
  *   The key of the journey page in ${journey.pages}.
  * @param titleKey
  *   The key of the title text in the message file. Defaults to <code>${pageKey}.title</code>.
  * @param headingKey
  *   The key of the heading text in the message file. Defaults to <code>${pageKey}.heading</code>.
  * @param viewRoute
  *   The route used for rendering the page in normal mode. Defaults to <code>/${page-key}</code>.
  * @param changeRoute
  *   The route used for rendering the page in check your answers mode. Defaults to
  *   <code>/change-${page-key}</code>.
  * @param controllerClass
  *   The fully-qualified class name of the controller to use to respond to this route. Defaults to
  *   <code>${basepackage}.controllers.${PageKey}BaseController</code>.
  * @param formProviderClass
  *   The fully-qualified class name of the form provider to use to create forms for this page.
  *   Defaults to <code>${basepackage}.forms.${PageKey}FormProvider</code>.
  * @param viewClass
  *   The fully-qualified class name of the view to use to render this route. Defaults to
  *   <code>views.html.${PageKey}View</code>.
  * @param withDefaultController
  *   Whether to generate a default controller implementation for this page.
  * @param withDefaultFormProvider
  *   Whether to generate a default form provider implementation for this page.
  * @param answerType
  *   The type of the answer saved by this journey page.
  */
case class JourneyPage(
  pageKey: String,
  titleKey: String,
  headingKey: String,
  viewRoute: String,
  changeRoute: String,
  controllerClass: String,
  formProviderClass: String,
  viewClass: String,
  withDefaultController: Boolean,
  withDefaultFormProvider: Boolean,
  answerType: FieldType
) extends Page
