(ns units2.core-test
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]))

(deftest amount-p
  (testing "amount?"
    (is (amount? (->amount 0 0)))
    ))
