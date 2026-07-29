package config

import com.google.inject.AbstractModule
import controllers.CheckYourAnswersController
import controllers.actions.*
import uk.gov.hmrc.fileuploadjourney.controllers.CheckYourAnswersBaseController

import java.time.{Clock, ZoneOffset}

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()

    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))

    bind(classOf[CheckYourAnswersBaseController]).to(classOf[CheckYourAnswersController])
  }
}
