package uk.gov.hmrc.simplejourney.generators

import _root_.generators.Generators // TODO: Remove this once we have a better template
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.simplejourney.models.*

import java.time.{Instant,LocalDate,ZoneOffset}

trait JourneyGenerators {
  // Empty strings are not valid for form binding
  given Arbitrary[String] = Arbitrary(Gen.nonEmptyBuildableOf[String, Char](Arbitrary.arbChar.arbitrary))
  // Instant.MIN and Instant.MAX can't be serialized by hmrc-mongo as they're out of range for Long
  given Arbitrary[Instant] = Arbitrary(Gen.choose(Instant.ofEpochMilli(Long.MinValue), Instant.ofEpochMilli(Long.MaxValue)))
  // LocalDate is converted to Instant before it's serialized by hmrc-mongo
  given Arbitrary[LocalDate] = Arbitrary(arbitrary[Instant].map(_.atZone(ZoneOffset.UTC).toLocalDate()))

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
