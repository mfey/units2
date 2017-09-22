[![Clojars Project](https://img.shields.io/clojars/v/units2.svg)](https://clojars.org/units2)

# units2

A Clojure library for units of measurement.

## Rationale

Few languages have built-in support for numerical quantities in a system of units. This library brings Clojure into the world of [unit-aware computing](https://xkcd.com/1643/); in fact, it abstracts away as much of the unit bookkeeping as possible in a functional, lispy way.

## Features

The aims of `units2` are to be highly expressive, unintrusive, and easy to use. The salient points of `units2` are:

+ A lispy syntax for first-class units that you can `map`, `comp`, etc.
+ New units can be defined
    + in reusable namespaces that collect them by application, and
    + at runtime, anonymously, even within local scope
+ Augmented math ops (`+`,`-`,`*`,`/`,`==`,  etc.), accessible
    + as namespace-qualified symbols (idiomatic clojure), and
    + within the scope of a context-creating macro (idiomatic in any lisp)
+ Dimensional analysis can be extended by the user (even at runtime!)

This library also respects the distinction between the algebra on quantities with units and the algebra on units themselves. This is an important prerequisite for dimensional analysis, nonlinear unit conversions (e.g. celsius-fahrenheit), and automatic 'unitification' of custom arithmetic operations and/or numerical differentiation and integration schemes.


## Tutorial / Sample Code / Documentation

There's a [tutorial](https://github.com/mfey/units2/blob/master/tutorial.md) to help get started with the library, and some example code in `spice.clj`. There's also a summary of the API (more of a cheatsheet really). Some effort was put into writing readable, well-commented code, but if you're impatient, here's the executive summary:

```clojure
;; core utilities, a collection of units and math ops

(require '[units2.core :refer :all])
(require '[units2.stdlib :refer :all])
(require '[units2.ops :as ops])

;; a function that computes an average, no matter what the units are

(ops/with-unit-arithmetic
    (defn average [numlist]
        (/ (apply + numlist) (count numlist))))

;; sec (second), minute, hour are units from the stdlib.

(def self-reported-half-marathon-times
    {:Bob        (hour 3.5)
     :Claire     (minute 203)
     :JJ         (ops/+ (minute 64) (sec 52)) ;; April 2017 world record
     :Chuck-Norris   (hour -3)  ;; The finish line ran towards him
     })

;; compute average and return the result (in units of hours)
((comp hour average vals) self-reported-half-marathon-times) ; --> 1.24 hours

;; Wait a minute... let's exclude Chuck Norris from the average

(Thread/sleep (getValue (minute 1) msec)) ; literally wait one minute

(let [realistic (fn [x] (filter #(ops/> % (minute 0)) x))]
    ((comp hour average realistic vals)
      self-reported-half-marathon-times)) ; --> 2.65 hours

;; 2.65 hours still hard to read because our clocks use sexagesimals.
;; Let's split this into hours and minutes (by hand, just for illustration)
((juxt ops/quot (comp minute ops/rem))
 (hour 2.65) (hour 1) (hour 1)) ; --> [(hour 2.0) (minute 39)]

```


## Etymology of *Unit*

No Clojure library is complete without a bit of etymology.

*Unit* is an alteration of *unity*, from Old French *unite* ("uniqueness, oneness", c. 1200), from the Latin *unitas* (cf. below), itself derived from *unus* ("one"). *Unit* was popularized by the English commented translation of Euclid's Elements (J. Dee and H. Billingsey, 1570), to express the Greek *monos* ("unique, solitary").

Extended sense of "a quantity adopted as a standard of measure" is from 1738 (!).

Originally *unitas*, *unitatis*, (f), "oneness, sameness, agreement". More explicitly:

1. quality of that which has no parts, or forms a whole out of parts
2. quantity considered as an elementary piece for constructing all others
3. quantity chosen as a term of comparison, to evaluate others of the same sort
4. conformity of sentiment, accord, harmony

(The material above was adapted from [etymonline.com](http://www.etymonline.com/index.php?term=unit); claim (!) has not been independently verified.)
