;; Mathy operations on the (generically defined) `units2.core.amount` type.

;; This implementation assumes:
;;
;;   1. that the values in `amount`, and any other numbers, are well-behaved under `clojure.core/fns` and `Math/fns`
;;   2. that the units in `amount` both `(satisfies? Unitlike)` and `(satisfies? Multiplicative)`.
;;   3. that the units in `amount` are all linear rescalings without any `offset`.
;;        (There are a few offset checks scattered throughout,
;;         so offsets *usually* work, but no overall guarantees.)
;;
;; As such, it should be fairly easy to reuse most of this code for other implementations.

;; ## Nota Bene
;;
;;   1. The ops we define here are fine with `require :refer :all`, they reduce to their `clojure.core/` and `Math/` counterparts on non-`amount?` quantities.
;;   2. When standard functions would give surprising behaviour in combination with units, users might inadvertently write buggy code. We provide a version of the function that issues an Exception whenever it is used on units (to force the user to think about the behaviour they want).
;;   3. For many ops, we also provide a standardised augmented-arity version that uses divide-into-double on the first two args
;;   <pre><code> ;; these two expressions are equivalent
;;   (pow a b c)
;;   (Math/pow (divide-into-double a b) c)
;;   </code></pre>
;;

;; ### TODO: add/fix docstrings
;; ### TODO: add/fix function specs


(ns units2.ops
  ;redefinitions imminent! `:exclude` turns off some WARNINGS.
  (:refer-clojure :exclude [+ - * / rem quot == > >= < <= zero? pos? neg? min max])
  (:require [units2.core :refer :all]
            [clojure.spec :as spec]
            [clojure.spec.gen :as gen]) ; TODO: spec these functions!!!
)

;; ## Specs

(spec/def ::number-or-amount (spec/or :number number? :amount :units2.core/amount))

