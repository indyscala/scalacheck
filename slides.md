% title: Intro to ScalaCheck
% subtitle: Indy Scala, April 6, 2015
% author: Ross A. Baker
% author: Principal Cloud Engineer, CrowdStrike, @rossabaker
% thankyou: Thanks everyone!
% thankyou_details: And especially these people:
% contact: <span>hosts</span> E-gineering
% contact: <span>meetup</span> Brad Fritz of Fewer Hassles
% favicon: http://www.stanford.edu/favicon.ico

---
title: How do we test?
class: img-top-center
build_lists: true

- We're all good developers here.
- We know it's not done until it's tested.
- But it's boring.
- And there's always pressure to deliver new features.
- And your tests miss corner cases anyway.
- Testing is hard.  Let's go watch basketball.

---
title: The testing continuum
class: img-top-center
build_lists: true

<img src=figures/continuum.svg />

- We are going to try to move right.
- Well, up to a point.

---
title: ¯\\(°_o)/¯-based testing
subtitle: A review
build_lists: true

- I didn't have time to write this slide.
- Because I had a Sev 1 defect.
- You know, because I didn't test.

---
title: Example-based testing
subtitle: A review
build_lists: true

<pre class="prettyprint" data-lang="scala">
"sqrt(c) * sqrt(c) == c by example" in {
   sqrt(1.0) must be (1.0)
   sqrt(9.0) must be (3.0)
   sqrt(0.25) must be (0.5)
}
</pre>

---
title: Property testing
subtitle: A preview
build_lists: true

<pre class="prettyprint" data-lang="scala">
"sqrt(c) * sqrt(c) == c" in {
  forAll { (d: Double) =>
    (sqrt(d) * sqrt(d)) must be (d)
  }
}
</pre>

---
title: Interactive theorem proving
subtitle: A hand wave
build_lists: true

<pre class="prettyprint tiny" data-lang="isabelle">
theorem sqrt2_not_rational:
  "sqrt (real 2) ∉ ℚ"
proof
  assume "sqrt (real 2) ∈ ℚ"
  then obtain m n :: nat where 
    n_nonzero: "n ≠ 0" and sqrt_rat: "¦sqrt (real 2)¦ = real m / real n"
    and lowest_terms: "gcd m n = 1" ..
  from n_nonzero and sqrt_rat have "real m = ¦sqrt (real 2)¦ * real n" by simp
  then have "real (m²) = (sqrt (real 2))² * real (n²)" by (auto simp add: power2_eq_square)
  also have "(sqrt (real 2))² = real 2" by simp
  also have "... * real (m²) = real (2 * n²)" by simp
  finally have eq: "m² = 2 * n²" ..
  hence "2 dvd m²" ..
  with two_is_prime have dvd_m: "2 dvd m" by (rule prime_dvd_power_two)
  then obtain k where "m = 2 * k" ..
  with eq have "2 * n² = 2² * k²" by (auto simp add: power2_eq_square mult_ac)
  hence "n² = 2 * k²" by simp
  hence "2 dvd n²" ..
  with two_is_prime have "2 dvd n" by (rule prime_dvd_power_two)
  with dvd_m have "2 dvd gcd m n" by (rule gcd_greatest)
  with lowest_terms have "2 dvd 1" by simp
  thus False by arith
qed
</pre>

<footer class="source">http://en.wikipedia.org/wiki/Isabelle_%28proof_assistant%29</footer>

---
title: Comparing the approaches
subtitle: Rigor of proof

- ¯\\(°_o)/¯: proof by hope
- Examples: proof by example
- Properties: proof by lack of counterexample
- Theorems: proof by, like, math and stuff

---
title: Comparing the approaches
subtitle: Time to market

- ¯\\(°_o)/¯: fast
- Examples: moderate
- Properties: moderate
- Theorems: uh, we're still researching that

---
title: Comparing the approaches
subtitle: Can I hire for this?

- ¯\\(°_o)/¯: too easily
- Examples: easily
- Properties: maybe not, but you can learn it
- Theorems: uh, we're still researching that

---
title: Comparing the approaches
subtitle: Can I do this in Scala?

