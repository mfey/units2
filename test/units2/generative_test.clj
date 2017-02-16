(ns units2.generative-test
  (:require [clojure.test :refer :all]
            [clojure.spec :refer [exercise-fn]]
            [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [units2.astro :refer :all]
            [units2.ops :as ops]))

;; these tests assume [ops/*unit-warnings-are-errors* true]

(deftest gen
  (testing "arithmetic"
    (is (exercise-fn 'units2.ops/+))
    (is (exercise-fn 'units2.ops/-))
    (is (exercise-fn 'units2.ops/*))
    (is (exercise-fn 'units2.ops//))
    (is (exercise-fn 'units2.ops/min))
    (is (exercise-fn 'units2.ops/max))
    )
  (testing "exponential"
    (is (exercise-fn 'units2.ops/expt))
    )
)