(spec/def ::compatible-amounts
  (spec/and
    (spec/* :units2.core/amount)
    (fn [amountlist] ;; it's really the conformed value here, but for amounts it's the same thing!
      (if (clojure.core/< (count amountlist) 2)
        true ; obvious if
        (every? #(compatible? (getUnit (first amountlist)) (getUnit %)) (rest amountlist))))))

(spec/def ::numbers-or-compatible-amounts
  (spec/or :all-numbers (spec/* number?)
           :all-amounts ::compatible-amounts))

(spec/def ::some-numbers-or-compatible-amounts
  (spec/and ::numbers-or-compatible-amounts
            #(not (empty? (spec/unform ::numbers-or-compatible-amounts %)))))

(spec/def ::linear-units
  (spec/and ::compatible-amounts
            (fn [amountlist] ;; it's really the conformed value here, but for amounts it's the same thing!
              (if (clojure.core/< (count amountlist) 2)
                true ; obvious if
                (every?
                  #(linear? (getConverter (getUnit (first amountlist)) (getUnit %)))
                  (rest amountlist))))))

(spec/def ::nonzero-amount
  (spec/and :units2.core/amount
            #(not (clojure.core/zero? (getValue % (getUnit %))))))

(spec/def ::two-linear-units-second-nonzero
  (spec/and (spec/coll-of :units2.core/amount :count 2)
            ::linear-units
            #(spec/valid? ::nonzero-amount (second %))))



;; ## Arithmetic

;;; ### Dispatch helper macros (possible code smell... use multimethods instead?)

;; dispatch for + and -
(defmacro threecond [[a b] both one neither]
  `(cond
    (and (amount? ~a) (amount? ~b))  ~both
    (or  (amount? ~a) (amount? ~b))  ~one
    true                             ~neither))

;; dispatch for + and - with a check that the conversion is linear
(defmacro lincond [[a b] lin nonlin one neither]
  `(threecond [~a ~b]
      (if (linear? (getConverter (getUnit ~a) (getUnit ~b)))
        ~lin
        ~nonlin)
      ~one
      ~neither))

;; dispatch for * and /
(defmacro fourcond [[a b] one two three four]
  `(cond
    (and (amount? ~a) (amount? ~b))       ~one
    (and (amount? ~a) (not (amount? ~b))) ~two
    (and (amount? ~b) (not (amount? ~a))) ~three
    (not (or (amount? ~a) (amount? ~b)))  ~four))


;; ### Abstract Algebra of Units in Arithmetic

(defn decorate-adder [adder]
  (fn decorated-adder
    ([] (adder))     ; same return value as the original
    ([a] (if (amount? a)
           (->amount (adder (getValue a (getUnit a))) (getUnit a))
           (adder a)))
    ([a b]
      (lincond [a b]
        (->amount (adder (getValue a (getUnit a)) (getValue b (getUnit a))) (getUnit a))
        (throw (UnsupportedOperationException. (str "Nonlinear conversion between `" (getUnit a) "' and `" (getUnit b)"'!")))
        (throw (UnsupportedOperationException. (str "It's meaningless to apply" (str adder) " to numbers with and without units! (`" a "' and `" b "' provided)")))
        (adder a b)))
    ([a b & rest] (reduce decorated-adder a (conj rest b)))
  )
)

(defn decorate-productor [productor]
  (fn decorated-productor
    ([] (productor)) ; same return value as the original
    ([a] (if (amount? a)
           (->amount (productor (getValue a (getUnit a))) (getUnit a))
           (productor a)))
    ([a b] (fourcond [a b]
      (->amount (productor (getValue a (getUnit a)) (getValue b (getUnit b))) (times (getUnit a) (getUnit b)))
      (->amount (productor (getValue a (getUnit a)) (double b)) (getUnit a))
      (->amount (productor (getValue b (getUnit b)) (double a)) (getUnit b))
      (productor a b)))
    ([a b & rest] (reduce decorated-productor a (conj rest b)))
  )
)

(defn decorate-divider [divider]
  (fn decorated-divider
    ([] (divider)) ; zero arity explicitly an error, but let the original catch this.
    ([a] (if (amount? a)
         (->amount (divider (getValue a (getUnit a))) (inverse (getUnit a)))
         (divider a)))
    ([a b] (fourcond [a b]
      (->amount (divider (getValue a (getUnit a)) (getValue b (getUnit b))) (divide (getUnit a) (getUnit b)))
      (->amount (divider (getValue a (getUnit a)) (double b)) (getUnit a))
      (->amount (divider (double a) (getValue b (getUnit b))) (inverse (getUnit b)))
      (divider a b)))
    ([a b & rest] (reduce decorated-divider a (conj rest b)))
  )
)


;; ### Concrete Arithmetic Ops (with Specs and old docstrings)

(def ^{:doc (:doc (meta (var clojure.core/+)))} + (decorate-adder clojure.core/+))
(def ^{:doc (:doc (meta (var clojure.core/-)))} - (decorate-adder clojure.core/-))
(def ^{:doc (:doc (meta (var clojure.core/*)))} * (decorate-productor clojure.core/*))
(def ^{:doc (:doc (meta (var clojure.core//)))} / (decorate-divider clojure.core//))


(spec/fdef +
  :args ::numbers-or-compatible-amounts
  :ret  ::number-or-amount
  :fn   #(if (every? number? (:args %))
           (number? (:ret %))
           (:units2.core/amount (:ret %)))
  )

(spec/fdef -
  :args ::some-numbers-or-compatible-amounts
  :ret  ::number-or-amount
  :fn   #(if (every? number? (:args %))
           (number? (:ret %))
           (:units2.core/amount (:ret %)))
  )

(spec/fdef *
  :args (spec/* (spec/or :amount :units2.core/amount :number number?))
  :ret  (spec/or :amount :units2.core/amount :number number?)
  :fn   #(if (every? number? (:args %))
           (number? (:ret %))
           (:units2.core/amount (:ret %)))
  )

(spec/fdef /
  :args (spec/and (spec/+ ::number-or-amount)
                  (fn [x] ; also: don't divide by zero!!!
                    (let [unsafe-zero? (fn [a] (clojure.core/zero? (if amount? (getValue a (getUnit a)) a)))] ;; this is an unsafe version of zero?
                      (if (empty? (rest x))
                        (not (unsafe-zero? (spec/unform ::number-or-amount (first x))))
                        (every? #(not (unsafe-zero? (spec/unform ::number-or-amount %))) (rest x))))))
  :ret  (spec/or :amount :units2.core/amount :number number?)
  :fn   #(if (every? number? (:args %))
           (number? (:ret %))
           (:units2.core/amount (:ret %)))
  )



(spec/fdef divide-into-double :args ::two-linear-units-second-nonzero
                              :ret double?)

(defn divide-into-double
  "When all units involved are linear rescalings of one another, fractions with the same dimensions in the numerator and denominator have a *unique* unit-free value.
  This is basically syntactic sugar for `(getValue a (AsUnit b))` with some error checking."
  ^double [a b]
   (if (and (amount? a)
            (amount? b)
            (compatible? (getUnit a) (getUnit b))
            (linear? (getConverter (getUnit a) (getUnit b))))
      (getValue a (AsUnit b))
      (throw (IllegalArgumentException. (str "divide-into-double requires two amounts with linearly related units! (`" a "' and `" b "' provided)")))))


;; ## Modular Arithmetic

(let [PSA "Modular arithmetic interacts nontrivially with offsets of units."]
  ;;   Modular arithmetic interacts nontrivially with rescalings of units.
  ;;   Consider the following:
  ;;   <pre><code>
  ;; (let [a (->amount 2.5 meter)
  ;;       b (->amount 5   (/ meter 2))
  ;;       c (->amount 3   second)]
  ;;   (list
  ;;     (== a b)  ; true
  ;;     (rem a c) ; 2.5 m/s
  ;;     (rem b c) ; 2 (m/2)/s
  ;;     (== (rem a c)
  ;;         (rem b c)))) ; FALSE
  ;;   </code></pre>

  (defn decorate-ratio [ratio]
    (fn
      ([a b]
        (if (or (amount? a) (amount? b))
          (throw (UnsupportedOperationException. PSA))
          (ratio a b)))
      ([a b c] 42) ;; treat third as the `unity' quantity... COMING SOON!
    )
  )

  (defn decorate-entier [entier]
    (fn
      ([a]
        (if (amount? a)
          (throw (UnsupportedOperationException. PSA))
          (entier a)))
      ([a b] ;; treat second amount as the `unity' quantity, e.g. (round ... 10) rounds to the nearest 10.
       (* (entier (divide-into-double a b)) b))
    )
  )

  (def ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/rem)))
                     "\n  Units2 added doc: " PSA)}
    rem (decorate-ratio clojure.core/rem))
  (def ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/quot)))
                     "\n  Units2 added doc: " PSA)}
    quot (decorate-ratio clojure.core/quot))

  (def ^{:doc PSA} ceil  (decorate-entier #(Math/ceil %)))
  (def ^{:doc PSA} floor (decorate-entier #(Math/floor %)))
  (def ^{:doc PSA} round (decorate-entier #(Math/round %)))
  (def ^{:doc PSA} abs   (decorate-entier #(Math/abs %)))

)

; specs here!



;; ## Comparisons

;; Defining `==` , `>=`, or `<` seems easy; but the desired behaviour for
;; comparisons of incompatible units could be either to return false or
;; throw an exception; we opt for the latter, since strictly speaking
;; `is 3 meters smaller than 5 seconds?' is a meaningless question and
;; shouldn't be given a boolean answer; consider for a moment the alternate
;; behaviour, which violates the excluded-middle-law:
;; <pre><code> ;; this is ugly!
;; (not (< (m 4) (celcius 6)) ;; not untruthy
;; (>= (m 4) (celcius 6)) ;; untruthy
;; </code></pre>
;;
;; The behaviour for `==` could be different than other comparators,
;; since e.g. 3 meters and 5 seconds are never the same amount; but
;; consistently throwing exceptions seems like the safer route.

(defn decorate-comparator [cmp]
  (fn decorated-comparator [& args]
    (cond
      (empty? args) (cmp)
      (every? #(not (amount? %)) args)
        (apply cmp args)
      (every? amount? args)
        (if (every? #(compatible? (getUnit (first args)) %) (map getUnit (rest args)))
            (apply cmp (map (from (getUnit (first args))) args))
            (throw (UnsupportedOperationException. "Can't compare quantities in incompatible units")))
      :else
        (throw (UnsupportedOperationException. "Can't compare quantities with and without units!") )
     )
  )
)

(def ^{:doc (:doc (meta (var clojure.core/==)))} == (decorate-comparator clojure.core/==))
(def ^{:doc (:doc (meta (var clojure.core/< )))} <  (decorate-comparator clojure.core/<))
(def ^{:doc (:doc (meta (var clojure.core/> )))} >  (decorate-comparator clojure.core/>))
(def ^{:doc (:doc (meta (var clojure.core/<=)))} <= (decorate-comparator clojure.core/<=))
(def ^{:doc (:doc (meta (var clojure.core/>=)))} >= (decorate-comparator clojure.core/>=))

(spec/fdef == :args ::some-numbers-or-compatible-amounts :ret boolean?)
(spec/fdef <  :args ::some-numbers-or-compatible-amounts :ret boolean?)
(spec/fdef >  :args ::some-numbers-or-compatible-amounts :ret boolean?)
(spec/fdef <= :args ::some-numbers-or-compatible-amounts :ret boolean?)
(spec/fdef >= :args ::some-numbers-or-compatible-amounts :ret boolean?)



(let [PSA "Sign checks interact nontrivially with offsets of units.\n Please use explicit `==`,`>`,`<` checks against a zero-valued quantity."]
  ;; `zero?`, `pos?`, and `neg?` can return different answers depending on the input units
  ;; (to see why, consider the Farenheit and Celius temperature scales)

  (defn decorate-sign-predicate [pred]
    (fn [a]
      (if (amount? a)
           (throw (UnsupportedOperationException. PSA))
           (pred a))))

  (def ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/zero?)))
                   "\n  Units2 added doc: " PSA)}
    zero? (decorate-sign-predicate clojure.core/zero?))
  (def ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/pos?)))
                   "\n  Units2 added doc: " PSA)}
    pos? (decorate-sign-predicate clojure.core/pos?))
  (def ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/neg?)))
                   "\n  Units2 added doc: " PSA)}
    neg? (decorate-sign-predicate clojure.core/neg?))
)

(spec/fdef zero? :args number? :ret boolean?)
(spec/fdef pos?  :args number? :ret boolean?)
(spec/fdef neg?  :args number? :ret boolean?)



(defn decorate-order-statistic [ord]
  (fn decorated-order-statistic [& nums]
    (cond
      (empty? nums)
        (ord)
      (every? #(not (amount? %)) nums)
        (apply ord nums)
      (every? amount? nums)
        (if (every? #(compatible? (getUnit (first nums)) (getUnit %)) (rest nums))
          (let [U (getUnit (first nums))]
            (->amount (apply ord (map (from U) nums)) U))
          (throw (UnsupportedOperationException. "Can't order dimensionally incompatible quantities!")))
      :else
        (throw (UnsupportedOperationException. "Can't order quantities with and without units!"))
    )
  )
)

(def ^{:doc (:doc (meta (var clojure.core/min)))} min (decorate-order-statistic clojure.core/min))
(def ^{:doc (:doc (meta (var clojure.core/max)))} max (decorate-order-statistic clojure.core/max))

(spec/fdef max :args ::some-numbers-or-compatible-amounts
               :ret  ::number-or-amount)

(spec/fdef min :args ::some-numbers-or-compatible-amounts
               :ret  ::number-or-amount)





;; ## Powers and Exponentiation

(spec/fdef expt :args (spec/and (spec/tuple :units2.core/amount integer?)
                                (fn [[base exponent]]
                                  (if (<= 0 exponent)
                                    true
                                    (spec/valid? ::nonzero-amount base))))
                :ret :units2.core/amount)

(defn expt
  "`(expt b n) == b^n`

  where `n` is an integer and `b` is an amount.
  "
  [b n]
  (if (clojure.core/neg? n)
    (apply / (repeat (+ 2 (- n)) b)) ; ugly hack, but it works fine. It even (correctly) throws a divide by zero exception when b is zero!
    (apply * (repeat n b))))


(let [PSA "Exponentiation interacts nontrivially with rescalings of units."]
  ;; The exponential functions below should only be defined on dimensionless quantities
  ;; (to see why, just imagine Maclaurin-expanding the `exp` or `ln` functions)
  ;; so we define them over ratios of amounts: exp(a/b), log(a/b), etc.


  (defn decorate-expt [expt]
    (fn
      ([a]
        (if (amount? a)
            (throw (UnsupportedOperationException. PSA))
             (expt a)))
      ([a b]
        (if (every? amount? [a b])
          (expt (divide-into-double a b))
          (throw (IllegalArgumentException. (str "Arity-2 exponentiation requires two amounts! (" a " and " b " provided)")))
          ))))

  (def ^{:doc PSA} exp  (decorate-expt #(Math/exp %)))
  (def ^{:doc PSA} log  (decorate-expt #(Math/log %)))
  (def ^{:doc PSA} log10 (decorate-expt #(Math/log10 %)))

)

; specs separately (function ranges)

(spec/fdef pow :args (spec/or :bin (spec/tuple number? number?)
                              :tri ;(spec/with-gen
                                     (spec/and (spec/tuple :units2.core/amount :units2.core/amount number?)
                                               (fn [[a b _]] (spec/valid? ::two-linear-units-second-nonzero [a b])))
                                   ; (fn [] (gen/cat (spec/gen ::two-linear-units-second-nonzero)
                                   ;                 (gen/fmap list (gen/gen-for-pred number?)))))
                          )
               :ret double?)

(defn pow
  "TODO: docstring"
  ([a b]
    (if (or (amount? a) (amount? b))
      (throw (Exception. "!!!!"))
      (Math/pow a b)))
  ([a b c]
     (if (and (amount? a) (amount? b) (compatible? (getUnit a) (getUnit b)) (not (amount? c)))
       (Math/pow (divide-into-double a b) c)
       (throw (IllegalArgumentException. (str "Arity-3 `pow` requires two compatible amounts and a number! (" a ", " b ", and " c " provided)"))))))



;; ## EVERYTHING TOGETHER

(defmacro defmacro-without-hygiene
  "Expands into a `defmacro` with a `(let [bindings] body)` that gets
  around Clojure's automatic namespacing in syntax-quote by building
  the `let` form explicitly. Macros can be dangerous. Have fun!!!!"
  [macroname docstring bindings]
  (let [body (gensym)]
  `(defmacro ~macroname ~docstring [& ~body]
     (clojure.core/conj ~body '~bindings 'clojure.core/let))))


;; 14/03/16: Thanks to the `Amsterdam Clojurians` for noticing that `conj` should be `clojure.core/conj` in the above,
;; with the comment "If you're willing to do that, you should be prepared for users willing to define their own `conj`."

(defmacro-without-hygiene with-unit-arithmetic
  "Locally rebind arithmetic operators like `+`, and `/` to unit-aware equivalents."
  [+ units2.ops/+
   - units2.ops/-
   * units2.ops/*
   / units2.ops//
   quot units2.ops/quot
   rem  units2.ops/rem
   ])

(defmacro-without-hygiene with-unit-comparisons
  "Locally rebind `==`, `>=`, `<`, `zero?`, `pos?`, `min`, ... to unit-aware equivalents."
   [== units2.ops/==
     >  units2.ops/>
     <  units2.ops/<
     >= units2.ops/>=
     <= units2.ops/<=
     zero? units2.ops/zero?
     pos?  units2.ops/pos?
     neg?  units2.ops/neg?
     min units2.ops/min
     max units2.ops/max
    ])


(defmacro-without-hygiene with-unit-expts
  "Locally bind exponentiation functions to unit-aware equivalents."
  [expt units2.ops/expt
    exp units2.ops/exp
    pow units2.ops/pow
    log units2.ops/log
    log10 units2.ops/log10
   ])

(defmacro-without-hygiene with-unit-magnitudes ;; THIS IS A BAD NAME. FIND A BETTER NAME.
  "Locally bind magnitude functions to unit-aware equivalents."
  [abs units2.ops/abs
    floor units2.ops/floor
    ceil units2.ops/ceil
    round units2.ops/round
   ])

(defmacro with-unit-
  [labels & body]
     (let [keywords {:arithmetic '[units2.ops/with-unit-arithmetic]
                     :expt '[units2.ops/with-unit-expts]
                     :magn '[units2.ops/with-unit-magnitudes]
                     :math '[units2.ops/with-unit-arithmetic
                             units2.ops/with-unit-expts]
                     :comp '[units2.ops/with-unit-comparisons]
                     :all  '[units2.ops/with-unit-arithmetic
                             units2.ops/with-unit-expts
                             units2.ops/with-unit-comparisons
                             units2.ops/with-unit-magnitudes
                            ]
                     }
           ; figure out which set of environments to use given the keywords
           ; we assume all envs are commutative, i.e. the set need not be ordered.
           envs (remove nil? (into #{} (flatten (map keywords labels))))]
        ; then right-fold the environment macros over the body, and give that as the form to evaluate
        (reduce #(list %2 %1) `(do ~@body) envs)))
