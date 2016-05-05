(ns units2.trig-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]
            [units2.ops :refer [==]]
            [units2.trig :refer :all]))

(deftest definitions
  (is (== (rad 0) (deg 0)))
  (is (== (rad Math/PI) (deg 180)))
)

(deftest composition
  (is (every? amount? [(atan 0) (asin 0) (acos 0)]))
  (is (try (sin 0) (catch Exception e true)))
  (is (try (acos (rad 0)) (catch Exception e true)))
  (let [angle (deg 30)]
    (is (every? number? [(cos angle) (sin angle) (tan angle)]))))