- ¯\\(°_o)/¯: of course!
- Examples: ScalaTest or Specs2
- Properties: ScalaCheck, optionally with ScalaTest or Specs2
- Theorems: Isabelle, I guess?

---
title: Property testing
subtitle: A deeper look
build_lists: true

<pre class="prettyprint" data-lang="scala">
"sqrt(c) * sqrt(c) == c" in {
  forAll { (d: Double) => (sqrt(d) * sqrt(d)) must be (d) }
}
</pre>

- Says what we mean
- ScalaCheck generates <code>d</code> ...
- ... a hundred times or more ...
- ... and finds your flawed assumptions.

---
title: Our first bug
build_lists: true

<pre class="prettyprint" data-lang="scala">
[info] - sqrt(c) * sqrt(c) == c property *** FAILED ***
[info]   TestFailedException was thrown during property evaluation.
[info]     Message: NaN was not equal to -3.9898976436050957E-94
[info]     Location: (SqrtSpec.scala:19)
[info]     Occurred when passed generated values (
[info]       arg0 = -3.9898976436050957E-94
[info]     )
</pre>

- We forgot to do bounds checking.
- Our type system does not express the domain of the function. :(
- But our property can. :)

---
title: whenever
build_lists: true

<pre class="prettyprint" data-lang="scala">
"sqrt(c) * sqrt(c) == c" in {
  forAll { (d: Double) =>
    whenever (d >= 0) {
      (sqrt(d) * sqrt(d)) must be (d) 
    }
  }
}
</pre>

- Still says what we mean
- ScalaCheck still generates <code>d</code> ...
- ... and discards <code>d</code> that don't fit.

---
title: Our second bug
build_lists: true

<pre class="prettyprint" data-lang="scala">
[info] - sqrt(c) * sqrt(c) == c *** FAILED ***
[info]   TestFailedException was thrown during property evaluation.
[info]     Message: 2.2437640832940142E64 was not equal to 2.2437640832940145E64
[info]     Location: (SqrtSpec.scala:20)
[info]     Occurred when passed generated values (
[info]       arg0 = 2.2437640832940145E64
[info]     )
</pre>

- Meh.  Close enough for government work.
- Our property overpromises.
- Let's fix it again.

---
title: tolerance
build_lists: true

<pre class="prettyprint" data-lang="scala">
"sqrt(c) * sqrt(c) == c" in {
  def beWithinTolerance(d: Double) = {
    be >= d * 0.999 and be <= d * 1.001
  }

  forAll { (d: Double) =>
    whenever (d >= 0) {
      (sqrt(d) * sqrt(d)) must beWithinTolerance(d)
    }
  }
}
</pre>

---
title: victory
build_lists: true

<pre class="prettyprint">
[info] SqrtSpec:
[info] sqrt
[info] - sqrt(c) * sqrt(c) == c by example
[info] - sqrt(c) * sqrt(c) == c
[info] ScalaTest
[info] Run completed in 338 milliseconds.
[info] Total number of tests run: 2
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 2, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[info] Passed: Total 2, Failed 0, Errors 0, Passed 2
</pre>

---
title: Complex numbers

<pre class="prettyprint" data-lang="scala">
case class Complex(r: Double, i: Double) {
  def *(c: Complex) = Complex(r * c.r, i * c.i)
  def squared = this * this
}
</pre>

---
title: Generating complex types
build_lists: true

<pre class="prettyprint" data-lang="scala">
val genComplex: Gen[Complex] = for {
  r <- arbitrary[Double]
  i <- arbitrary[Double]
} yield Complex(r, i)

implicit val arbComplex: Arbitrary[Complex] =
  Arbitrary(genComplex)
</pre>

- Generate complex types (pun intended) from simple.
- ScalaCheck looks for an implicit <code>Arbitrary</code> instance.

---
title: Lots of arbitrary types built in
build_lists: false

