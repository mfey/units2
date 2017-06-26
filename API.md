# units2 API -- Overview

This is not a full specification of the edge cases, the thrown errors, etc. Think of it as the API-lite or the Getting-Started-API or the Cheatsheet.

# Namespaces

## units2.core

### Dimensionful

Protocol

Method  | arguments | output | description |
------: | ---- | ---- | ---- |
AsUnit  | amount | unit | Returns a new unit in which the given amount is unity. |
getUnit | amount | unit | Returns the unit associated to the given amount. |
getValue| amount, unit | double | Returns the value of the given amount in the given unit as a double (if possible). |
to      | amount, unit | amount | Converts the given amount to the given unit (if possible) and returns it. See also `IFnUnit`. |

For all `x` and all units `U` for which `(to x U)` is well-defined, we should have `(getValue (to x U) U) ==> x`.

### Unitlike

Protocol

Method  | arguments | output | description |
------: | ---- | ---- | ---- |
compatible? | unit1, unit2 | boolean | Returns true when conversions between the two units are possible. |
from    | unit | fn: amount -> number | Returns a function `#(getValue % unit)`.
offset  | unit, amount | unit | Returns a unit offset by the given amount (if possible). |
rescale | unit, number | unit | Returns a unit linearly rescaled by the given factor. |

### units2.core.amount

Type

Implementation of `Dimensionful`. See also `IFnUnit`.

### unit-from-powers

Function

Returns a composite unit from a map of units and integer powers.

### amount?

Function (Predicate)

Returns `true` when given an `units2.core.amount` object, and `false` otherwise.

## units2.IFnUnit

### IFnUnit

Type

Implementation of `Unitlike` and of `IFn`. Applying an `IFnUnit` on a regular clojure number returns an `units2.core.amount` with this unit. Applying an `IFnUnit` on such an amount returns that amount converted into this unit.

### defunit

Macro

`def` the given var to hold the given unit, and change that unit's printed representation to that var.

### makebaseunit

Function (Constructor)

Returns a new (anonymous) unit at the base of a new dimension. See also `defbaseunit`.

### defbaseunit

Macro

A hybrid between `defunit` and `makebaseunit`.

### defunit-with-SI-prefixes

Macro

A `defunit` that also defunits all SI-prefixed units.

## units2.ops

### with-unit-, with-unit-arithmetic, with-unit-comparisons, with-unit-expts, with-unit-magnitudes

Macros

Rebind various clojure.core and java.lang.Math functions to unit-aware equivalents within the macro's scope.

### ==, <, >, <=, >=

Functions

### zero?, pos?, neg?, min, max

Functions

### +, -, *, /, rem, quot

Functions

### divide-into-double

Function

Returns the ratio of two compatible units as a double (if possible).

### exp, log , log10

Functions

### pow

Function

### expt

Function

Returns the given amount raised to the given integer power.

### abs, floor, ceil, round

Functions

### decorate-[...]

Functions

## units2.calc

### decorate-differentiator, decorate-integrator

Higher-order functions for wrapping the algebra of units around pre-existing differentiation and integration schemes.

### differentiate, integrate

Decorated versions of Incanter's derivatives and integrals that can deal with units.
