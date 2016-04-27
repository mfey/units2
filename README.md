# units2

A Clojure library for quantities with units.

## Rationale

Clojure does not have built-in support for numerical quantities in a system of units. Most (all?) previous solutions to this problem construct ad-hoc domain specific languages, instead of transforming the entire language into a DSL (which is what Lisp languages are supposed to do). This library brings Clojure into the world of unit-aware computing (https://xkcd.com/1643/), indeed **abstracts away** as much of the unit bookkeeping as possible.

This library also distinguishes itself by respecting the distinction between the algebra on quantities with units and the algebra on units themselves. This is an important prerequisite for dimensional analysis and for nonlinear unit conversions (e.g. celsius-fahrenheit).

### This solution

We provide protocols for things that are units and for things that have units ("amounts", "dimensionful quantities", ...), and a reference implementation of these protocols that augments Clojure in a functional, lispy way.

In addition to these protocols,
+ The reference implementation of units partakes in `clojure.lang.IFn`
    + A sample bunch of IFnUnits units are provided, as well as tools to quickly create your own IFnUnits
+ The reference implementation of dimensionful quantities comes with unit-aware version of `+`,`-`,`*`,`/`, etc.
    + These augmented `+-*/` are accessible both using namespace-qualified symbols (idiomatic clojure) and within the scope of a context-creating macro (idiomatic in any lisp)
    + `+-*/` also conform to the rules dimensional analysis

## More to Read

There's a "tutorial.md" file to help get started with the library and some example code in "spice.clj". Also, some effort was put into writing readable, well-commented code, so please read the Marginalia (html) output in `docs/`!

## Status / Roadmap / Features / TODO

Elements marked `(*)` are incomplete.

### Version 1.0 (minimal functionality and some syntactic sugar)

### Version 2.0 (fully functional, tested and documented)
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
    + `(*)` Exhaustive Testing
+ Comprehensive Documentation:
    + Inline Comments / Marginalia
    + Tutorial
    + Example code (`spice.clj`)

### Version 3.0 (experimental features)
+ `(*)` Interop with `core.typed` (check for correct dimensions at compile time)
+ `(*)` More cool stuff

