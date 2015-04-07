package org.indyscala.scalacheck

import org.scalatest._
import org.scalatest.prop._
import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

class ComplexSpec extends FunSuite with MustMatchers with PropertyChecks with Inside {
  // generate something complex from something simple
  val genComplex: Gen[Complex] = for {
    r <- arbitrary[Double]
    i <- arbitrary[Double]
  } yield Complex(r, i)

  // An arbitrary is an implicit generator
  implicit val arbComplex: Arbitrary[Complex] =
    Arbitrary(genComplex)

  def beWithinTolerance(d: Double) = {
    val byRatio = (be >= d * 0.999 and be <= d * 1.001)
    val byDiff = (be >= d - 1e-3 and be <= d + 1e-3)
    byRatio or byDiff
  }

  test("c * c == c.squared") {
    forAll { (c: Complex) =>
      whenever (math.abs(c.r) < 1e10 && math.abs(c.i) < 1e10) {
        (c * c) must be (c.squared)
      }
    }
  }

  test("i * i == -1") {
    (Complex(0.0,1.0) * Complex(0.0,1.0)) must be (Complex(-1.0, 0.0))
  }
}
