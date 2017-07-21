(ns units2.generative-test
  (:require [clojure.test :refer :all]
            [clojure.spec :refer [exercise-fn]]
            [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [units2.astro :refer :all]
            [units2.ops :as ops]))

(deftest gen-ops
  (testing "arithmetic"
    (is (exercise-fn 'units2.ops/+))
    (is (exercise-fn 'units2.ops/-))
    (is (exercise-fn 'units2.ops/*))
    (is (exercise-fn 'units2.ops//))
    (is (exercise-fn 'units2.ops/divide-into-double))
    (is (exercise-fn 'units2.ops/min))
    (is (exercise-fn 'units2.ops/max))
    )
  (testing "modular"
    (is (exercise-fn 'units2.ops/rem))
    (is (exercise-fn 'units2.ops/quot))
    (is (exercise-fn 'units2.ops/ceil))
    (is (exercise-fn 'units2.ops/floor))
    (is (exercise-fn 'units2.ops/round))
    (is (exercise-fn 'units2.ops/abs))
    )
  (testing "comparators"
    (is (exercise-fn 'units2.ops/==))
    (is (exercise-fn 'units2.ops/<))
    (is (exercise-fn 'units2.ops/>))
    (is (exercise-fn 'units2.ops/<=))
    (is (exercise-fn 'units2.ops/>=))
    (is (exercise-fn 'units2.ops/zero?))
    (is (exercise-fn 'units2.ops/pos?))
    (is (exercise-fn 'units2.ops/neg?))
    )
  (testing "exponential"
    (is (exercise-fn 'units2.ops/expt))
    (is (exercise-fn 'units2.ops/exp))
    (is (exercise-fn 'units2.ops/log))
    (is (exercise-fn 'units2.ops/log10))
    (is (exercise-fn 'units2.ops/sqrt))
    (is (exercise-fn 'units2.ops/pow))
    )
)
