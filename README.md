# units2

A Clojure library for quantities with units.

## Rationale

Clojure does not have built-in support for numerical quantities in a system of units. Most (all?) previous solutions to this problem use add-on domain specific languages instead of improving the core language in a lispy way.

### Pre-existing Clojure (Java) solutions:

#### Frinj
+ flexible units, currencies
+ interprets dimensions
- DSL rather than idiomatic clojure... and the DSL is UGLY!

#### Meajure
+ Arithmetic and basic math
- No notion of `dimension`: `units` are really just keyword tags with exponents, can't convert between them easily
- Reader-macro-based DSL rather than idiomatic Clojure...

#### Minderbinder
+ defunits-of is really clean
- Again, it's a DSL

#### units (clojars)
+ Very lispy
- Idiomatically mixes units into arithmetic as multiplying factors. This blurs the important conceptual distinction between numerical values and units. This is bad.

#### JScience (JAVA)
+ flexible units, currencies
+ Units have dimensions
+ API is part of the Java Standard Library
- Not Clojure

### This solution

Provide protocols for things that are units and for things that have units ("dimensionful quantities"), and a reference implementation that augments Clojure rather than confines units to a DSL.

+ The reference implementation of units partakes in `clojure.lang.IFn`
+ The reference implementation of dimensionful quantities comes with unit-aware version of `+`,`-`,`*`,`/`, etc.

## Status / Roadmap

Elements marked `(*)` are incomplete.

### Version 1.0 (minimal functionality and some syntactic sugar)
+ Minimal (unstandardised) Implementation
+ Standard Operations (+ - pos? max etc.)
+ Integration and Differentiation (of univariate functions)
+ Minimal testing
+ Helpful Documentation

### Version 2.0 (fully tested, some experimental features)
+ Protocols standardise the behaviour across implmentations
+ Generic `amount` type
    + Standard Operations on this generic type (`units2.ops`)
    + Calculus on functions using this generic type (`units2.calc`)
+ `IFnUnit` type based on `javax.measure` (JSR 275)
    + Standard Unit Libraries for:
        + `(*)` Information and Probability Theory (`units2.prob`)
        + Astroparticle Physics (`units2.astro`)
+ Debugging:
    + `(*)` Comprehensive Warning/Error messages
    + `(*)` Exhaustive Testing
+ Comprehensive Documentation:
    + Inline Comments / Marginalia
    + Tutorial
+ `(*)` Preliminary interop with `core.typed` (check for correct dimensions at compile time)


### Version 3.0
+ `(*)` Interop with `core.typed`
+ `(*)` More cool stuff

## More to Read

There's a "tutorial.md" file to help get started with the library. Also, some effort was put into writing readable, well-commented code, so please read the Marginalia (html) output in `docs/`!
