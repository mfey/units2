(ns units2.generative-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :refer [exercise-fn gen valid?]]
            [clojure.spec.gen.alpha :as gen]
            [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [units2.stdlib :refer :all]
            [units2.ops :as ops]))

(deftest gen-core
  (testing "parser")
)

(deftest gen-units
  (testing "macros")
)

(defmacro spec-check [kw]
  `(is (valid? ~kw (gen/generate (gen ~kw)))))

(deftest gen-stdlib
  (testing "specs"
    (spec-check :units2.stdlib/length)
    (spec-check :units2.stdlib/time)
    (spec-check :units2.stdlib/speed)
    (spec-check :units2.stdlib/velocity)
    (spec-check :units2.stdlib/acceleration)
    (spec-check :units2.stdlib/area)
    (spec-check :units2.stdlib/volume)
    (spec-check :units2.stdlib/mass)
    (spec-check :units2.stdlib/charge)
    (spec-check :units2.stdlib/temperature)
    (spec-check :units2.stdlib/angle)
    (spec-check :units2.stdlib/solidangle)
    (spec-check :units2.stdlib/data-amount)
    (spec-check :units2.stdlib/data-rate)
    (spec-check :units2.stdlib/frequency)
    (spec-check :units2.stdlib/force)
    (spec-check :units2.stdlib/pressure)
    (spec-check :units2.stdlib/energy)
    (spec-check :units2.stdlib/power)
    (spec-check :units2.stdlib/electric-current)
    ))

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
