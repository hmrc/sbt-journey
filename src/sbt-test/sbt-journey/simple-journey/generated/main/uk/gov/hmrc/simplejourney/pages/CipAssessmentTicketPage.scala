package uk.gov.hmrc.simplejourney.pages

import _root_.pages.* // TODO: Remove this once we have a better template
import play.api.libs.json.JsPath


object CipAssessmentTicketPage extends QuestionPage[String] {
  override def path: JsPath = JsPath \ "cipAssessmentTicket"
  override def toString: String = "cipAssessmentTicket"
}
