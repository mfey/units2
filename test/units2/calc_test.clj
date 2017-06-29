(ns units2.calc-test
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [units2.astro :refer :all]
            [units2.ops :as ops]
            [units2.calc :refer :all]))

(defn almost-equal [a b]
  (< 0.999 (if (amount? a) (ops/divide-into-double a b) (/ a b)) 1.001))

(deftest differentiation
  (testing "without units"
    (is (almost-equal 16 (differentiate (fn [x] (* 2 x x)) 4 [1e-4]))))
  (testing "with units"
    (is (almost-equal (m 16.0) (differentiate (fn [x] (ops/* 2 x x)) (m 4) [1e-4])))
    (is (almost-equal (sec 16) (differentiate (fn [x] (ops/* (sec 2) x x)) 4 [1e-4])))
    (is (almost-equal ((inverse m) 16) (differentiate (comp (fn [x] (* 2 x x)) #(getValue % m)) (m 4) [1e-4])))
  )
)

(deftest integration
  (testing "without units"
    (is (almost-equal 15.0 (integrate (fn [x] (* x 2)) [1 4] [1e3]))))
  (testing "with units"
    (is (almost-equal (m 15.0)
           (integrate (fn [x] (ops/* x (m 2)))
                      [1 4] [1e3])))
    (is (almost-equal (m 15.0)
           (integrate (fn [x] (* (getValue x m) 2))
                      [(m 1) (m 4)] [1e3])))
    (is (almost-equal ((times m m) 15.0)
           (integrate (fn [x] (ops/* x 2))
                      [(m 1) (m 4)] [1e3])))
  )
)
