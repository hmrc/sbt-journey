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

import uk.gov.hmrc.sbt.journey.models.{AnswerModel, CaseClassModel, EnumModel, QualifiedName}

object CustomModel {
  def render(basePackage: QualifiedName, model: AnswerModel): String = model match {
    case model: CaseClassModel => caseClassModel(basePackage, model)
    case model: EnumModel      => enumModel(basePackage, model)
  }

  private[templates] def enumModel(basePackage: QualifiedName, enumModel: EnumModel): String = {
    val modelsPackage = basePackage / "models"
    val name          = enumModel.name
    val cases         = enumModel.cases

    s"""package $modelsPackage
       |
       |import play.api.libs.json.{Json, JsonValidationError, Format, Reads, Writes}
       |
       |enum $name {
       |  case ${cases.mkString(", ")}
       |}
       |
       |object $name {
       |  private val labels = values.map(_.toString)
       |
       |  given reads: Reads[$name] = Reads.of[String]
       |    .filter(JsonValidationError("error.invalid"))(labels.contains)
       |    .map($name.valueOf)
       |
       |  given writes: Writes[$name] = Writes.of[String].contramap(_.toString)
       |
       |  given Format[$name] = Format(reads, writes)
       |
       |  def fromString(value: String): Option[$name] =
       |    values.find(_.toString == value)
       |}
       |""".stripMargin
  }

  private[templates] def caseClassModel(
    basePackage: QualifiedName,
    caseClassModel: CaseClassModel
  ): String = {
    val modelsPackage = basePackage / "models"
    val name          = caseClassModel.name
    val fields        = caseClassModel.fields

    val importPrefixes = Imports.importedSymbols(fields)
    val imports        = Imports.importsFor(modelsPackage, importPrefixes)
    val extendsClause  = FormatTraits.extendsClause(importPrefixes)

    s"""package $modelsPackage
       |
       |import play.api.libs.json.{Json, Format}
       |$imports
       |
       |case class $name(
       |${fields.map((ModelFields.field _).tupled).mkString("," + System.lineSeparator())}
       |)
       |
       |object $name $extendsClause{
       |  given Format[$name] = Json.format[$name]
       |}
       |""".stripMargin
  }

}
