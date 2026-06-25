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

/** The journey configuration of an application.
  * @param serviceName The name of the service.
  * @param basePackage
  *   The base package of the application. This is used in the default values for page
  *   configuration. Defaults to <code>uk.gov.hmrc.${servicename}</code>.
  * @param rootPages
  *   The root pages of an application.
  * @param journeys
  *   Named journeys through the application.
  */
case class JourneyConfig(
  serviceName: String,
  basePackage: String,
  rootPages: Map[String, RootPage],
  models: Map[String, AnswerModel],
  journeys: Map[String, Journey]
)
