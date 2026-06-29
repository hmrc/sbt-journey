package uk.gov.hmrc.fileuploadjourney.generators

import _root_.generators.Generators // TODO: Remove this once we have a better template
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.fileuploadjourney.models.*

trait JourneyGenerators extends Generators {

  given Arbitrary[Choice] = Arbitrary(Gen.oneOf(Choice.values.toIndexedSeq))
}
