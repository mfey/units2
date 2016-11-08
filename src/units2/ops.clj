;; Mathy operations on the (generically defined) `units2.core.amount` type.

;; This implementation assumes:
;;
;;   1. that the values in `amount`, and any other numbers, are well-behaved under `clojure.core/fns` and `Math/fns`
;;   2. that the units in `amount` both `(satisfies? Unitlike)` and `(satisfies? Multiplicative)`.
;;   3. that the units in `amount` are all linear rescalings without any `offset`. There are a few offset checks scattered throughout, but no overall guarantees.
;;
;; As such, it should be fairly easy to reuse most of this code for other implementations.

;; ## Nota Bene
;;
;;   1. The ops we define here are fine with `require :refer :all`, they reduce to their `clojure.core/` and `Math/` counterparts on non-`amount?` quantities.
;;   2. When standard functions would give surprising behaviour in combination with units, users might inadvertently write buggy code. We provide a version of the function that issues an Exception whenever it is used on units (to force the user to think about the behaviour they want).
;;   3. Since most users DO know what they want, and since this is usually <pre><code> (getValue x (getUnit x)) </code></pre> these exceptions can be downgraded to warnings by rebinding `*unit-warnings-are-errors*`. Then `getValue`-`getUnit` is used. These warnings can further be silenced by rebinding `*unit-warnings-are-printed*`.
;;   4. For exponentiation ops, we also provide a standardised augmented-arity version that uses divide-into-double on the first two args
;;   <pre><code>
;;   (pow a b c)
;;   (Math/pow (divide-into-double a b) c)
;;   </code></pre>
;;

;; TODO: fix docstrings
;; TODO: add/fix function specs (useful for spec instrumentation)

(ns units2.ops
  ;redefinitions imminent! `:exclude` turns off some WARNINGS.
  (:refer-clojure :exclude [+ - * / rem quot == > >= < <= zero? pos? neg? min max])
  (:require [units2.core :refer :all]
            [clojure.spec :as spec]) ; TODO: spec these functions!!!
)

;; ## WARNINGS

;; users who know what they are doing can set this to false at their own risk.
(def ^:dynamic *unit-warnings-are-errors* true)

;; users who REALLY know what they are doing can prevent warnings from being printed.
;; this is ONLY provided to prevent the printing I/O from slowing down well-tested, innermost-loop,
;; speed-critical computations, and should NOT be used lightly.
(def ^:dynamic *unit-warnings-are-printed* true)



(defn- warn [msg]
  (cond
    *unit-warnings-are-errors*
      (throw (UnsupportedOperationException. (str msg)))
    *unit-warnings-are-printed*
     (println (clojure.string/join (concat "WARNING: " msg)))))


;; ## Comparisons
;; Defining `==` , `>=`, or `<` is easy; but `zero?`, `pos?`, and `neg?` can return different answers depending on the input units
;; (to see why, consider the Farenheit and Celius temperature scales)

(defmacro defcmp [cmp cljcmp]
  (let [args (gensym)]
`(do
   (spec/fdef ~cmp :args (spec/or :all-amounts (spec/+ :units2.core/amount)
                                  :all-numbers (spec/+ number?)) :ret boolean?)
   ;; todo: assert that all amounts must be compatible... somehow?
   (defn ~cmp [& ~args]
      (cond
        (every? amount? ~args)
          (apply ~cljcmp (map #(getValue % (getUnit (first ~args))) ~args))
        (every? #(not (amount? %)) ~args) ; includes empty argslist.
          (apply ~cljcmp ~args)
        true
          (throw (UnsupportedOperationException. "It makes no sense to compare values with and without units!")))))))


(defcmp == clojure.core/==)
(defcmp < clojure.core/<)
(defcmp > clojure.core/>)
(defcmp <= clojure.core/<=)
(defcmp >= clojure.core/>=)

;(clojure.spec.gen/generate (clojure.spec/gen (spec/+ any?)))


(defmacro defsgn [sgn cljsgn]
  (let [a (gensym)]
  `(do
    (spec/fdef ~sgn :args (spec/or :units2.core/amount number?) :ret boolean?)
    (defn ~sgn [~a]
      (if (amount? ~a)
     (do
       (warn (str ~sgn " interacts nontrivially with rescalings of units."))
       (~cljsgn (getValue ~a (getUnit ~a))))
     (~cljsgn ~a))))))

(defsgn zero? clojure.core/zero?)
(defsgn pos? clojure.core/pos?)
(defsgn neg? clojure.core/neg?)

