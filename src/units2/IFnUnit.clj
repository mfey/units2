(ns units2.IFnUnit
  (:require [units2.core :refer :all]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen])
  (:import (javax.measure.unit BaseUnit Unit UnitFormat)
           (javax.measure.converter UnitConverter ConversionException)))


;; The reference implementation is based on javax.measure

;; We use a wrapper instead of an `extend-type` in order to allow future changes to
;; the reference implementation without breaking backwards compatibility.
;; Hiding away the javaworld implementation also encourages idiomatic clojure
;; and protocol dispatch (nice, fast) rather than java interop and reflection (ugly, slow).

(defn ^:private to-javax ^Unit [thisclass obj] ; a helper function for a recurring pattern in IFnUnit
  (cond
      (instance? thisclass obj) (implementation-hook obj)
      (instance? Unit obj) obj
      true (throw (Exception. "error in to-javax!"))))

(deftype IFnUnit [^Unit javax-unit]

  Hackable
  (implementation-hook [this] javax-unit)

  Unitlike
  (getDimension [this] (.getDimension ^Unit javax-unit))
  (compatible? [this that]
    (= (getDimension this) (getDimension that)))
  (getConverter [this that]
    (if (compatible? this that)
      (fn [x] (try (.convert ^UnitConverter (.getConverterTo javax-unit (to-javax IFnUnit that)) (double x))
                (catch ConversionException e
                  ;; let's try to divide the two units and work dimensionlessly...
                  ;; maybe the offsets/nonlinearities of our two units cancel exactly?
                  (if-not (compatible? this Unit/ONE) ;; getValue calls getConverter, without this we might loop until the stack overflows!
                    (let [dimensionless-rescale (.divide javax-unit (to-javax IFnUnit that))]
                      (.convert ^UnitConverter (.getConverterTo dimensionless-rescale Unit/ONE) (double x)))
                    (throw e))))) ;; give up!
      ;; still fails for e.g. ((divide (rescale celsius 2) m) ((divide celsius m) 1))
      (throw (UnsupportedOperationException. (str "The units `" this "' and `" that "' are not compatible, no conversion exists.")))))
  (from [this]
    (fn [a]
      (if (amount? a)
        (getValue a this)
        (throw (IllegalArgumentException. (str "Expected an amount with dimension " (getDimension this) " (" a " provided)"))))))
  (rescale [this x]
    (if (amount? x)
      (throw (IllegalArgumentException. (str "Units can only be rescaled by numbers without units (" x " provided)")))
      (if (== 1 (double x))
        this ; identity converter not allowed in javax?
        (new IFnUnit (.times javax-unit (double x))))))
  (offset [this a]
    (if (amount? a)
      (let [x (double (getValue a this))]
        (if (zero? x)
          this
          (new IFnUnit (.plus javax-unit x))))
      (throw (IllegalArgumentException. (str "Units can only be offset by amounts with units (" a " provided)")))))

  clojure.lang.IFn
  (applyTo [this [x]] (cond (satisfies? Dimensionful x) (to x this)
                            (number? x) (new units2.core.amount x this)))
  (invoke [this x] (apply this [x]))
  ; higher arity invoke/apply should NOT be defined. Use `map' to avoid silly nonsense.
  ;(invoke [this x y] (throw (ArityException.)))

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
   (power [this N]
      (cond
       (integer? N) (new IFnUnit (.pow javax-unit N))
       (rational? N) (new IFnUnit (.pow (.root javax-unit (denominator N)) (numerator N)))
       :else (throw (IllegalArgumentException. (str "Expected a `rational?` number (" N " provided)")))
       ))
   (root [this N] (power this (/ N)))
  )

;; (def d (.compound (implementation-hook km) (.compound (implementation-hook m) (implementation-hook mm)))
;; The `.compound` method is basically a `cons` for compatible units.

;; (.getHigher d) ; Basically `(first compound-unit)`
;; #object[javax.measure.unit.TransformedUnit 0x7586ad77 "km"]

;; (.getLower d) ; Basically `(rest compound-unit)`
;; #object[javax.measure.unit.CompoundUnit 0x47077afc "m:mm"]

;; Units in the .compound do not need to be `distinct` or sorted in any way.
;; Basically, Compound units are `seq`able collections of (compatible) units.
;; It's probably better to implement this in clojure... that way we can drop
;; the requirement that units be compatible and make the move to cljs easier.

;; What would compound units look like in a LISP? More importantly,
;; what are the advantages of having compound IFnUnits in amounts?

;; (def amnt (m:mm [1 57])) ???
;; (apply vector amnt) --> [amnt], not ([m 1] [mm 57])!
;; (km:m (m:m [100 10])) --> (km:m [0.1 10]) ???
;;   vs (map apply [km m] (map vector [(m 100) (m 10)])) --> ((km 0.1) (m 10))
;; ((cons cm cm) amnt) --> (cm:cm [100 5.7]) ???
;;   vs (map apply [cm cm] (map vector [(m 1) (mm 57)])) --> ((cm 100) (cm 5.7))

;; So this behaviour could just be subsumed into a regular function
;; (defn mapply [fns args] (map apply fns (map vector args)))
;; (mapply [inc dec] [7 7]); --> (8 6)
;; which feels like it's hidden in the stdlib somewhere...

;; Overall, the best thing to do is to keep the data structures
;; separate from the data being structured: no compound units.


(defmethod print-method IFnUnit [U ^java.io.Writer w]
  (.write w (str U))) ; human and (almost) computer readable.

(defmacro defunit
"`def` a var to hold a unit, and change that unit's printed representation to that var."
[varname value]
  (let [vname varname]
    `(do
      (def ~vname ~value)
      (.. UnitFormat getInstance (label (implementation-hook ~vname) (str '~vname)))
      #'~vname))) ; return like the regular `def`/`defn`/`defmacro`



(defn makebaseunit
  "Returns a new (anonymous) unit at the base of a new dimension. See also `defbaseunit`."
  [^String newdimension]
  ;; If we try to use a *unit* already defined in JScience's Unit/SI as the name of a *dimension*, we get a
  ;; "java.lang.IllegalArgumentException: Symbol W is associated to a different unit"
  ;; However, using units from Unit/NonSI is just fine.
  ;; This is clearly a bug.
   (->IFnUnit (BaseUnit. newdimension)))

;; testme!
(defmacro make-dimensional-spec [keyword-expr unit-expr]
  (let [kw keyword-expr
        unit unit-expr]
  `(do
    (spec/def ~kw (spec/with-gen
      (spec/and :units2.core/amount #(compatible? (getUnit %) ~unit))
      (fn [] (gen/fmap ~unit (gen/gen-for-pred number?)))))
     (derive ~kw :units2.core/amount) ; useful for autogenerating amounts.
     )))

;; testme!
(defmacro defbaseunit
  "Creates a new unit at the base of a new dimension. This function allows dimensional analysis to be extended by the user.

  If the dimensionname is given as a namespaced keyword, a `clojure.spec` spec for the dimension is generated."
  [unit-name dimension-name] ; dimensionname as a namespaced keyword
  (let [u unit-name
        d dimension-name]
  `(do
    (defunit ~u (makebaseunit (name ~d)))
    ~(if (and (keyword? d) (not (nil? (namespace d)))) ; namespaced keyword
      `(make-dimensional-spec ~d ~u))
    #'~u)))


;; This is implementation-independent, please reuse!!!
(defmacro defunit-with-SI-prefixes
  "A `defunit` that also defines all SI-prefixed units."
  [name-expr value-expr]
  (let [varname name-expr
        value   value-expr]
  `(do
     (defunit ~varname ~value) ;; Don't forget the base unit!!
     ~@(map (fn [pre] `(defunit
                         ~(symbol (str (first pre) varname))
                         (rescale ~varname ~(second pre))))
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
                           my 1e4 ; myria, obsolete
                           M 1e6
                           G 1e9
                           T 1e12
                           P 1e15
                           E 1e18
                           Z 1e21
                           Y 1e24
                           ]))
     #'~varname)))

(defmacro defunit-with-IEC-prefixes
  "A `defunit` that also defines all IEC-prefixed units."
  [name-expr value-expr]
  (let [varname name-expr
        value   value-expr]
  `(do
     (defunit ~varname ~value) ;; Don't forget the base unit!!
     ~@(map (fn [pre] `(defunit
                         ~(symbol (str (first pre) varname))
                         (rescale ~varname ~(second pre))))
            (partition 2 '[Ki (Math/pow 1024 1)
                           Mi (Math/pow 1024 2)
                           Gi (Math/pow 1024 3)
                           Ti (Math/pow 1024 4)
                           Pi (Math/pow 1024 5)
                           Ei (Math/pow 1024 6)
                           Zi (Math/pow 1024 7)
                           Yi (Math/pow 1024 8)
                           ]))
     #'~varname)))

;; FUTURE:
;; ; Define U = (* a (- U0 b)) so that rescalings are trivial.

;; (deftype IFnUnit [scaling offset dimension stringname]
;;   Hackable
;;   (implementation-hook [this] [scaling offset dimension stringname])

;;   Unitlike
;;   (getDimension [this] dimension)
;;   (compatible? [this that] (= dimension (getDimension that)))
;;   (getConverter [this that]
;;     (if (compatible? this that)
;;       (let [that-scaling (first (implementation-hook that))
;;             that-offset  (second (implementation-hook that))]
;;         (fn [x] (* that-scaling (+ (/ x scaling) offset (- that-offset)))))
;;       (throw (UnsupportedOperationException. (str "The units `" this "' and `" that "' are not compatible, no conversion exists.")))))
;;   (from [this]
;;     (fn [a]
;;       (if (amount? a)
;;         (getValue a this)
;;         (throw (Exception. (str "Expected an amount with dimension " (getDimension this) " (" a " provided)"))))))
;;   (rescale [this x]
;;     (if (and (number? x) (not (zero? x)))
;;       (new IFnUnit (/ scaling x) offset dimension "")
;;       (throw (IllegalArgumentException. (str "Units can only be rescaled by nonzero-numbers without units (" x " provided)")))))
;;   (offset [this a]
;;     (if (and (amount? a) (compatible? this (getUnit a)))
;;       (let [base (new IFnUnit 1 0 dimension "")
;;             x (getValue a base)]
;;         (if (zero? x)
;;           this
;;           (new IFnUnit scaling (+ offset x) dimension "")))
;;       (throw (IllegalArgumentException. (str "Units can only be offset by amounts with compatible units (" a " provided)")))))

;;   clojure.lang.IFn
;;   (applyTo [this [x]] (cond (satisfies? Dimensionful x) (to x this)
;;                             (number? x) (new units2.core.amount x this)))
;;   (invoke [this x] (apply this [x]))
;;   ;; higher arity invoke/apply should NOT be defined. Use `map' to avoid silly nonsense.
;;   ;; this is independent of the IFnUnit implementation, but does depend on units2.core.amount.

;;   Object
;;   (toString [this]
;;     (if (empty? stringname)
;;      (str [scaling offset dimension])
;;       stringname))

;;   Multiplicative
;;   TODO -- probably requires rethinking the above!!!
;; )


;; ;; The above works now, but isn't "multiplicative" yet... so, it doesn't work with `ops'.
;; (def K (new IFnUnit 1 0 {(Dimension. \T) 1} "K"))
;; (def R (rescale K 5/9))
;; (def C (offset K (K 273.15)))
;; (def F (offset R (R 459.67)))

;; (R (K 0))
;; (F (K 0))
;; (C (K 0))

;; (F (C 37.8))
;; (C (F 0))
;; (F (C 0))
