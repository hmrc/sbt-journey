package pages

import models.Mode
import play.api.mvc.Call
import queries.{Gettable, Settable}

trait QuestionPage[A] extends Page with Gettable[A] with Settable[A] {
  override type AnswerType = A
  def submitRoute(mode: Mode): Call
}
