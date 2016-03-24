(ns units2.calc-test
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [units2.astro :refer :all]
            [units2.ops :as ops]
            [units2.calc :refer :all]))

(deftest differentiation
  (is (< 15.9 (differentiate (fn [x] (* 2 x x)) 4) 16.1))
  ; fourth option to test too...
  (is (ops/< (m 15.9) (differentiate (fn [x] (ops/* 2 x x)) (m 4)) (m 16.1)))
  (is (ops/< (m 15.9) (differentiate (fn [x] (ops/* (m 2) x x)) 4) (m 16.1)))
)

(deftest integration
  (testing "without units"
    (is (== 15.0 (integrate (fn [x] (* x 2)) [1 4]))))
  (testing "with units"
    (is (ops/== (m 15.0)
           (integrate (fn [x] (ops/* x (m 2)))
                      [1 4])))
    (is (ops/== (m 15.0)
           (integrate (fn [x] (* (getValue x m) 2))
                      [(m 1) (m 4)])))
    (is (ops/== ((times m m) 15.0)
           (integrate (fn [x] (ops/* x 2))
                      [(m 1) (m 4)])))
  )
)