(defn min
  [& args]
  (if (amount? (first args))
    (let [U (getUnit (first args))]
      (->amount (apply clojure.core/min (map #(getValue % U) args)) U))
    (apply clojure.core/min args)
    ))

(defn max
  [& args]
  (if (amount? (first args))
    (let [U (getUnit (first args))]
      (->amount (apply clojure.core/max (map #(getValue % U) args)) U))
    (apply clojure.core/max args)
    ))

(defmacro with-unit-comparisons
  "Locally rebind `>`, `>=`, `<`, `zero?`, `pos?`, ... to unit-aware equivalents."
  [& body]
  (clojure.core/conj body '[== units2.ops/==
         >  units2.ops/>
         <  units2.ops/<
         >= units2.ops/>=
         <= units2.ops/<=
         zero? units2.ops/zero?
         pos?  units2.ops/pos?
         neg?  units2.ops/neg?
         min units2.ops/min
         max units2.ops/max
        ]
     'clojure.core/let))

;; Other `with-unit-...` macros will share the same `(conj body '[bindings] 'let)` hack.
;; I'm only 99.5% sure it's correct, and need to do more testing.
;; Once I'm 100% sure, I'll use a macro to refactor these macros:
;; <pre><code>
;; (defmacro defhack [macroname docstring bindings]
;;   `(defmacro ~namcroname ~docstring [& body]
;;   (clojure.core/conj body '~bindings
;;      'clojure.core/let))) ;; maybe ????
;; </code></pre>
;; but until testing is done I'll avoid macro-inception for the sake of sanity.

;; 14/03/16: Thanks to the `Amsterdam Clojurians` for noticing that `conj` should be `clojure.core/conj` in these macros,
;; with the comment "If you're willing to do that, you should be prepared for users willing to define their own `conj`."


;; ## Arithmetic

;; dispatch for +,-
(defmacro threecond [[a b] both one neither]
  `(cond
    (and (amount? ~a) (amount? ~b))  ~both
    (or  (amount? ~a) (amount? ~b))  ~one
    true                             ~neither))

;; dispatch for +,- with a check that the conversion is linear
(defmacro lincond [[a b] lin nonlin one neither]
  `(threecond [~a ~b]
      (if (linear? (getConverter (getUnit ~a) (getUnit ~b)))
        ~lin
        ~nonlin)
      ~one
      ~neither))

;(spec/fdef + :args ... :ret (spec/or :units2.core/amount number?))

(defn +
  ([] (clojure.core/+)) ; same return value as Clojure.
  ([a] a)
  ([a b]
    (lincond [a b]
      (->amount (clojure.core/+ (getValue a (getUnit a)) (getValue b (getUnit a))) (getUnit a))
      (throw (UnsupportedOperationException. (str "Nonlinear conversion between `" (getUnit a) "' and `" (getUnit b)"'!")))
      (throw (UnsupportedOperationException. (str "It's meaningless to add numbers with and without units! (`" a "' and `" b "' provided)")))
      (clojure.core/+ a b)))
    ([a b & rest] (reduce + (conj rest a b))))

;(spec/fdef - :args ... :ret (spec/or :units2.core/amount number?))

(defn -
  ([] (clojure.core/-)) ; zero arity explicitly an error, but let Clojure catch this.
  ([a] (if (amount? a) (->amount (clojure.core/- (getValue a (getUnit a))) (getUnit a)) (clojure.core/- a)))
  ([a b]
    (lincond [a b]
      (->amount (clojure.core/- (getValue a (getUnit a)) (getValue b (getUnit a))) (getUnit a))
      (throw (UnsupportedOperationException. (str "Nonlinear conversion between `" (getUnit a) "' and `" (getUnit b)"'!")))
      (throw (UnsupportedOperationException. (str "It's meaningless to subtract numbers with and without units! (`" a "' and `" b "' provided)")))
      (clojure.core/- a b)))
    ([a b & rest] (- a (apply + b rest))))


;; dispatch for *,/
(defmacro fourcond [[a b] one two three four]
  `(cond
    (and (amount? ~a) (amount? ~b))       ~one
    (and (amount? ~a) (not (amount? ~b))) ~two
    (and (amount? ~b) (not (amount? ~a))) ~three
    (not (or (amount? ~a) (amount? ~b)))  ~four))

;(spec/fdef * :args ... :ret (spec/or :units2.core/amount number?))

(defn *
  ([] (clojure.core/*)) ; same return value as Clojure.
  ([a] a)
  ([a b] (fourcond [a b]
    (->amount (clojure.core/* (getValue a (getUnit a)) (getValue b (getUnit b))) (times (getUnit a) (getUnit b)))
    (->amount (clojure.core/* (getValue a (getUnit a)) (double b)) (getUnit a))
    (->amount (clojure.core/* (getValue b (getUnit b)) (double a)) (getUnit b))
    (clojure.core/* a b)))
  ([a b & rest] (* a (apply * b rest))))

;(spec/fdef / :args ... :ret (spec/or :units2.core/amount number?))

(defn /
  ([] (clojure.core//)) ; zero arity explicitly an error, but let Clojure catch this.
  ([a] (if (amount? a)
         (->amount (clojure.core// (getValue a (getUnit a))) (inverse (getUnit a)))
         (clojure.core// a)))
  ([a b] (fourcond [a b]
    (->amount (clojure.core// (getValue a (getUnit a)) (getValue b (getUnit b))) (divide (getUnit a) (getUnit b)))
    (->amount (clojure.core// (getValue a (getUnit a)) (double b)) (getUnit a))
    (* (/ b) (double a))
    (clojure.core// a b)))
  ([a b & rest] (/ a (apply * b rest))))

;(spec/fdef + :args [:units2.core/amount :units2.core/amount] :ret number?)

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

(defmacro defratio [ratio clojureratio]
  (let [a (gensym) b (gensym)]
 `(do
    ;;(spec/fdef ~ratio :args [a b] :ret (spec/or :units2.core/amount number?))
    (defn ~ratio [~a ~b]
    (if (amount? ~a)
      (do
        (warn "Modular arithmetic interacts nontrivially with rescalings of units.")
        (if (amount? ~b)
          (->amount (~clojureratio (getValue ~a (getUnit ~a)) (getValue ~b (getUnit ~b))) (divide (getUnit ~a) (getUnit ~b)))
          (->amount (~clojureratio (getValue ~a (getUnit ~a)) ~b) (getUnit ~a))))
      (if (amount? ~b)
        (do
          (warn "Modular arithmetic interacts nontrivially with rescalings of units.")
          (->amount (~clojureratio ~a (getValue ~b (getUnit ~b))) (inverse (getUnit ~b))))
        (~clojureratio ~a ~b)))))))

(defratio rem clojure.core/rem)
(defratio quot clojure.core/quot)



(defmacro with-unit-arithmetic
  "Locally rebinds arithmetic operators like `+`, and `/` to unit-aware equivalents."
  [& body]
  (clojure.core/conj body
        '[+ units2.ops/+
         - units2.ops/-
         * units2.ops/*
         / units2.ops//
         quot units2.ops/quot
         rem  units2.ops/rem
        ]
    'clojure.core/let))


;; ## Powers and Exponentiation

;(spec/fdef expt :args [:units2.core/amount integer?] :ret :units2.core/amount)

(defn expt
  "`(expt b n) == b^n`

  where `n` is an integer and `b` is an amount.
  "
  [b n]
  (if (clojure.core/neg? n)
    (apply / (repeat (+ 2 (- n)) b)) ; ugly hack, but it works fine.
    (apply * (repeat n b))))

;; The functions below should only be defined on dimensionless quantities
;; (to see why, just imagine Maclaurin-expanding the `exp` or `ln` functions)
;; so we define them over ratios of amounts: exp(a/b), log(a/b), etc.

(defmacro defexpt [expt cljexpt]
  (let [a (gensym) b (gensym)]
    `(do
       ;(spec/fdef ~expt ...)
       (defn ~expt
       ([~a]
         (if (amount? ~a)
           (do
             (warn "Exponentiation interacts nontrivially with rescalings of units.")
             (~cljexpt (getValue ~a (getUnit ~a))))
           (~cljexpt ~a)))
       ([~a ~b]
        (if (every? amount? [~a ~b])
          (~cljexpt (divide-into-double ~a ~b))
          (throw (IllegalArgumentException. (str "Arity-2 " ~expt " requires two amounts! (" ~a " and " ~b " provided)")))))))))

(defexpt exp   Math/exp)
(defexpt log   Math/log)
(defexpt log10 Math/log10)

(defn pow
  ([a b]
    (if (amount? a)
      (do
        (warn "pow interacts nontrivially with rescalings of units.")
        (Math/pow (getValue a (getUnit a)) b))
      (Math/pow a b)))
  ([a b c] ;; TODO: check that (every amount? [a b])
   (Math/pow (divide-into-double a b) c)))

(defmacro with-unit-expts
"Locally rebinds exponentiation functions to unit-aware equivalents."
  [& body]
  (clojure.core/conj body
        '[expt units2.ops/expt
          exp units2.ops/exp
          pow units2.ops/pow
          log units2.ops/log
          log10 units2.ops/log10
        ]
      'clojure.core/let))

;; ## Magnitudes

(defmacro defmgn [mgn javamgn]
  (let [a (gensym) b (gensym)]
    `(do
       ;(spec/fdef ~mgn :args :ret)
       (defn ~mgn
       ([~a]
         (if (amount? ~a)
           (do
             (warn "Magnitudes interact nontrivially with rescalings of units.")
             (~javamgn (double (getValue ~a (getUnit ~a)))))
           (~javamgn (double ~a))))
       ([~a ~b]
        (if (every? amount? [~a ~b])
          (~javamgn (double (divide-into-double ~a ~b)))
          (throw (IllegalArgumentException. (str "Arity-2 " ~expt " requires two amounts! ("~a" and "~b" provided)")))))))))

(defmgn abs Math/abs)
(defmgn floor Math/floor)
(defmgn ceil Math/ceil)
(defmgn round Math/round)

(defmacro with-unit-magnitudes
"Locally rebinds magnitude functions to unit-aware equivalents."
  [& body]
  (clojure.core/conj body
        '[abs units2.ops/abs
          floor units2.ops/floor
          ceil units2.ops/ceil
          round units2.ops/round
        ]
      'clojure.core/let))

;; ## EVERYTHING TOGETHER

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
