(ns units2.core-test
  (:require [clojure.test :refer :all]
            [units2.core :refer :all]
            [units2.stdlib :refer [m sec]]))

(deftest amount-p
  (testing "amount?"
    (is (amount? (->amount 1 m)))
    (is (not (amount? (->amount 0 "meter"))))
    (is (amount? (->amount "six" m))) ; I'm not sure if this should be allowed or excluded... more freedom to the user I guess?
  ))

(deftest printing
  (testing "machine-readable (if IFn)"
    (is (= '(m 0) (read-string (pr-str (->amount 0 m)))))
    (is (= '(m 5) (read-string (pr-str (m 5)))))
    ))

(deftest linear
  (testing "linear?"
    (is (linear? #(* 3 %)))
    (is (linear? identity))
    (is (not (linear? #(+ 3 %))))
  )
)

(deftest parser
  (testing "parse-unit (functionality)"
    (is (instance? units2.core.Unitlike (parse-unit {m 1})))
    (is (instance? units2.core.Unitlike (parse-unit [m 1])))
    (is (instance? units2.core.Unitlike (parse-unit {m 1 sec -2})))
    (is (instance? units2.core.Unitlike (parse-unit [m 1 sec -2])))
    ;(is (instance? units2.core.Unitlike (parse-unit "{units2.stdlib/m 1}"))) ;; these work unqualified
    ;(is (instance? units2.core.Unitlike (parse-unit "[units2.stdlib/m 1]"))) ;; at the REPL... why not here?
    ;(is (instance? units2.core.Unitlike (parse-unit "{m 1}"))) ;--> no good !?!
    ;(is (instance? units2.core.Unitlike (parse-unit "[m 1]"))) ;--> no good !?!
  )
  (testing "parse-unit (exceptions)"
    ;(is (thrown? java.lang.RuntimeException (parse-unit "#=(/ 0)"))) ; dodge an unsafe read-string
    (is (thrown? IllegalArgumentException (parse-unit true)))
    (is (thrown? IllegalArgumentException (parse-unit {})))
    (is (thrown? IllegalArgumentException (parse-unit [])))
    ;(is (thrown? IllegalArgumentException (parse-unit "()")))
    ;(is (thrown? IllegalArgumentException (parse-unit "[]")))
    ;(is (thrown? IllegalArgumentException (parse-unit "{}")))
    ;(is (thrown? IllegalArgumentException (parse-unit "true")))
    (is (thrown? IllegalArgumentException (parse-unit {m 1.2}))) ; integer exponents
    (is (thrown? IllegalArgumentException (parse-unit {+ 7})))   ; Unitlike units
    ; TODO: test the validation for "multiplicative"
  )
  )
