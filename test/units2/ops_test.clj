(ns units2.ops-test
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [units2.astro :refer :all]
            [units2.ops :as ops]))

(deftest comparisons
  (testing "=="
    (is (ops/== 0 0))
    (is (ops/== (kg 1) (g 1000)))
  )
  (testing "min"
    (is (== 5 (ops/min 5 6 7)))
    (is (ops/== (m 5) (ops/min (m 5) (m 6) (m 7))))
  )
  (testing "max"
    (is (== 7 (ops/max 5 6 7)))
    (is (ops/== (m 7) (ops/max (m 5) (m 6) (m 7))))
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
   (testing "+"
     (is (clojure.core/zero? (ops/+)))
     (is (clojure.core/== (ops/+ 2 2) 4))
     (is (ops/== (ops/+ (m 2) (m 2)) (m 4)))
     )
   (testing "-"
     (is (try (ops/-) (catch clojure.lang.ArityException e true)))
     )
   (testing "*"
     (is (clojure.core/== 1 (ops/*)))
     (is (ops/== (ops/* (sec 6)) (sec 6)))
     (is (ops/== (ops/* (sec 6) (m 2)) ((times sec m) 12)))
     )
  (testing "/"
    (is (try (ops//) (catch clojure.lang.ArityException e true)))
    ;; never divide by zero!!!
    (is (try (ops// 1 0) (catch java.lang.ArithmeticException e true)))
    (is (try (ops// (sec 1) 0) (catch java.lang.ArithmeticException e true)))
    (is (try (ops// 1 (m 0)) (catch java.lang.ArithmeticException e true)))
    (is (try (ops// (sec 1) (m 0)) (catch java.lang.ArithmeticException e true)))
  )
  (testing "div-into-double"
    (is (number? (ops/divide-into-double (m 1) (m 1))))
    ;(is (ops/divide-into-double (m 1) (m 0)))
    (is (== (/ 3 9) (ops/divide-into-double (m 3) (m 9)))))
  ;(testing "rem")
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
    (is (= 1.0 (ops/exp 0))))
  (testing "logarithms"
    (is (= 0.0 (ops/log 1) (ops/log10 1)))
    (is (= 1.0 (ops/log10 10)))
    (is (= 3.0 (ops/log10 (km 1) (m 1)))))
  (testing "expt"
    (is (ops/== (ops/expt (m 2) 3) ((power m 3) 8))))
  (testing "pow"
    (is (= 1.0 (ops/pow (m 1) (m 4) 0)))
    (is (= 1.0 (ops/pow (m 1) (m 1) 1) (ops/pow (m 1) (m 1) 6)))
    (is (= 4.0 (ops/pow (m 1) (cm 50) 2))))
  (testing "exponentiation-macro"
    (ops/with-unit-expts
      (is (= 0.0 (log 1) (log10 1)))
      (is (= 1.0 (log10 10)))
      (is (= 3.0 (log10 (km 1) (m 1))))
      ;etc
    )
  )
)

(deftest magnitudes
  (testing "round"
    (is (= 4 (ops/round (m 8.1) (m 2)))))
  (testing "magnitude-macro"
    (is (= 4   (ops/with-unit-magnitudes (round (m 8.1) (m 2)))))
    (is (= 4.0 (ops/with-unit-magnitudes (floor (m 8.1) (m 2))))))
)

;(deftest everything-together)
