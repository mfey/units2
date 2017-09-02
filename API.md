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

### parse-unit

Function

arguments | output |
------: | ---- |
map     | unit |
alist   | unit |
string  | unit |

Returns a composite unit from a map of units and integer powers.

### amount?

Function (Predicate)

Returns `true` when given an `units2.core.amount` object carrying a unit that is `Unitlike`, and `false` otherwise.

## units2.IFnUnit

### IFnUnit

Type

Implementation of `Unitlike` and of `IFn`.

arguments | output |
------: | ---- |
number  | amount |
amount  | amount |

Applying an `IFnUnit` on a regular clojure number returns an `units2.core.amount` with this unit. Applying an `IFnUnit` on such an amount returns that amount converted into this unit.

### defunit

Macro

arguments | output |
------: | ---- |
var, unit | var |

`def` the given var to hold the given unit, and change that unit's printed representation to that var. Returns the given var for consistency with `def`, `defn`, `defmacro`.

### makebaseunit

Function (Constructor)

arguments | output |
------: | ---- |
string | unit |

Returns a new (anonymous) unit at the base of a new dimension. See also `defbaseunit`.

### defbaseunit

Macro

A hybrid between `defunit` and `makebaseunit`.

### defunit-with-SI-prefixes, defunit-with-IEC-prefixes

Macro

arguments | output |
------: | ---- |
var, unit | var |

A `defunit` that also defunits all SI/IEC prefixed units. Returns the given var for consistency with `def`, `defn`, `defmacro`.

## units2.ops

### with-unit-, with-unit-arithmetic, with-unit-comparisons, with-unit-expts, with-unit-magnitudes

Macros

Rebind (lexically shadow) various clojure.core and java.lang.Math functions to unit-aware equivalents within the macro's scope.

### ==, <, >, <=, >=

Functions

### zero?, pos?, neg?, min, max

Functions

### +, -, *, /

Functions

### divide-into-double

Function

 arguments | output |
------: | ---- |
amount, amount | double |

Returns the ratio of two compatible units as a double (if possible).

### exp, log , log10, sqrt, pow

Functions

These exponentiation functions are inspired by methods of `java.lang.Math`. They should only be defined on dimensionless quantities (to see why, just imagine Maclaurin-expanding the `exp` or `ln` functions).

We define them over ratios of amounts using the ages-old "`atan2`" convention `atan2(a,b) == atan(a/b)`. For instance,

```clojure
(exp a) ; ==> (Math/exp a)
(exp a b) ; ==> (Math/exp (divide-into-double a b))
(pow a n) ; ==> (Math/pow a n)
(pow a b n) ; ==> (Math/pow (divide-into-double a b) n)
```

### expt

Function

Returns the given amount raised to the given integer power.

### abs, floor, ceil, round, rem, quot

Functions

These modular arithmetic functions are inspired by methods of `java.lang.Math`.

### decorate-[...]

Functions

## units2.calc

### decorate-differentiator, decorate-integrator

Functions

Higher-order functions for wrapping the algebra of units around pre-existing differentiation and integration schemes.

Name    | arguments | output | description |
------: | ---- | ---- | ---- |
decorate-differentiator | fn : f(number), number , arglist -> number | fn\* | Decorate a differentiation scheme|
decorate-integrator | fn : f(number), [number number] , arglist -> number | fn\* | Decorate an integration scheme|

where the shape of a decorated fn\* is illustrated below.

### differentiate, integrate

Functions

Decorated versions of naive differentiation and integration schemes that can deal with units. These are provided for convenience/prototyping, and should be replaced by better (user-provided) schemes in production.

In what follows, let "amount(X)" denote "an `amount` carrying units of `X`".

Name    | arguments | output | description |
------: | ---- | ---- | ---- |
differentiate | fn: amount(A) -> amount(B), amount(A) , [amount(A)] | amount(B/A) | Returns the (first-order-forward) derivative of a function |
 || fn: amount(A) -> number, amount(A) , [amount(A)] | amount(1/A) | Returns the (first-order-forward) derivative of a function |
|| fn: number -> amount(A), number , [number] | amount(A) | Returns the (first-order-forward) derivative of a function |
 || fn: number -> number, number , [number] | number | Returns the (first-order-forward) derivative of a function |
integrate | fn: amount(A) -> amount(B), [amount(A) amount(A)] , [number] | amount(BA) | Returns the definite integral of a function over an interval|
| | fn: amount(A) -> number, [amount(A) amount(A)] , [number] | amount(A) | Returns the definite integral of a function over an interval|
| | fn: number -> amount(A), [number number] , [number] | amount(A) | Returns the definite integral of a function over an interval|
| | fn: number -> number, [number number], [number] | number | Returns the definite integral of a function over an interval|
