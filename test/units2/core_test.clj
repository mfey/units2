(ns units2.core-test
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]
            [units2.astro :refer [m]]))

(deftest amount-p
  (testing "amount?"
    (is (amount? (->amount 0 "meter"))) ; constructor is agnostic to actual definition
    (is (amount? (->amount 1 m)))
    ))

(deftest printing
  (testing "machine-readable (if IFn)"
    (is (= '(m 0) (read-string (pr-str (->amount 0 m)))))))
