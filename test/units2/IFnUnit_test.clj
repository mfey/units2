(ns units2.IFnUnit-test
  (:require [clojure.test   :refer :all]
            [units2.core    :refer :all]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit SI)))

(deftest helper
  (is (.equals (to-javax units2.IFnUnit.IFnUnit (->IFnUnit SI/METER)) SI/METER))
  (is (.equals (to-javax units2.IFnUnit.IFnUnit SI/METER) SI/METER))
  (is (try (to-javax units2.IFnUnit.IFnUnit 42) (catch Exception e true)))
  )

(deftest protocoladherence
  (is (satisfies? Unitlike (->IFnUnit SI/METER)))
  ;(is (satisfies? clojure.lang.IFn (->IFnUnit SI/METER)))
  (is (satisfies? Multiplicative (->IFnUnit SI/METER)))
  (is (satisfies? Wrappable (->IFnUnit SI/METER)))
)
