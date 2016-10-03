(ns units2.core
  (:require [clojure.spec :as spec]))

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
  ; Not 100% necessary, but nice to have
  (rescale [this number] "Returns a unit linearly rescaled by the given factor.")
  (offset [this amount] "Returns a unit offset by the given amount.")
)

;; this should be hidden from users somehow... right?
(defprotocol Hackable
  (implementation-hook [this] "this may be useful for implementors, but should NOT be used in applications.")
)

(defprotocol Multiplicative
  (times [this] [this that]
         [this that the-other]) ;Seinfeld reference
  (divide [this] [this that] [this that the-other])
  (inverse [this] "syntactic sugar for arity-1 `divide`")
  (power [this N] "syntactic sugar merging `times` and `divide`")
)


(defn unit-from-powers
"Returns a composite unit from a map or association-list of `Multiplicative` units and integer powers.

    `(this {kg 1 m -3})` -> a unit for density
    `(this [cm 2])` -> a small area unit
  "
[pfs]
	(let [pairs (cond
               (map? pfs)
                  (if (empty? (keys pfs))
                    (throw (IllegalArgumentException. "Can't generate a unit from an empty map!"))
                    (map list (keys pfs) (vals pfs)))
               (sequential? pfs)
                  (cond
                    (empty? pfs)       (throw (IllegalArgumentException. "Can't generate a unit from an empty association-list!"))
                    (odd? (count pfs)) (throw (IllegalArgumentException. "Odd number of elements in the association-list!"))
                    true               (partition 2 pfs))
               true (throw (IllegalArgumentException. "Mis-specified map or association-list!")))]
      (when (not (every? (comp integer? second) pairs))
        (throw (IllegalArgumentException. "All powers must be integers!")))
      (when (not (every? (comp #(and (satisfies? Unitlike %) (satisfies? Multiplicative %)) first) pairs))
        (throw (IllegalArgumentException. "All units must be `Multiplicative`!")))
      (let [mapped (map (fn [[a b]] (power a b)) pairs)]; generate ((power unit exponent) ... (power unit exponent))
        (reduce (fn [x y] (times x y)) mapped))))


(spec/def ::amount (spec/and #(satisfies? Dimensionful %)
                             #(satisfies? Unitlike (getUnit %))))

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
  "Returns true if x is an instance of units2.core.amount."
  [x] (instance? amount x)) ; TODO: think about whether we want this or (spec/valid? ::amount %)



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
