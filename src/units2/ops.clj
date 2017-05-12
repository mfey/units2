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
;;   <pre><code> ;; these two expressions are equivalent
;;   (pow a b c)
;;   (Math/pow (divide-into-double a b) c)
;;   </code></pre>
;;

;; ### TODO: add/fix docstrings (exp, log, log10, pow)

;; ### TODO: add/fix function specs (exp, log, log10, abs, floor, ceil, round)

;; ### TODO: Improve errors (min, max, ...)

(ns units2.ops
  ;redefinitions imminent! `:exclude` turns off some WARNINGS.
  (:refer-clojure :exclude [+ - * / rem quot == > >= < <= zero? pos? neg? min max])
  (:require [units2.core :refer :all]
            [clojure.spec :as spec]
            [clojure.spec.gen :as gen]) ; TODO: spec these functions!!!
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

(declare zero?); this spec uses the unit-aware zero? function.

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
            #(binding [*unit-warnings-are-errors* false
                       *unit-warnings-are-printed* false]
               (not (zero? %)))))

(spec/def ::two-linear-units-second-nonzero
  (spec/and (spec/coll-of :units2.core/amount :count 2)
            ::linear-units
            #(spec/valid? ::nonzero-amount (second %))))

;; this spec correctly sees the safety switch in
;;   (binding [*...* false] (valid? ...))
;; but the generator only returns numbers (no amounts), so we can't generatively
;; test the `unsafe' behaviour. This kind of defeats the purpose of generative
;; tests, but turning the safety off isn't something I'd recommend to users.
;;
(spec/def ::number-unless-safety-is-off
  (spec/with-gen
    (spec/and ::number-or-amount
              #(if *unit-warnings-are-errors*
                  (do
                   ; (print "true branch validation")
                    (number? (spec/unform ::number-or-amount %)))
                  (do
                   ; (print "false branch validation")
                    true)))
    (fn []
      (if *unit-warnings-are-errors*
             (spec/gen number?)
             (do
              ; (print "false branch generator")
               (spec/gen ::number-or-amount))
             ))))
;;
;; the "print statement analysis" shows that we correctly employ the
;; `false branch' generator when we rebind, but then in the function
;; `gensub` of `spec.clj` there's a second-pass `(valid? spec %)` that
;; chooses the true branch for validation (it doesn't see the rebinding!)

;; Now, if we `(alter-var-root #'*unit-warnings-are-errors* (constantly false))
;; at the repl then we get the generator behaviour we want on rebinding... because
;; statefulness is magic... but even then (exercise-fn `zero?) fails anyway.
;; NOT WORTH IT.

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
(defmacro defcmp [cmp cljcmp]
  (let [args (gensym)
        warning "It makes no sense to compare values in incompatible units!"
        docstr (str "Official Clojure doc: " (eval `(:doc (meta (var ~cljcmp))))
                    "\n  Units2 added doc: " warning)
        ]
`(do
   (spec/fdef ~cmp :args ::some-numbers-or-compatible-amounts
                   :ret boolean?)
   (defn ~cmp ~docstr [& ~args]
      (cond
        (empty? ~args)
          (~cljcmp) ;; get the same edge case as regular clojure
        (every? amount? ~args)
          (if (every? #(compatible? (getUnit (first ~args)) %) (map getUnit (rest ~args)))
            (apply ~cljcmp (map (from (getUnit (first ~args))) ~args))
            (throw (UnsupportedOperationException. ~warning)))
        (every? #(not (amount? %)) ~args) ; empty argslist caught above (to get the same Exception subclass).
          (apply ~cljcmp ~args)
        true
          (throw (UnsupportedOperationException. ~warning)))))))


(defcmp == clojure.core/==)
(defcmp < clojure.core/<)
(defcmp > clojure.core/>)
(defcmp <= clojure.core/<=)
(defcmp >= clojure.core/>=)

;; `zero?`, `pos?`, and `neg?` can return different answers depending on the input units
;; (to see why, consider the Farenheit and Celius temperature scales)
(defmacro defsgn [sgn cljsgn]
  (let [a (gensym)
        warning " interacts nontrivially with rescalings/offsets of units."
        docstr (str "Official Clojure doc: " (eval `(:doc (meta (var ~cljsgn))))
                    "\n  Units2 added doc:" warning)]
  `(do
    (spec/fdef ~sgn
      :args ::number-unless-safety-is-off
      :ret boolean?)
    (defn ~sgn ~docstr [~a]
      (if (amount? ~a)
         (do
           (warn (str ~sgn ~warning))
           (~cljsgn (getValue ~a (getUnit ~a))))
         (~cljsgn ~a))))))

(defsgn zero? clojure.core/zero?)
(defsgn pos? clojure.core/pos?)
(defsgn neg? clojure.core/neg?)

;(zero? 0)


;(spec/exercise (:args (spec/get-spec `units2.ops/pos?)))

;; We get  `(binding [units2.ops/*unit-warnings-are-errors* false] (spec/valid? (:args (spec/get-spec units2.ops/zero?)) (m 7)))`
;; but not `(binding [units2.ops/*unit-warnings-are-errors* false] (gen/sample (spec/gen :units2.ops/number-unless-safety-is-off)))`


;; NB: `(min)` and `(max)` cascade down to their clojure.core equivalents, which throw ArityExceptions.

(spec/fdef min :args ::some-numbers-or-compatible-amounts
               :ret  ::number-or-amount)

(defn
  ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/min)))
              "\n  Units2 added doc: It works for amounts with compatible units, too.")}
  min
  [& nums]
  (if (amount? (first nums))
    (let [U (getUnit (first nums))]
      (->amount (apply clojure.core/min (map (from U) nums)) U))
    (apply clojure.core/min nums)
    ))

(spec/fdef max :args ::some-numbers-or-compatible-amounts
               :ret  ::number-or-amount)

(defn
  ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/max)))
              "\n  Units2 added doc: It works for amounts with compatible units, too.")}
  max
  [& nums]
  (if (amount? (first nums))
    (let [U (getUnit (first nums))]
      (->amount (apply clojure.core/max (map (from U) nums)) U))
    (apply clojure.core/max nums)
    ))

(defmacro with-unit-comparisons
  "Locally rebind `==`, `>=`, `<`, `zero?`, `pos?`, `min`, ... to unit-aware equivalents."
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

(spec/fdef + :args ::numbers-or-compatible-amounts :ret ::number-or-amount)

(defn
  ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/+)))
              "\n  Units2 added doc: It works for amounts with units, too.")}
  +
  ([] (clojure.core/+)) ; same return value as Clojure.
  ([a] a)
  ([a b]
    (lincond [a b]
      (->amount (clojure.core/+ (getValue a (getUnit a)) (getValue b (getUnit a))) (getUnit a))
      (throw (UnsupportedOperationException. (str "Nonlinear conversion between `" (getUnit a) "' and `" (getUnit b)"'!")))
      (throw (UnsupportedOperationException. (str "It's meaningless to add numbers with and without units! (`" a "' and `" b "' provided)")))
      (clojure.core/+ a b)))
    ([a b & rest] (reduce + (conj rest a b))))


(spec/fdef - :args ::some-numbers-or-compatible-amounts
             :ret ::number-or-amount)

(defn
  ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/-)))
              "\n  Units2 added doc: It works for amounts with units, too.")}
  -
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

(spec/fdef *
  :args (spec/* (spec/or :amount :units2.core/amount :number number?))
  :ret (spec/or :amount :units2.core/amount :number number?)
  ; :fn (if (every? number? args), returns a number, else an amount)
  )

(defn
  ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core/*)))
              "\n  Units2 added doc: It works for amounts with units, too.")}
  *
  ([] (clojure.core/*)) ; same return value as Clojure.
  ([a] a)
  ([a b] (fourcond [a b]
    (->amount (clojure.core/* (getValue a (getUnit a)) (getValue b (getUnit b))) (times (getUnit a) (getUnit b)))
    (->amount (clojure.core/* (getValue a (getUnit a)) (double b)) (getUnit a))
    (->amount (clojure.core/* (getValue b (getUnit b)) (double a)) (getUnit b))
    (clojure.core/* a b)))
  ([a b & rest] (* a (apply * b rest))))

(spec/fdef /
  :args (spec/and (spec/+ ::number-or-amount)
                  (fn [x] ; also: don't divide by zero!!!
                    (binding [*unit-warnings-are-errors* false]
                      (if (empty? (rest x))
                        (not (zero? (spec/unform ::number-or-amount (first x))))
                        (every? #(not (zero? (spec/unform ::number-or-amount %))) (rest x))))))
  :ret (spec/or :amount :units2.core/amount :number number?)
  ; :fn (if (every? number? args), returns a number, else an amount)
  )

(defn
  ^{:doc (str "Official Clojure doc: " (:doc (meta (var clojure.core//)))
              "\n  Units2 added doc: It works for amounts with units, too.")}
  /
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
  (let [a (gensym) b (gensym)
        warning "Modular arithmetic interacts nontrivially with rescalings of units."
        docstr (str "Official Clojure doc: " (eval `(:doc (meta (var ~clojureratio))))
                    "\nUnits2 added doc: " warning)
        ]
 `(do
    (spec/fdef ~ratio :args ::two-linear-units-second-nonzero :ret (spec/or :units2.core/amount number?))
    (defn ~ratio ~docstr [~a ~b]
    (if (amount? ~a)
      (do
        (warn ~warning)
        (if (amount? ~b)
          (->amount (~clojureratio (getValue ~a (getUnit ~a)) (getValue ~b (getUnit ~b))) (divide (getUnit ~a) (getUnit ~b)))
          (->amount (~clojureratio (getValue ~a (getUnit ~a)) ~b) (getUnit ~a))))
      (if (amount? ~b)
        (do
          (warn ~warning)
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

;; The exponential functions below should only be defined on dimensionless quantities
;; (to see why, just imagine Maclaurin-expanding the `exp` or `ln` functions)
;; so we define them over ratios of amounts: exp(a/b), log(a/b), etc.
(defmacro defexpt [expt cljexpt]
  (let [a (gensym) b (gensym)
        docstr (str "`" (str expt) "` is only sensible for dimensionless quanities.")
        ]
    `(defn ~expt ~docstr
       ([~a]
         (if (amount? ~a)
           (do
             (warn "Exponentiation interacts nontrivially with rescalings of units.")
             (~cljexpt (getValue ~a (getUnit ~a))))
           (~cljexpt ~a)))
       ([~a ~b]
        (if (every? amount? [~a ~b])
          (~cljexpt (divide-into-double ~a ~b))
          (throw (IllegalArgumentException. (str "Arity-2 `" ~expt "` requires two amounts! (" ~a " and " ~b " provided)"))))))))

; def specs separately (for function ranges!)

(defexpt exp   Math/exp)
(defexpt log   Math/log)
(defexpt log10 Math/log10)

(spec/fdef pow :args (spec/or :bin (spec/tuple ::number-unless-safety-is-off number?) ;; safety can be off!!
                              :tri ;(spec/with-gen
                                     (spec/and (spec/tuple :units2.core/amount :units2.core/amount number?)
                                               (fn [[a b _]] (spec/valid? ::two-linear-units-second-nonzero [a b])))
                                   ; (fn [] (gen/cat (spec/gen ::two-linear-units-second-nonzero)
                                   ;                 (gen/fmap list (gen/gen-for-pred number?)))))
                          )

               :ret double?)

;; ??? (let [a (into (first (spec/exercise ::nonzero-amount)) [0.8])]
;; ??? [(spec/valid? (:args (spec/get-spec `pow)) a) a])

;; (spec/exercise (:args (spec/get-spec `pow))) ; works seldomly, but sometimes

;; ??? (into [] (gen/generate (gen/cat (spec/gen ::two-linear-units-second-nonzero)
;; ???         (gen/fmap list (gen/gen-for-pred number?)))))


;; toggle true/false to check that amounts are valid when the safety is off (even if not autogenerated)
;(binding [*unit-warnings-are-errors* false]
;  (spec/valid? (:args (spec/get-spec `pow)) [(first (first (spec/exercise :units2.core/amount))) 8]))

(defn pow
  "TODO: docstring"
  ([a b]
    (if (amount? a)
      (do
        (warn "pow interacts nontrivially with rescalings of units.")
        (Math/pow (getValue a (getUnit a)) b))
      (Math/pow a b)))
  ([a b c]
     (if (and (amount? a) (amount? b))
       (Math/pow (divide-into-double a b) c)
       (throw (IllegalArgumentException. (str "Arity-2 `pow` requires two amounts! (" a " and " b " provided)"))))))

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
  (let [a (gensym) b (gensym)
        docstr (str "`" mgn "` is only sensible for dimensionless quantities.")
        ]
    `(do
       ;(spec/fdef ~mgn :args :ret :fn)
       (defn ~mgn ~docstr
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
