# Getting Started (at the REPL)

Start by loading the core of the library and some commonly used units.

    (require '[units2.core :refer :all])
    (require '[units2.stdlib :refer :all])

## Creating Amounts

The unit for meters in the standard library is `m`, and amounts representing 7 meters and 2 kilometers are created by

    (m 7)
    (km 2)

which admittedly may not look like much at the REPL because of how amounts are printed. It's easy to check that these are actually amounts (not lists), though:

    (class (celsius 16))

Both units and amounts are first-class citizens of the language. You can check that units from the standard library are of type `units2.IFnUnit.IFnUnit`, which we'll discuss in more detail later. For now, remember only that they appear in the first position of prefix notation since they implement `clojure.lang.IFn`:

    (map ifn? [celsius km]) ; --> [true true]

Therefore, to convert an amount to another unit, just `apply` that unit on the given amount:

    (fahrenheit (celsius 0))

    (let [a (m 6)]
      [(km a) (m (km 0.5))]
    ) ; --> amounts of 0.006 km and 500 meters

When the conversion does not exist, the library throws an exception, telling you that your code does not respect the rules of dimensional analysis:

    (m (celsius 5)) ; --> Helpful Exception

Sometimes you may want to extract the value of an amount as a regular clojure number. In that case you'll have to fall back on the `units2.core.Dimensionful` protocol and write

    (getValue (km 6) cm)
    (-> (fahrenheit 76) (getValue celsius))

That said, the only reason to extract a value from an amount is to interact with pre-existing clojure functions. To promote functional code, we recommend the following `comp`-based wrap/unwrap idioms:

    (let [atan (comp rad #(Math/atan %))
          sin  (comp #(Math/sin %) (from rad))
          angle (deg 90)]
      (atan (sin angle)))

where `(from <unit>)` is just syntactic sugar for `#(getValue % <unit>)`.

# Mathematics with units -- the `ops` library

The usual operations (`+`,`-`,`*`,`/`,`<`,`>`,`==`,`exp`,`log`, etc.) are defined over usual numbers. The `units2.ops` namespace redefines these operations in a way that behaves correctly with units:

    (require '[units2.ops :as op])

    (let [x (m 10)
          y (km 0.1)]
      (op/+ x y))

    (op/== (kg 1) (g 1000)) ; --> true

It's recommended to namespace-qualify these operations; however they fall back to the usual ops from `clojure.core` when acting on regular numbers, so `[units2.ops :refer :all]` is safe too.

## Macros

Constantly working out of the `ops` namespace can be a bit tedious, especially for very mathy code, so there's a couple of macros that locally define `+`, `-`, `*`, etc. to be the `units2.ops/` versions, to make writing math easier:

    (op/with-unit-arithmetic
      (+ (m 7) (* (m 8) 4))
    ) ;; 39 meters

    (op/with-unit-comparisons
      (< (m 2) (min (cm 180) (m 0.18)))
    ) ;; false

These are joined together into the super-macro `(with-unit- [keywords] body)`, which expands into (possibly nested) calls to these other macros based on the keywords. These context-macros are the recommended way to do math with units. Indeed, in functionally-written code they essentially abstract away **ALL** of the unit bookkepping:

    (op/with-unit-arithmetic
      (defn average [& xs]
        (/ (apply + xs) (count xs))) ;; vanilla Clojure code
    )

    ;;; 100 lines later

    (average (m 7) (m 6) (cm 60))     ; units work automatically
    (average (celsius 7) (celsius 6)) ; no matter their dimension
    (average 300 400 500)   ; and the no-units equivalent is free

Of course, you'd reach the same level of abstraction with `[units2.ops :refer [...]]`, but syntactically limiting redefinitions of the core language operations and explicitly mentioning them where they are necessary is probably a good idea.

## Exponentiation

Exponentials and logarithms can be a bit subtle; but given how often they occur in code with units, using them should not require re-inventing the wheel.

Besides `expt` (a new function we'll discuss soon), the usual `java.lang.math` functions are redefined over dimensionless ratios of amounts using the ages-old "`atan2`" convention `atan2(a,b) == atan(a/b)`.

    (== 12.0 (op/log10 (km 1) (nm 1))) ; true since 1km = 1000000000000 nm (twelve zeros)

They still accept arity-one definitions over regular numbers to behave nicely in `with-unit-expts`.

The `expt` function (borrowing the name of `pow` in the LISP language Scheme) is a `pow` for amounts with units:

    (op/expt (km 1) 3) ; --> large volume
    (op/expt (m 1) -2) ; --> small surface density

## Decorating your own ops

`units2` provides you with decorated versions of `clojure.core` and `java.lang.Math` ops out of the box; but it's also easy to add unit-awareness to ops from other namespaces/libraries.

TODO: Discuss decorators

# Defining Custom IFnUnits

`units2.stdlib` obviously doesn't contain each and every unit you might encounter when working with Clojure. Fortunately, it's easy to create your own IFnUnits (either at the top level of a namespace if you need them often, or just within a `let` scope if you don't).

## core

Sticking to the `units2.core` protocols, you can `rescale` or `offset` existing units, and you can turn any `amount` you've computed into a unit with `AsUnit`.

    (rescale (offset celsius (celsius -17.777)) (/ 5 9)) ;; fahrenheit
    (let [foot (AsUnit (cm 30))] (foot (m 1))) ;; how many feet in a meter?

IFnUnits are also `Multiplicative`, so you can combine existing units with `times` and `divide`. However, the recommended way to combine `Multiplicative` units is to use the `parse-unit` (alias `unit-from-powers`) function:

    (parse-unit {kg 1 m 2 sec -2}) ; Joule from an EDN specification
    (let [dimension 3] (parse-unit [m dimension sec 1])) ; a spacetime 4-volume

Note that these new units are first-class and anonymous. For work at the REPL, or for units you'll want to use throughout your entire code, units can also be conveniently bound to symbols with the `defunit` macro, so that the printed representation of a unit matches the symbol it's bound to.

## `makebaseunit`

When the units you care about have no relation to any existing unit, it's necessary to make them from scratch. You can (and should!) create new base units to express your problem. `makebaseunit` will create a new unit (along with a new dimension for the purpose of dimensional analysis). Imagine you're working on a program in the general problem space of human happiness. These things can't be measured in the SI, but

    (defunit hedon (makebaseunit "Happy")) ; the hedon is a unit of happiness (dimensions of [H])
    (defunit nap   (makebaseunit "sleep")) ; the nap is a (poorly-named?) measure of how deep one's sleep is.
    (defunit restfulness (divide hedon nap))

    (let [fullnight (op// (hedon 10) (nap 8))
          powernap  (op// (hedon 1)  (nap 0.5))]
      (map restfulness [fullnight powernap])) ;; inspect: which is more restful?


# Offset Pitfalls

Some standard operations (like `pos?`) become less meaningful with units. For example: the Celsius and Fahrenheit scales are offset, so that some temperatures are positive and negative depending on which unit you're quoting them in:

    (fahrenheit (celsius -10)) ;; +14 Fahrenheit
    (op/pos? (fahrenheit 14)) ;; -> Helpful Exception

This exception is meant to trigger some thought about the meaning one wants to assign to the boolean returned by `pos?`. Since negative temperatures in the celsius scale are linked to the freezing point of water, either of the following are probably what was meant:

    (op/> (fahrenheit 14) (celsius 0))
    (clojure.core/pos? (getValue (fahrenheit 14) celsius))

However, operations like `pos?` and `zero?` are not the end of our worries. When all conversions between units are linear rescalings, arithmetic works fine; but when offsets are involved, even basic arithmetic may become ill-defined. There are checks that `+`, `-`, and `divide-into-double` don't give nonsense, e.g.

    (+ (fahrenheit 1) (celsius 1)) ;; --> Helpful Exception

but there are still plenty of ways to combine units and make the underlying `javax.measure` implementation of unit conversions throw an `Exception`. Without even using `ops`, one could engineer the following:

    ;; temperature gradient conversions
    ((divide celsius mm) ((divide celsius m) 1)) ;; successful conversion (linear)
    ((divide fahrenheit m) ((divide celsius m) 1)) ;; unsuccessful conversion (nonlinear)


# Advanced Features

## Calculus

The functions in `units2.calc` handle numerical univariate calculus (differentiation and integration) for any combination of regular/dimensionful quantities in the domain/range of the function.

If you're not too picky about which numerical algorithms are being used behind the scenes, just use `differentiate` and `integrate`:

    (require '[units2.calc :refer :all])

    (integrate (fn [x] (* x 2)) [0 1] [1e3]) ;; 1
    (integrate (fn [x] (op/* x (sec 2))) [0 1] [1e3]) ;; 1 second
    (integrate (fn [x] (op/* x 2)) (map m [0 1]) [1e3]) ;; 1 square-meter

    (differentiate (fn [x] (op/* x x (m 2))) 1 [1e-4]) ;; 4 meters

The extra `[]` are for sending extra arguments to the underlying algorithms.

If you want to use a different algorithm for differentiation or integration, it's easy to wrap the algebra of units around existing implementations using the functions `decorate-differentiator` and `decorate-integrator`. Examples are provided in the source of `units2.calc`.

## Spec Interop

In `units2.stdlib` are also defined a variety of specs such as `::length`, `::time`, etc., to do dimensional analysis with specs:

    (require '[clojure.spec :as spec])
    (spec/valid? :units2.stdlib/length (m 7)) ; --> true
    (spec/valid? :units2.stdlib/energy (celsius 7)) ; --> false

These can also be used to generate amounts of the given dimension(s):

    (require '[clojure.spec.gen :as gen])
    (repeatedly 5 #(gen/generate (spec/gen :units2.stdlib/time)))
    (gen/generate (spec/gen (spec/or :l :units2.stdlib/length
                                     :t :units2.stdlib/time)))

Note that the generated amounts are (for the moment?) all in the base unit of the given dimension.

These specs automatically `derive` into `:units2.core/amount` and, are used in its generator:

    (descendants :units2.core/amount)
    (repeatedly 5 #(gen/generate (spec/gen :units2.core/amount)))

This feature is relevant for generatively testing functions that are unit-aware and have specs:

    (spec/exercise-fn 'units2.ops/*)

There's also support for a more advanced `defbaseunit`, which automatically connects dimensional analysis to `clojure.spec`:

    (defbaseunit U ::dim)
    (spec/exercise ::dim)

However, these features are still relatively incomplete and unsafe.

## Extending the protocols

The `IFnUnit` implementation of the `Unitlike` protocol is meant to cover most use cases; however some users may require higher numerical precision or speed than offered by the underlying `javax.measure` implementation. The functions in `ops` and `calc` were written with such user extensions in mind, and should work with other implementations of `Unitlike` with little to no changes. For instance, it's possible to wrap the algebra of units around your own definitions of `+`, `*`, `==`, `>`, using the many `decorate-` functions in the source of `units2.ops`.

# Closing Thoughts

Some effort was put into writing readable, well-commented code. Please read the Marginalia (html) output in `docs/` and the sample code `spice.clj`!
