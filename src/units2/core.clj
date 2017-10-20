(ns units2.core
  (:require [clojure.spec.alpha :as spec]))

(defprotocol Dimensionful
  "`Dimensionful` values are values that 'have a unit'. Their unit can be inspected (`getUnit`) and they can be converted into other units (`to`)."
  (getUnit [this] "Returns the unit associated to this quantity.")
  (to [this Unit] "Converts this quantity to the given unit.")
  (getValue [this Unit] "Converts this quantity to the given unit, but returns a nondimensionful (normal Clojure) value.")
  (AsUnit [this] "Returns a unit in which this quantity is unity.")
)

(defprotocol Unitlike
  "`Unitlike` values are units (as far as duck typing is concerned). They can be compared and manipulated as first-class members of the language."
  (getDimension [this] "Returns the dimension of this unit.")
  (compatible? [this that] "Do two units have the same dimension? If so, they are `compatible?'")
  (getConverter [this that] "Returns a function that describes the unit transformation, as it acts on normal Clojure values... mostly for internal use.")
  (from [this] "Returns a function that converts a quantity to the given unit and returns a nondimensionful (normal Clojure) value.")
  (rescale [this number] "Returns a unit linearly rescaled by the given factor, e.g. `(rescale minute 60)` ==> `hour`.")
  (offset [this amount] "Returns a unit offset by the given amount.")
)

;; Below: "Read-only" restriction of the above
;; that can be used to work with pre-existing
;; Dimensionful or Unitlike things, but NOT to
;; create new ones automatically. Maybe useful
;; if doing so would be expensive or insecure?
;; ((cf eg http://wiki.c2.com/?FacetPattern))

(comment

(defprotocol RestrictedDimensionful
  (getUnit [this])
  (getValue [this Unit]))

(defprotocol RestrictedUnitlike
  (getDimension [this])
  (compatible? [this that])
  (getConverter [this that])
  (from [this]))

)

;; If you're even remotely tempted to use this,
;; I'd like to know what your use case is. I
;; cannot think of a plausible reason to want it.
;;
;; That said, notice that these are the functions
;; you will encounter at the outgoing boundary
;; between your unit-aware code and all the other
;; code that doesn't need to know about units.


;; this should be hidden from users somehow... right?
(defprotocol Hackable
  (implementation-hook [this] "this may be useful for implementors, but should NOT be used in applications.")
)

(defprotocol Multiplicative
  (times [this] [this that]
         [this that the-other]) ; Seinfeld reference
  (divide [this] [this that] [this that the-other])
  (inverse [this] "syntactic sugar for arity-1 `divide`")
  (power [this N])
  (root  [this N])
)


(spec/def ::multiplicative-unitlike
  (spec/and #(satisfies? Multiplicative %)
            #(satisfies? Unitlike %)))

(spec/fdef parse-unit
  :args (spec/or :map (spec/map-of ::multiplicative-unitlike integer? :min-count 1)
                 :sequence (spec/+ (spec/cat :unit ::multiplicative-unitlike
                                             :power integer?)))
  :ret  ::multiplicative-unitlike
)

(defn parse-unit
"Returns a composite unit from a map or association-list of `Multiplicative` units and integer powers.

    `(this {kg 1 m -3})` -> a unit for density
    `(this [m 2])` -> an area unit
    `(this [m 3 m -1])` -> same as above
  "
[pfs]
  (letfn [(parse [input]
            (cond
               (map? input)
                  (if (empty? (keys input))
                    (throw (IllegalArgumentException. "Can't generate a unit from an empty map!"))
                    (vec (map list (keys input) (vals input))))
               (sequential? input)
                  (cond
                    (empty? input)       (throw (IllegalArgumentException. "Can't generate a unit from an empty association-list!"))
                    (odd? (count input)) (throw (IllegalArgumentException. (str "Odd number of elements in the association-list! (" input " provided)")))
                    true               (vec (partition 2 input)))
              ; TODO: think long and hard about whether parsing from a string is worth it...
              ; (string? input)
              ;   (if (empty? input)
              ;     (throw (IllegalArgumentException. "Can't generate a unit from an empty string!"))
              ;     (parse (eval (binding [*read-eval* false] (read-string input)))))
               true (throw (IllegalArgumentException. (str "Mis-specified map or association-list! (" input " provided)")))))
           (validate [parsed-input]
              (cond
                (not (every? (comp integer? second) parsed-input))
                  (throw (IllegalArgumentException. (str "All exponents must be integers! (" (vec (map second parsed-input)) " provided)"))) ; I could generalise this to `rational?` exponents, but the restriction to integers here seems justifiable.
                (not (every? (comp #(instance? units2.core.Unitlike %) first) parsed-input))
                  (throw (IllegalArgumentException. (str "All units must be `Unitlike`! (" (vec (map first parsed-input)) " provided)")))
                (not (every? (comp #(instance? units2.core.Multiplicative %) first) parsed-input))
                  (throw (IllegalArgumentException. (str "All units must be `Multiplicative`! (" (vec (map first parsed-input)) " provided)")))
                :else
                  parsed-input))
          ]
	  (let [pairs (-> pfs parse validate)]
        ;; do the work
        (let [mapped (map (fn [[a b]] (power a b)) pairs)]; generate ((power unit exponent) ... (power unit exponent))
          (reduce (fn [x y] (times x y)) mapped)))))

;; both names make sense in different contexts
(def unit-from-powers parse-unit)

(spec/def ::amount
  (spec/with-gen
    (spec/and #(satisfies? Dimensionful %)
              #(satisfies? Unitlike (getUnit %)))
    (fn [] (let [children (descendants ::amount)]
              (if (nil? children)
                (throw (IllegalStateException. "`(descendants :units2.core/amount)` is empty! Uninstantiable generator."))
                (spec/gen  (rand-nth (vec children))))))))

;; A generic type for an amount with a unit, that doesn't care about the implementation of `unit` or of `value` (as long as these are consistent).
(deftype amount [value unit]
  Dimensionful
  (getUnit [this] unit)
  (getValue [this other-unit] ((getConverter unit other-unit) value))
  (to [this other-unit] (new amount (getValue this other-unit) other-unit))
  (AsUnit [this] (rescale unit value))

  Object
  (toString [this] (str "(" unit " " value ")"))
)

(defmethod print-method amount [a ^java.io.Writer w]
  (.write w (str a))) ; human and (almost) computer readable.


(defn amount?
  "Returns true if x is an instance of `units2.core.amount`, and carries a unit that `(satisfies? Unitlike)`."
  [x] (and (instance? amount x)
           (satisfies? Unitlike (getUnit x))))


;; This is NOT a safe way to check for linearity!
(defn linear?
  "An extended Blum-Luby-Rubinfeld [BLR 1990] test
  (with a relative tolerance, for double precision)
  taking seven samples of `f` to perform seven tests."
  [f]
  (let [x (rand)
        y (rand)
        z (rand)
        fx (f x)
        fy (f y)
        fz (f z)
        fxy (f (+ x y))
        fyz (f (+ y z))
        fxz (f (+ x z))
        fxyz (f (+ x y z))
        close? (fn [x y] (< -1e-6 (/ (- x y) x) 1e-6))
        ]
    (and
      (close? fxy (+ fx fy))
      (close? fyz (+ fy fz))
      (close? fxz (+ fx fz))
      (close? fxyz (+ fx fyz))
      (close? fxyz (+ fy fxz))
      (close? fxyz (+ fz fxy))
      (close? fxyz (+ fx fy fz))
     )))
