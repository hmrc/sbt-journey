package uk.gov.hmrc.simplejourney.generators

import _root_.generators.Generators // TODO: Remove this once we have a better template
import java.time.LocalDate
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.simplejourney.models.*

trait JourneyGenerators extends Generators {

  given Arbitrary[TaxRegime] = Arbitrary(Gen.oneOf(TaxRegime.values.toIndexedSeq))

  given Arbitrary[AuditEvent] = Arbitrary {
    for {
      auditType <- arbitrary[String]
      description <- arbitrary[String]
      expectedGoLiveDate <- arbitrary[LocalDate]
      expectedDecommissioningDate <- Gen.option(arbitrary[LocalDate])
    } yield AuditEvent(auditType, description, expectedGoLiveDate, expectedDecommissioningDate)
  }

  given Arbitrary[Choice] = Arbitrary(Gen.oneOf(Choice.values.toIndexedSeq))
}
