(ns units2.core)

(set! *warn-on-reflection* true)

;; As a physicist, I use an analogy to gauge theory to think about code with units.

(defprotocol Dimensionful
  "The values represented by dimensionful datatypes are gauged under unit transformations. If 1km = 1000 meters are the same quantity under different but redundant descriptions, then we want our code to be invariant (ie not need rewriting) under transformations of the variables from one set of units to another."
  (getUnit [this] "get the unit of")
  (to [this Unit] "convert this to the given unit.")
  (getValue [this Unit] "convert this to the given unit and return the value only.")
  (AsUnit [this] "return a unit in which this quantity is unity")
)

(defprotocol Unitlike
  "Defines the behaviour of a unit"
  (getDimension [this] "get the dimension of")
  (compatible? [this that] "Do two units have the same dimension? If so, they are `compatible?'")
  (getConverter [this that] "Returns a function that describes the unit transformation, as it acts on values... mostly for internal use.")
  ;; Not 100% necessary, but nice to have
  (rescale [this number] "Returns a unit linearly rescaled by the given factor")
  (offset [this amount] "Returns a unit offset by the given amount")
  )

(defprotocol Hackable
  (implementation-hook [this] "this may be useful for implementors, but should NOT be used in applications.") ;; this should be hidden from users... right?
  )

(defprotocol Multiplicative
  (times [this] [this that]
         [this that the-other]) ;Seinfeld reference
  (divide [this] [this that] [this that the-other])
  (inverse [this] "syntactic sugar for arity-1 (divide ...)")
  (power [this N] "syntactic sugar for all of the above")
)


(defn unit-from-powers
"Returns a composite unit from a map of `Multiplicative` units and (integer) powers, or a partitionable seq.

    `(this {kg 1 m -3})` -> a unit for density
    `(this [cm 2])` -> a small area unit
  "
[pfs]
	(let [pairs (cond
               (map? pfs)
                  ; todo: check nonempty
                  (map list (keys pfs) (vals pfs))
               (sequential? pfs)
                  ; todo: other checks
                  (cond
                    (empty? pfs)       (throw (IllegalArgumentException. "Empty list not implemented yet"))
                    (odd? (count pfs)) (throw (IllegalArgumentException. "Odd argnum error"))
                    true               (partition 2 pfs))
               true (throw (IllegalArgumentException. "args should be specified as a map or an alist")))
        ; todo: some checks that we are indeed working with units and integers.
	      mapped (map (fn [[a b]] (power a b)) pairs)]; generate ((power unit exponent) ... (power unit exponent))
           (reduce (fn [x y] (times x y)) mapped)))



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
  (.write w (str a))) ;; human and (almost) computer readable.

(defn amount? [x] (instance? amount x))



;; this is NOT a safe way to check for linearity!
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
