(ns units2.IFnUnit-test
  (:require [clojure.test   :refer :all]
            [units2.core    :refer :all]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit SI)))

(defunit m (->IFnUnit SI/METER)) ;; as long as the tests pass, this should be OK.

(deftest helper
  (is (.equals (to-javax units2.IFnUnit.IFnUnit (->IFnUnit SI/METER)) SI/METER))
  (is (.equals (to-javax units2.IFnUnit.IFnUnit SI/METER) SI/METER))
  (is (try (to-javax units2.IFnUnit.IFnUnit 42) (catch Exception e true)))
  )

(deftest protocoladherence
  (is (satisfies? Unitlike m))
  (is (instance? clojure.lang.IFn m))
  (is (satisfies? Multiplicative m))
)

(deftest from-method
  (is (fn? (from m)))
  (is (== 1 ((from m) (m 1)) (getValue (m 1) m))))

(deftest redefinitions
  (testing "rescale"
    (is (= (m (rescale m 1))))
    (is (= (m (rescale (rescale m 0.5) 2))))
  )
  (testing "offset"
    (is (= (m (offset m (m 0)))))
    (is (= (m (offset (offset m (m 7)) (m -7)))))
  )
)
