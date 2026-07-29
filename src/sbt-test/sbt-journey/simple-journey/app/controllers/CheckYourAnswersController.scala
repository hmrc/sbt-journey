package controllers

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AddATaxRegimeQuery, AuditSourcesQuery, DataDomainsQuery}
import uk.gov.hmrc.simplejourney.controllers.CheckYourAnswersBaseController
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.util.control.NonFatal

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView
                                          ) extends CheckYourAnswersBaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val list = SummaryListViewModel(
        rows = Seq.empty
      )

      try {
        println(request.userAnswers.get(DataDomainsQuery))
        println(request.userAnswers.get(AddATaxRegimeQuery))
        println(request.userAnswers.get(AuditSourcesQuery))
      } catch {
        case NonFatal(e) =>
          e.printStackTrace()
      }

      Ok(view(list))
  }
}
