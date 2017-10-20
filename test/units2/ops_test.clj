(ns units2.ops-test
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [units2.stdlib :refer :all]
            [units2.ops :as ops]))

(defn almost-equal [a b]
  (< 0.999 (if (amount? a) (ops/divide-into-double a b) (/ a b)) 1.001))

(deftest comparisons
  (testing "=="
    (is (ops/== 0 0))
    (is (ops/== (kg 1) (g 1000)))
    (is (thrown? java.lang.UnsupportedOperationException (ops/== (g 0) (m 0))))
    (is (thrown? java.lang.UnsupportedOperationException (ops/== 0 (m 0))))
  )
  (testing "min"
    (is (== 5 (ops/min 5 6 7)))
    (is (ops/== (m 5) (ops/min (m 5) (m 6) (m 7))))
    (is (try (ops/min 6 (m 5)) (catch java.lang.UnsupportedOperationException e true)))
    (is (try (ops/min (kg 6) (m 5)) (catch java.lang.UnsupportedOperationException e true)))
  )
  (testing "max"
    (is (== 7 (ops/max 5 6 7)))
    (is (ops/== (m 7) (ops/max (m 5) (m 6) (m 7))))
    (is (try (ops/max 6 (m 5)) (catch java.lang.UnsupportedOperationException e true)))
    (is (try (ops/max (kg 6) (m 5)) (catch java.lang.UnsupportedOperationException e true)))
  )
  (testing "empty-arglist" ; should reduce to the clojure function's behaviour
    (is (thrown? clojure.lang.ArityException (ops/==)))
    (is (thrown? clojure.lang.ArityException (ops/<)))
    (is (thrown? clojure.lang.ArityException (ops/>)))
    (is (thrown? clojure.lang.ArityException (ops/<=)))
    (is (thrown? clojure.lang.ArityException (ops/>=)))
    (is (thrown? clojure.lang.ArityException (ops/min)))
    (is (thrown? clojure.lang.ArityException (ops/max)))
    )
  (testing "comparison-macro"
    (ops/with-unit-comparisons
      (is (== 0 0))

      (is (== (m 3) (m 3)))
      (is (<= (m 3) (m 3)))
      (is (>= (m 3) (m 3)))

      (is (<= (m 3) (m 5)))
      (is (<  (m 3) (m 5)))
      (is (>= (m 5) (m 3)))
      (is (>  (m 5) (m 3)))
    )
  )
)

