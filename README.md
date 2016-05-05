# units2

A Clojure library for quantities with units.


## Rationale

Few languages have built-in support for numerical quantities in a system of units. This library brings Clojure into the world of unit-aware computing (https://xkcd.com/1643/); in fact, it abstracts away as much of the unit bookkeeping as possible in a functional, lispy way.

## Features

The aims of `units2` are to be highly expressive and easy to use. The salient points of `units2` are:

+ A lispy syntax for first-class units that you can `map`, `comp`, etc.
+ New units can be defined
    + in namespaces that collect them by application, and
    + at runtime, anonymously, even within local scope
+ Augmented math ops (`+`,`-`,`*`,`/`, etc.), accessible
    + as namespace-qualified symbols (idiomatic clojure), and
    + within the scope of a context-creating macro (idiomatic in any lisp)
+ Dimensional analysis can be extended at runtime

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
    + Comprehensive Warning/Error messages
    + `(*)` Painfully Exhaustive Testing
+ Comprehensive Documentation:
    + Inline Comments / Marginalia
    + Tutorial
    + Example code (`spice.clj`)
    + `(*)` API Reference

### Version 3.0

`(*)` Objective: introduce new/experimental features

+ `(*)` Interop with `core.typed` (automated dimensional analysis at compile time)

