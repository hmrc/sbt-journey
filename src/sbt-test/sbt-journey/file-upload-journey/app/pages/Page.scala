package pages

import scala.language.implicitConversions

trait Page {
  type AnswerType
}

object Page {

  implicit def toString(page: Page): String =
    page.toString
}