(deftest arithmetic
   (testing "+ (functionality)"
     (is (every? clojure.core/zero? [(ops/+) (ops/+ 0) (ops/+ 0 0) (ops/+ 0 0 0)]))
     (is (clojure.core/== (ops/+ 2 2) (ops/+ 1 1 2) 4))
     (is (ops/== (ops/+ (m 2) (m 2)) (ops/+ (m 1) (m 1) (m 2)) (m 4)))
    )
   (testing "+ (exceptions)"
     (is (thrown? java.lang.UnsupportedOperationException (ops/+ (m 4) 5)))
     (is (thrown? java.lang.UnsupportedOperationException (ops/+ 4 (m 5))))
     (is (thrown? java.lang.UnsupportedOperationException (ops/+ (fahrenheit 1) (celsius 1))))
     )
   (testing "- (functionality)"
     (is (every? clojure.core/zero? [(ops/- 2 2) (ops/- 2 1 1)  (ops/- 0) (ops/- 0 0) (ops/- 0 0 0)]))
     (is (clojure.core/== 3 (ops/- 5 2) (ops/- 5 1 1) (ops/- -3)))
     (is (ops/== (m 0) (ops/- (m 2) (m 2)) (ops/- (m 0)) (ops/- (m 2) (m 1) (m 1))))
     (is (ops/== (sec 3) (ops/- (sec 5) (sec 2)) (ops/- (sec 5) (sec 1) (sec 1)) (ops/- (sec -3))))
     )
   (testing "- (exceptions)"
     (is (try (ops/-) (catch clojure.lang.ArityException e true)))
     (is (try (ops/- (m 4) 5) (catch java.lang.UnsupportedOperationException e true)))
     (is (try (ops/- 4 (m 5)) (catch java.lang.UnsupportedOperationException e true)))
     (is (try (ops/- (fahrenheit 1) (celsius 1)) (catch java.lang.UnsupportedOperationException e true)))
     )
   (testing "* (functionality)"
     (is (clojure.core/== 1 (ops/*) (ops/* 1) (ops/* 1 1) (ops/* 1 1 1)))
     (is (ops/== (ops/* (sec 6)) (sec 6)))
     (is (ops/== (ops/* (sec 6) (m 2)) (ops/* (sec 3) 2 (m 2)) ((times sec m) 12)))
     )
  (testing "* (exceptions)"
    ;; ops/* should not generate exceptions, unless you're TRYING to break it.
    )
  (testing "/ (functionality)"
    (is (== 1 (ops// 1) (ops// 1 1) (ops// 1 1 1)))
    (is (== 0.5 (ops// 1 2) (ops// 1 1 2) (ops// 1 1 1 2)))
    (is (ops/== ((inverse sec) 0.5) (ops// (sec 2)) (ops// 1 (sec 2)) (ops// 2 2 (sec 2)) (ops// 2 (sec 2) 2)))
    )
  (testing "/ (exceptions)"
    (is (try (ops//) (catch clojure.lang.ArityException e true)))
    ;; never divide by zero!!!
    (is (thrown? java.lang.ArithmeticException (ops// 1 0)))
    (is (thrown? java.lang.ArithmeticException (ops// (sec 1) 0)))
    (is (thrown? java.lang.ArithmeticException (ops// 1 (m 0))))
    (is (thrown? java.lang.ArithmeticException (ops// (sec 1) (m 0))))
  )
  (testing "div-into-double (functionality)"
    (is (number? (ops/divide-into-double (m 1) (m 1)))) ; does it do what's advertised and produce a double?
    (is (== (/ 3 9) (ops/divide-into-double (m 3) (m 9))))
    (is (== 1.0 (ops/divide-into-double (celsius 6) (celsius 6))
                (ops/divide-into-double (fahrenheit 1)(fahrenheit 1))))
  )
  (testing "div-into-double (exceptions)"
    (is (try (ops/divide-into-double 4 5) (catch java.lang.IllegalArgumentException e true)))
    (is (try (ops/divide-into-double (m 4) 5) (catch java.lang.IllegalArgumentException e true)))
    (is (try (ops/divide-into-double 4 (sec 5)) (catch java.lang.IllegalArgumentException e true)))
    (is (try (ops/divide-into-double (celsius 6) (fahrenheit (celsius 6))) (catch java.lang.IllegalArgumentException e true)))
    (is (try (ops/divide-into-double (m 1) (m 0)) (catch java.lang.Exception e true)))
    )
  (testing "rem"
    (is (== 1 (ops/rem 7 3)))
    (is (ops/== (minute 30) (ops/rem (hour 1.5) (hour 1) (hour 1))))
    (is (thrown? java.lang.UnsupportedOperationException (ops/rem (hour 1) 60)))
    (is (thrown? java.lang.UnsupportedOperationException (ops/rem 60 (hour 1))))
  )
  ;(testing "quot")
  (testing "arithmetic-macro"
    (ops/with-unit-arithmetic
      (is (clojure.core/zero? (+)))
      (is (ops/== (+ (sec 2) (sec 1))) (sec 3))
    ))
)

(deftest exponentiation
  (testing "exp"
    (is (= Math/E (ops/exp 1)))
    (is (= 1.0 (ops/exp 0)))
    (is (= Math/E (ops/exp (m 1) (m 1))))
    (is (thrown? java.lang.IllegalArgumentException (ops/exp (m 1) 7)))
    (is (thrown? java.lang.UnsupportedOperationException (ops/exp (m 1))))
  )
  (testing "logarithms"
    (is (= 0.0 (ops/log 1) (ops/log10 1)))
    (is (= 1.0 (ops/log10 10)))
    (is (= 3.0 (ops/log10 (km 1) (m 1))))
    (is (try (ops/log (m 1) 7) (catch java.lang.IllegalArgumentException e true)))
  )
  (testing "expt"
    (is (ops/== (ops/expt (m 2) 3) ((power m 3) 8))))
  (testing "pow"
    (is (= 1.0 (ops/pow 1 (rand))))
    (is (= 1.0 (ops/pow (m 1) (m 4) 0)))
    (is (= 1.0 (ops/pow (m 1) (m 1) 1) (ops/pow (m 1) (m 1) 6)))
    (is (= 4.0 (ops/pow (m 1) (cm 50) 2)))
    (is (thrown? java.lang.IllegalArgumentException (ops/pow (m 1) (m 1))))
    (is (thrown? java.lang.IllegalArgumentException (ops/pow (m 1) 1)))
    (is (thrown? java.lang.IllegalArgumentException (ops/pow 1 (m 1))))
    (is (thrown? java.lang.IllegalArgumentException (ops/pow 1 (m 1) 1)))
    (is (thrown? java.lang.IllegalArgumentException (ops/pow (m 1) 1 1)))
    (is (thrown? java.lang.IllegalArgumentException (ops/pow (m 1) (sec 1) 1)))
    (is (thrown? java.lang.IllegalArgumentException (ops/pow (m 1) (m 1) (m 1))))
  )
  (testing "sqrt"
    (is (= 2.0 (ops/sqrt (m 4) (m 1)) (ops/sqrt (m 8) (m 2))))
    )
  (testing "exponentiation-macro"
    (ops/with-unit-expts
      (is (= 0.0 (log 1) (log10 1)))
      (is (= 1.0 (log10 10)))
      (is (= 3.0 (log10 (km 1) (m 1))))
      ;etc
    )
  )
)

(deftest sign-functions
  (testing "sign-functions (functionality)"
    (is (ops/zero? 0))
    (is (not (ops/zero? 1)))
  )
  (testing "sign-functions (exceptions)"
    (is (thrown? java.lang.UnsupportedOperationException (ops/pos? (m (rand)))))
    (is (thrown? java.lang.UnsupportedOperationException (ops/neg? (m (rand)))))
    (is (thrown? java.lang.UnsupportedOperationException (ops/zero? (m (rand)))))
  )
)

(deftest modular-arithmetic
  (testing "round"
    (is (== 1 (ops/round 1.2)))
    (is (ops/== (m 100) (ops/round (m 124) (m 100)))) ; round to the nearest 100
    (is (ops/== (m 8.0) (ops/round (m 8.1) (m 2)))))  ; round to the nearest "2"
  (testing "abs"
    (is (int? (ops/abs 1)))   ; checking
    (is (double? (ops/abs 1.0)))  ; types...
    (is (== 2 (ops/abs 2) (ops/abs -2)))
    (let [a (rand)
          b (rand)]
      (is (almost-equal (m a) (ops/abs (m a) (m b))))
      (is (almost-equal (m a) (ops/abs (m (- a)) (m b)))))
    (is (thrown? java.lang.UnsupportedOperationException (ops/abs (m 4))))
    )
  (testing "macro"
    (is (ops/== (m 8.0) (ops/with-unit-magnitudes (round (m 8.1) (m 2)))))
    (is (ops/== (m 8.0) (ops/with-unit-magnitudes (floor (m 8.1) (m 2)))))
    (is (ops/== (m 10.0) (ops/with-unit-magnitudes (ceil (m 8.1) (m 2))))))
)

(deftest everything-together
  (testing "macro"
    (is (== 4 (ops/with-unit- [:math] (+ 2 2))))
    (is (ops/== (m 4) (ops/with-unit- [:math] (+ (m 2) (m 2)))))
    )
  )