<ul class="tinier">
<li>AnyVal</li>
<li>Array</li>
<li>BigDecimal</li>
<li>Bigint</li>
<li>Boolean</li>
<li>Byte</li>
<li>Char</li>
<li>Container</li>
<li>Date</li>
<li>Double</li>
<li>Either</li>
<li>Float</li>
<li>Function{1,2,3,4,5}</li>
<li>ImmutableMap</li>
<li>Int</li>
<li>Long</li>
<li>MutableMap</li>
<li>Number</li>
<li>Option</li>
<li>Short</li>
<li>String</li>
<li>Throwable</li>
<li>Tuple{2,3,4,5,6,7,8,9}</li>
<li>Unit</li>
<ul>

---
title: Testing our complex number
build_lists: true

<pre class="prettyprint" data-lang="scala">
test("c * c == c.squared") in {
  forAll { (c: Complex) =>
    (c * c) must be (c.squared)
  }
}
</pre>

- ScalaCheck really shines checking relations between methods.

---
title: Success

<pre class="prettyprint" data-lang="scala">
[info] ComplexSpec:
[info] - c * c == c.squared
</pre>

---
title: Lies, damned lies, and property testing

<pre class="prettyprint" data-lang="scala">
scala> Complex(0,1) * Complex(0,1)
res1: org.indyscala.scalacheck.Complex = Complex(0.0,1.0)
</pre>

- Well, that's not right at all.
- $i * i = -1$ 

---
title: How can we fix this?
subtitle: Add an example
build_lists: true

<pre class="prettyprint">
[info] ComplexSpec:
[info] complex
[info] - c * c == c.squared
[info] - i * i == -1 *** FAILED ***
[info]   Complex(0.0,1.0) was not equal to Complex(1.0,0.0) (ComplexSpec.scala:38)
</pre>

- You don't have to leave example testing behind.
- Examples are a fine way to test your properties.

---
title: A corrected Complex

<pre class="prettyprint" data-lang="scala">
case class Complex(r: Double, i: Double) {
  def *(c: Complex) = Complex(r * c.r - i * c.i, r * c.i - i * c.r)
  def squared = this * this
}
</pre>

---
title: We found a boundary condition
build_lists: true

<pre class="prettyprint">
[info] ComplexSpec:
[info] - c * c == c.squared *** FAILED ***
[info]   TestFailedException was thrown during property evaluation.
[info]     Message: Complex(NaN,NaN) was not equal to Complex(NaN,NaN)
[info]     Location: (ComplexSpec.scala:26)
[info]     Occurred when passed generated values (
[info]       arg0 = Complex(1.672687821964044E279,7.301512514377243E305)
[info]     )
[info] - i * i == -1
</pre>

- We could fix that by putting a <code>whenever</code> on our values
- Or we could switch our represenation to big decimals
- Either way, now we know how robust it is

---
title: Hey, doesn't our test look like the implementation?
build_lists: true

<pre class="prettyprint" data-lang="scala">
def squared = this * this
</pre>

<pre class="prettyprint" data-lang="scala">
(c * c) must be (c.squared)
</pre>

- It's a trap: it contributed to our false positive
- It's useful: it's a regression suite if you optimize the impl
- Testing is still an art

---
title: I'm only dabbling in Scala.
subtitle: Most major languages have a port!

<ul class="tiniest">
<li>C</li>
<li>C++</li>
<li>Chicken Scheme</li>
<li>Clojure</li>
<li>Common Lisp</li>
<li>D</li>
<li>Elm</li>
<li>Erlang</li>
<li>F#</li>
<li>Factor</li>
<li>Haskell</li>
<li>Io</li>
<li>Java</li>
<li>JavaScript</li>
<li>Node.js</li>
<li>Objective-C</li>
<li>OCaml</li>
<li>Perl</li>
<li>Prolog</li>
<li>Python</li>
<li>R</li>
<li>Ruby</li>
<li>Scheme</li>
<li>Smalltalk</li>
<li>Standard ML</li>
</ul>

---
title: Choose Your Own Adventure
subtitle: If time permits

- Exercise: Reimplement sqrt to return a Complex
- Dive deeper into generators
- Look at how cats tests with Discipline
- Go watch basketball

---
title: Some further reading

- [I Dream of Gen'ning](http://www.slideshare.net/kelseyinnis/scalacheck-2-41828057)
- [Testing Stateful Systems with ScalaCheck](http://scalacheck.org/files/scaladays2014/)
