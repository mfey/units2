# Getting Started

## Getting Started (at the REPL)

Start by loading the core of the library and some commonly used units.

    (require '[units2.core :refer :all])
    (require '[units2.astro :refer :all])

# Working with IFnUnits

The unit for meters in the astro library is `m`, and amounts representing 7 meters and 2 kilometers are created by

    (m 7)
    (km 2)

which admittedly may not look like much at the REPL because of how amounts are printed. It's easy to check that these are actually amounts (not lists), though:

    (class (celsius 16))

Both units and amounts are first-class citizens of the language. You can check that units from the astro library are of type `units2.IFnUnit.IFnUnit`, which we'll discuss in more detail later. For now, know only that they are named as such since they implement `clojure.lang.IFn`.

To convert an amount to another unit, just act with that unit on the given amount:

    (fahrenheit (celsius 0))

    (let [a (m 6)]
      [(km a) (m (km 0.5))]
    ) ; --> amounts of 0.006 km and 500 meters


When the conversion does not exist, the library throws an exception, telling you that your code does not respect the rules of dimensional analysis:

    (m (celsius 5)) ; --> Helpful Exception

Sometimes you may want to extract the value of an amount as a regular clojure number. In that case you'll have to fall back on the `units2.core.Dimensionful` protocol and write

    (getValue (km 6) cm)

That said, the only reason to extract a value from an amount is to interact with pre-existing clojure functions. To promote functional code, unit wrap/unwrap methods over 1-to-1 functions are part of the reference implementation:

    (let [sin (unwrap-in rad #(Math/sin %))
          atan (wrap-out rad #(Math/atan %))
          angle (deg 90)]
      (atan (sin angle)))


# Mathematics with units -- the `ops` library

The usual operations (`+`,`-`,`*`,`/`,`<`,`>`,`==`,`exp`,`log`, etc.) are defined over usual numbers. The `units2.ops` namespace redefines these operations in a way that behaves correctly with units:

    (require '[units2.ops :as op])

    (let [x (m 10)
          y (km 0.1)]
      (op/+ x y))

    (op/== (kg 1) (g 1000)) ; true

It's recommended to namespace-qualify these operations; however they fall back to the usual ops from `clojure.core` when acting on regular numbers, so `[units2.ops :refer :all]` would be safe too (unless another namespace also redefines `+-*/`).

## Macros

Constantly working out of the `ops` namespace can be a bit tedious, especially for very mathy code, so there's a couple of macros that locally define `+`, `-`, `*`, etc to be the units2.ops/... versions to make writing math easier:

    (op/with-unit-arithmetic
      (+ (m 7) (* (m 8) 4))
    ) ;; 39 meters

    (op/with-unit-comparisons
      (< (m 2) (min (cm 180) (m 0.18)))
     ;; false

These are joined together into the super-macro `(with-unit- [keywords] body)`, which expands into (possibly nested) calls to these other macros based on the keywords. These context-macros are the recommended way to do math with units. Indeed, in functionally-written code they essentially abstract away **ALL** of the unit bookkepping:

    (op/with-unit-arithmetic
      (defn average [& xs]
        (/ (apply + xs) (count xs))) ;; vanilla Clojure code
    )

    ;;; 100 lines later

    (average (m 7) (m 6) (cm 60)) ;; units work automatically

Of course, you'd reach the same level of abstraction with `[units2.ops :refer :all]`, but explicitly mentioning redefinitions of the core language is probably a good idea.

## Exponentiation

Exponentials and logarithms can be a bit subtle; but given how often they occur in application, using them should not require re-inventing the wheel.

Besides `expt` (a new function we'll discuss soon), the usual `java.lang.math` functions are redefined over dimensionless ratios of amounts using the ages-old "`atan2`" convention `atan2(a,b) == atan(a/b)`.

    (== 3.0 (op/log10 (km 1) (m 1))) ; true since 1km = 1000 m (three zeros)

They still accept arity-one definitions over regular numbers to behave nicely in `with-unit-expts`; and intrepid users can read the source to learn about `*unit-warnings-are-errors*` and arity-one exponentiation with units.


The `expt` function (following the name of `pow` in Scheme) is a `pow` for amounts with units:

    (op/expt (m 1) 3) ; --> volume

## Pitfalls

Some standard operations (like `pos?`) become less meaningful. For example: the Celsius and Fahrenheit scales are offset, so that some temperatures are positive and negative depending on which unit you're quoting them in:

    (fahrenheit (celsius -10)) ;; +14 Fahrenheit
    (op/pos? (fahrenheit 14)) ;; -> Helpful Exception

This exception is meant to trigger some thought about the meaning one wants to assign to the boolean returned by `pos?`. Since negative temperatures in the celsius scale are linked to the freezing point of water, either of the following are probably what was meant:

    (op/< (fahrenheit 14) (celsius 0))
    (clojure.core/pos? (getValue (fahrenheit 14) celsius))

Dauntless users can once again access more powerful version of `pos?`, `neg?`, etc., by turning off these warnings:

    (binding [units2.ops/*unit-warnings-are-errors* false]
      (op/neg? (celsius (fahrenheit 14)))) ;; true

# Advanced Features

## Calculus

We can do calculus with amounts by leveraging existing implementations of derivatives and integrals of functions (such as Incanter or Apache Commons). The functions in `units2.calc` handle univariate calculus for any combination of regular/dimensionful quantities in the domain/range of the function.

    (require '[units2.calc :refer :all])

    (integrate (fn [x] (* x 2)) [0 1]) ;; 1
    (integrate (fn [x] (op/* x (sec 2))) [0 1]) ;; 1 second
    (integrate (fn [x] (op/* x 2)) (map m [0 1])) ;; 1 square-meter

    (differentiate (fn [x] (op/* x x (m 2))) 1) ;; 4 meters

## Defining Custom IFnUnits

`units2.astro` and `.bake` don't contain many commonly-used units, let alone every unit you might encounter when working with Clojure. Fortunately, it's easy to create your own IFnUnits.

### core

Sticking to the `units2.core` protocols, you can `rescale` or `offset` existing units, and you can turn any `amount` you've computed into a unit with `AsUnit`.

    (def f1 (rescale (offset celsius (celsius -17.777)) (/ 5 9))) ;; fahrenheit
    (def f2 (AsUnit (cm 30))) ;; foot

IFnUnits are also `Multiplicative`, so you can combine existing units with `times` and `divide`. However, the recommended way to combine `Multiplicative` units is to use the `unit-from-powers` function:

    (unit-from-powers {kg 1 m 2 sec -2}) ; Joule (from map)
    (unit-from-powers [kg 1 m 2 sec -2]) ; Joule (from alist)

### `SI` and `NonSI`

If the unit you want to define is a commonly used unit, then it might be a member of the `SI` or `NonSI` classes of `javax.measure.unit`. In that case, you can simply `import` these and call the `deftype`-constructor for `IFnUnit`:

    (new units2.IFnUnit.IFnUnit SI/METER)
    ;; or (units2.IFnUnit/->IFnUnit SI/WATT)

in a `let` scope. For work at the REPL, units can also be bound to symbols with the `defunit` macro, so that the printed representation of a unit matches the symbol it's bound to.

### `makebaseunit`

When the units you care about have no relation to any existing unit, it's necessary to make them from scratch. You can (and should!) create new base units to express your problem. `makebaseunit` will create a new unit (along with a new dimension for the purpose of dimensional analysis). Imagine you're working on a program in the general problem space of human happiness. These things can't be measured in the SI, but

    (defunit hedon (makebaseunit "Happy")) ; the hedon is a unit of happiness (dimensions of [H])
    (defunit nap   (makebaseunit "sleep")) ; the nap is a (poorly-named?) measure of how deep one's sleep is.
    (defunit restfulness (divide hedon nap))

    (let [fullnight (op// (hedon 10) (nap 8))
          powernap  (op// (hedon 1)  (nap 0.5))]
      (op/> fullnight powernap)) ;; which is more restful?



## Extending the protocols

The `IFnUnit` implementation of the `Unitlike` protocol is meant to cover most use cases; however some users may require higher numerical precision or speed than offered by the underlying `javax.measure` implementation. The functions in `ops` and `calc` were written with such user extensions in mind, and should work with other implementations of `Unitlike` with little to no changes.

# Closing Thoughts

Some effort was put into writing readable, well-commented code. Please read the Marginalia (html) output in `docs/` and the sample code `spice.clj`!
