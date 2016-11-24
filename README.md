# units2

A Clojure library for quantities with units.


## Rationale

Few languages have built-in support for numerical quantities in a system of units. This library brings Clojure into the world of unit-aware computing (https://xkcd.com/1643/); in fact, it abstracts away as much of the unit bookkeeping as possible in a functional, lispy way.

## Illustrative Code Snippet

```clojure
(with-unit-arithmetic
    (defn average [a b]
        "a function that computes an average, no matter what the units are"
        (/ (+ a b) 2)))

;; assume sec, minute, day are units in the namespace.

(let [f (comp print hour average)] ; compute average and print the result in units of hours
    (f 4 5)
    (f (minute 180) (day 0.5))
    (average 4 5)) ; --> 4.5 hours, 7.5 hours; 4.5

;; Note that dimensional analysis, unit conversions and coercions all happen automatically.
;; Also note that the unit-aware `average` behaves normally on normal clojure numbers.
```

## Features

The aims of `units2` are to be highly expressive, unintrusive, and easy to use. The salient points of `units2` are:

+ A lispy syntax for first-class units that you can `map`, `comp`, etc.
+ New units can be defined
    + in namespaces that collect them by application, and
    + at runtime, anonymously, even within local scope
+ Augmented math ops (`+`,`-`,`*`,`/`, etc.), accessible
    + as namespace-qualified symbols (idiomatic clojure), and
    + within the scope of a context-creating macro (idiomatic in any lisp)
+ Dimensional analysis can be extended by the user (even at runtime!)

This library also respects the distinction between the algebra on quantities with units and the algebra on units themselves. This is an important prerequisite for dimensional analysis and for nonlinear unit conversions (e.g. celsius-fahrenheit).

## Tutorial / Sample Code / Documentation

There's a **tutorial** to help get started with the library, and some example code in `spice.clj`. Also, some effort was put into writing readable, well-commented code, so please read the Marginalia (html) output in `docs/`!

## Roadmap / TODO

Elements marked `(*)` are incomplete.

### Version 1.0

Objective: bare minimal functionality and some syntactic sugar.

### Version 2.0

`(*)` Objective: fully featured, tested and documented library.

+ Protocols standardise the behaviour of units
+ Generic `amount` type
    + Standard Operations on this generic type (`units2.ops`)
    + Calculus on functions using this generic type (`units2.calc`)
+ `IFnUnit` type based on `javax.measure` (JSR 275)
    + Standard Unit Libraries for:
        + Baking (`units2.bake`)
        + Astroparticle Physics (`units2.astro`)
    + Support for nonlinear unit conversions
+ Debugging:
    + `(*)` Comprehensive Warning/Error messages
    + `(*)` Painfully Exhaustive Testing
+ Comprehensive Documentation:
    + Inline Comments / Marginalia
    + Tutorial
    + Example code (`spice.clj`)
    + `(*)` Docstrings
    + `(*)` API Reference

### Version 3.0

`(*)` Objective: introduce new/experimental features

+ `(*)` Interop with `clojure.spec`
