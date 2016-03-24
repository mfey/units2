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
    (is (ops/with-unit-comparisons (== 0 0)))
  ))

(deftest arithmetic
   (testing "+"
     (is (clojure.core/== (ops/+ 2 2) 4))
     (is (ops/== (ops/+ (m 2) (m 2)) (m 4)))
     (is (clojure.core/zero? (ops/+)))
     )
   (testing "-"
     (is (try (ops/-) (catch clojure.lang.ArityException e true)))
     )
   (testing "*"
     (is (clojure.core/== 1 (ops/*)))
     (is (ops/== (ops/* (sec 6) (sec 6))))
     )
  (testing "/"
    (is (try (ops//) (catch clojure.lang.ArityException e true)))
    )
  (testing "rem")
  (testing "quot")
  (testing "arithmetic-macro"
    (ops/with-unit-arithmetic
    (is (ops/== (+ (sec 2) (sec 1))) (sec 3))
    (is (clojure.core/zero? (ops/+)))))
)

(deftest exponentiation
;;   (testing "pow")
;;   (testing "exp"
;;     (is (= Math/E (ops/exp 1)))
;;     (is (= 1.0 (ops/exp 0))))
   (testing "logarithms"
     (is (= 0.0 (ops/log 1) (ops/log10 1)))
     (is (= 1.0 (ops/log10 10)))
     (is (= 3.0 (ops/log10 (km 1) (m 1))))
   )
;;   (testing "expt"
;;     (is (= (ops/expt 2 3) 8))
;;     )
;;   (testing "exponentiation-macro")
;; ; TODO: check that converting to standard dimensionless unit is the most intuitive thing to do...
)

(deftest magnitudes
  (testing "round"
    (is (= 4 (ops/round (m 8.1) (m 2))))
  )
  (testing "magnitude-macro"
    (is (= 4   (ops/with-unit-magnitudes (round (m 8.1) (m 2)))))
    (is (= 4.0 (ops/with-unit-magnitudes (floor (m 8.1) (m 2)))))
    )
)

(deftest everything-together)
