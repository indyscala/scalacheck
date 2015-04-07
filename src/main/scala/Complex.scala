package org.indyscala.scalacheck

case class Complex(r: Double, i: Double) {
  def *(c: Complex) = Complex(r * c.r - i * c.i, r * c.i - i * c.r)
  def squared = this * this
}
