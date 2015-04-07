package org.indyscala.scalacheck

import org.scalatest._
import org.scalatest.prop._
import org.scalactic.TolerantNumerics

class SqrtSpec extends FunSuite with MustMatchers with PropertyChecks {

  test("sqrt(c) * sqrt(c) == c by example") {
    sqrt(1.0) must be (1.0)
    sqrt(9.0) must be (3.0)
    sqrt(0.25) must be (0.5)
  }

  test("sqrt(c) * sqrt(c) == c") {
    def beWithinTolerance(d: Double) = {
      be >= d * 0.999 and be <= d * 1.001
    }

    forAll { (d: Double) =>
      whenever (d >= 0) {
        (sqrt(d) * sqrt(d)) must beWithinTolerance(d)
      }
    }
  }
}
