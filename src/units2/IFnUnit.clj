(ns units2.IFnUnit
  (:require [units2.core :refer :all])
  (:import (javax.measure.unit Unit UnitFormat)
           (javax.measure.converter UnitConverter)))

(set! *warn-on-reflection* true)

;; The reference implementation is based on javax.measure

;; We use a wrapper instead of an `extend-type` in order to allow future changes to
;; the reference implementation without breaking backwards compatibility.
;; Hiding away the javaworld implementation also encourages idiomatic clojure
;; and protocol dispatch (nice, fast) rather than java interop and reflection (ugly, slow).

(defn- to-javax ^Unit [thisclass obj] ; a helper function for a recurring pattern in IFnUnit
  (cond
      (instance? thisclass obj) (implementation-hook obj)
      (instance? Unit obj) obj
      true (throw (Exception. "error in to-javax!"))))

(deftype IFnUnit [^Unit javax-unit]
  Unitlike
  (getDimension [this] (.getDimension ^Unit javax-unit))
  (implementation-hook [this] javax-unit)
  (compatible? [this that]
    (.isCompatible javax-unit (to-javax IFnUnit that)))
  (getConverter [this that]
    (if (compatible? this that)
      (fn [x] (.convert ^UnitConverter (.getConverterTo javax-unit (to-javax IFnUnit that)) (double x)))
      (throw (Exception. "The units are not compatible, no conversion exists."))))
  (rescale [this x]
    (if (== 1 (double x))
      this ; identity converter not allowed in javax
      (new IFnUnit (.times javax-unit (double x)))))

  clojure.lang.IFn
  (applyTo [this [x]] (cond (satisfies? Dimensionful x) (to x this)
                            ;(satisfies? Unitful x)      (getConverter this x) ; unit conversions without the `amount` overhead.
                            (number? x) (new units2.core.amount x this)))
  (invoke [this x] (apply this [x]))
  ; higher arity invoke/apply should NOT be defined. Use `map' to avoid silly nonsense.
  ; this is independent of the IFnUnit implementation, but does depend on units2.core.amount so it can't be moved below.

  Object
  (toString [this] (.toString javax-unit))

   Multiplicative
   (inverse [this] (new IFnUnit (.inverse javax-unit)))
   (times [this] this)
   (times [this that] (new IFnUnit (.times javax-unit (to-javax IFnUnit that))))
   (times [this that the-other] (new IFnUnit (.times javax-unit (.times (to-javax IFnUnit that) (to-javax IFnUnit the-other)))))
   (divide [this] (inverse this))
   (divide [this that] (new IFnUnit (.divide  javax-unit (to-javax IFnUnit that))))
   (divide [this that the-other] (new IFnUnit (.divide  javax-unit (.times (to-javax IFnUnit that) (to-javax IFnUnit the-other)))))
   (power [this N] (new IFnUnit (.pow javax-unit N)))
  )

(defmacro defunit
"`def` a symbol to hold a unit, and change that unit's `UnitFormat` to that symbol (for prettier printing)."
[name value]
    `(do
      (def ~name ~value)
      (.. UnitFormat getInstance (label (implementation-hook ~name) (str '~name)))))


;; These are implementation-independent, separated from the code above to encourage reuse.

(extend-type IFnUnit
  Wrappable
  (wrap-in [this f] (fn [x] (f (this x))))
  (wrap-out [this f] (fn [x] (this (f x))))
  (unwrap-in [this f] (fn [x] (f (getValue x this))))
  (unwrap-out [this f] (fn [x] (getValue (f x) this)))
)

(defmacro defunit-with-SI-prefixes
  "A `defunit` that also defines all SI-prefixed units."
  [name value]
  `(do
     (defunit ~name ~value) ;; Don't forget the base unit!!
     ~@(map (fn [pre] `(defunit
                         ~(symbol (str (first pre) name))
                         (rescale ~name ~(second pre))))
            (partition 2 '[y 1e-24
                           z 1e-21
                           a 1e-18
                           f 1e-15
                           p 1e-12
                           n 1e-9
                           mu 1e-6
                           m 0.001
                           c 0.01
                           d 0.1
                           ;; base unit above
                           da 10
                           h 100
                           k 1000
                           M 1e6
                           G 1e9
                           T 1e12
                           P 1e15
                           E 1e18
                           Z 1e21
                           Y 1e24
                           ]))))
