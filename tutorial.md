# Getting Started

## Overview

There are two protocols: units should be `Unitlike` and amounts that have units should be `Dimensionful`.

The reference implementation of `Dimensionful` is the `amount` type. The reference implementation of `Unitlike` is the `IFnUnit` type (which, as its name suggests, also implements `clojure.lang.IFn`).

## Getting Started (at the REPL)

Start by loading a library of units and some standard operations defined over amounts.

    (require '[units2.astro :refer :all])
    (require '[units2.ops :as op])


# Working with IFnUnits

The unit for meters in the astro library is `m`, and an amount representing 7 meters is created by

    (m 7)

To convert an amount to another unit, just act with that unit on the given amount:

    (let [a (m 6)]
      [(km a) (m (km 0.5))]
    ) ; --> amounts of 0.006 km and 500 meters


Sometimes you may want to extract the value of an amount as a regular clojure number. In that case you'll have to fall back on the `Dimensionful` protocol and write

    (getValue amount unit)

That said, the only reason to extract a value from an amount is to interact with pre-existing clojure functions. To promote functional code, unit wrap/unwrap methods over 1-to-1 functions are part of the reference implementation:

    (let [sin (unwrap-in rad #(Math/sin %))
          atan (warp-out rad #(Math/atan %))
          angle (deg 90)]
      (atan (sin angle)))


# Mathematics with units -- the `ops` library

The usual operations (`+`,`-`,`*`,`/`,`<`,`>`,`==`,`exp`,`log`, etc.) are defined over usual numbers. The `units2.ops` namespace redefines these operations in a way that behaves correctly with units.

    (let [x (m 10)
          y (km 0.1)]
      (op/+ x y))

    (op/== (kg 1) (g 1000)) ; true

## Macros

Constantly working out of the `ops` namespace can be a bit tedious, especially for very mathy code, so there's a couple of macros that locally define +,-,*, etc to be the units2.ops/... versions to make writing math easier:

    (op/with-unit-arithmetic
      (+ (m 7) (* (m 8) 4))
    )

    (op/with-unit-comparisons
      (< (m 2) (min (cm 180) (m 0.18)))
    )

These are joined together into the super-macro `(with-unit- [keywords] body)`, which expands into (possibly nested) calls to these other macros based on the keywords.


## Exponentiation

Exponentials and logarithms can be a bit subtle, but given how often they occur in application using them should not be difficult.

Besides `expt` (a new function we'll discuss later), the usual `java.lang.math` functions are redefined over dimensionless ratios of amounts using the ages-old "`atan2`" convention `atan2(a,b) == atan(a/b)`.

    (== 3.0 (op/log10 (km 1) (m 1))) ; true

They still accept arity-one definitions over regular numbers to behave nicely in `with-unit-expts`; and intrepid users can read the source to learn about `*unit-warnings-are-errors*` and arity-one exponentitation with units.


The `expt` function (following the name of `pow` in Scheme) is a `pow` for amounts with units.

    (expt (m 1) 3) ; --> volume

## Pitfalls

Some standard operations (like `pos?`) become less meaningful. TODO: discuss this further.

# Advanced Features

Now that we've seen how to work within the reference library, let's focus on how to extend it.

## Calculus

We can do calculus with amounts by leveraging existing implementations of derivatives and integrals of functions (such as Incanter or Apache Commons). The functions in `units2.calc` handle univariate calculus for any combination of regular/dimensionful quantities in the domain/range of the function.

## Defining Custom Units

It's easy to create your own IFnUnits: they can be created with `AsUnit` or with `->IFnUnit`; they can also be bound to symbols with the `defunit` macro. TODO: expand on this.

## Extending the protocols

TODO: Say something about this.

# Closing Thoughts / Roadmap

bla bla bla
