(ns units2.core
  (:require [clojure.core.typed :as t]))

(set! *warn-on-reflection* true)

;; As a physicist, I use an analogy to gauge theory to think about code with units.

;; (t/ann-protocol ^:no-check Dimensionful
;;   getUnit [Object :-> Unitlike]
;;   to (t/All [x] [x Unitlike :-> x])
;;   getValue [Object Unitlike :-> t/Any]
;;   AsUnit [Object :-> t/Any]
;; )

(defprotocol Dimensionful
  "The values represented by dimensionful datatypes are gauged under unit transformations. If 1km = 1000 meters are the same quantity under different but redundant descriptions, then we want our code to be invariant (ie not need rewriting) under transformations of the variables from one set of units to another."
  (getUnit [this] "get the unit of")
  (to [this Unit] "convert this to the given unit.")
  (getValue [this Unit] "convert this to the given unit and return the value only.")
  (AsUnit [this] "return a unit in which this quantity is unity")
)

;; (t/ann-protocol ^:no-check Unitlike
;;   getDimension [Object :-> t/Any]
;;   compatible? [Object Object :-> t/Bool]
;;   getConverter [Object Object :-> t/IFn]
;;   rescale (t/All [x] [x t/Num :-> x])
;; )

(defprotocol Unitlike
  "Defines the behaviour of a unit"
  (getDimension [this] "get the dimension of")
  (compatible? [this that] "Do two units have the same dimension? If so, they are `compatible?'")
  (getConverter [this that] "Returns a function that describes the unit transformation, as it acts on values... mostly for internal use.")
  (rescale [this number] "Returns a unit linearly rescaled by the given factor") ;; Not 100% necessary, but nice to have
)

;; (t/ann-protocol Hackable
;;   implementation-hook [Object :-> Object])

(defprotocol Hackable
  (implementation-hook [this] "this may be useful for implementors, but should NOT be used in applications.") ;; this should be hidden from users... right?
  )

;; (t/ann-protocol ^:no-check Wrappable
;;   wrap-in [Object t/IFn :-> t/IFn]
;;   wrap-out [Object t/IFn :-> t/IFn]
;;   unwrap-in [Object t/IFn :-> t/IFn]
;;   unwrap-out [Object t/IFn :-> t/IFn]
;; )

(defprotocol Wrappable
  "Defines the behavior for a function that can be wrapped/unwrapped around other functions. In the context of units, it is useful for mixing gauge-invariant (user) and gauge-dependent (library) code.
  This utility is provided for one-to-one functions."
  (wrap-in [this f] "Returns a function that converts an amount into a given unit, before passing it to the first argument of the supplied function.")
  (wrap-out [this f] "Returns a function that converts an amount into a given unit, before returning it.")
  (unwrap-in [this f] "Returns a function that extracts the value of an amount into a given unit, before passing it to the first argument of the supplied function.")
  (unwrap-out [this f] "Returns a function that extracts the value of an amount in the given unit, before returning it.")
  )


;; (t/ann-protocol ^:no-check Multiplicative
;;   times (t/All [x] (t/IFn [x :-> x] [x x :-> x] [x x x :-> x]))
;;   divide (t/All [x] (t/IFn [x :-> x] [x x :-> x] [x x x :-> x]))
;;   inverse [Object :-> Object]
;;   power (t/All [x] [t/AnyInteger :-> x])
;; )

(defprotocol Multiplicative
  (times [this] [this that]
         [this that the-other]) ;Seinfeld reference
  (divide [this] [this that] [this that the-other])
  (inverse [this] "syntactic sugar for arity-1 (divide ...)")
  (power [this N] "syntactic sugar for all of the above")
)


;; (t/ann ^:no-check unit-from-powers [t/Any :-> Unitlike])


(defn unit-from-powers
"Returns a composite unit from a map of `Multiplicative` units and (integer) powers, or a partitionable seq.

    `(this {kg 1 m -3})` -> a unit for density
  "
[pfs]
	(let [pairs (cond
               (map? pfs)
                  ; todo: check nonempty
                  (map list (keys pfs) (vals pfs))
               (seq? pfs)
                  ; todo: other checks
                  (cond
                    (empty? pfs)       (throw (Exception. "Empty list not implemented yet"))
                    (odd? (count pfs)) (throw (Exception. "Odd argnum error"))
                    true               (partition 2 pfs))
               true (throw (Exception. "prime factors as a map or an alist")))
        ; todo: some checks that we are indeed working with units and integers.
	      mapped (map (fn [[a b]] (power a b)) pairs)]; generate ((power unit exponent) ... (power unit exponent))
           (reduce (fn [x y] (times x y)) mapped)))



;; A generic type for an amount with a unit, that doesn't care about the implementation of `unit` or of `value` (as long as these are consistent).

;; (t/ann-datatype amount [value :- Object, unit :- Unitlike])

(deftype amount [value unit]
  Dimensionful
  (getUnit [this] unit)
  (getValue [this other-unit] ((getConverter unit other-unit) value))
  (to [this other-unit] (new amount (getValue this other-unit) other-unit))
  (AsUnit [this] (rescale unit value))

  Object
  (toString [this] (str value " " unit))

  ;; TODO: improve printed output somehow...
)

;; (t/ann amount? (t/Pred amount))

(defn amount? [x] (instance? amount x))
