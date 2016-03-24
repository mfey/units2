;; Trigonometry

(ns units2.trig
  (:require [units2.core :refer :all]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit Unit SI NonSI))
)


(set! *warn-on-reflection* true)

(defunit rad (->IFnUnit SI/RADIAN))
(defunit deg (->IFnUnit NonSI/DEGREE_ANGLE))

(defmacro radians-in [trig javatrig]
  (let [a (gensym)]
  `(defn ~trig [~a]
     (if (amount? ~a)
       (~javatrig (getValue ~a rad))
       (throw (Exception. "Trigonometry requires input units of angle"))
       ))))

(radians-in sin Math/sin)
(radians-in cos Math/cos)
(radians-in tan Math/tan)

(defmacro radians-out [trig javatrig]
  `(def ~trig (wrap-out rad #(~javatrig %))))

(radians-out asin Math/asin)
(radians-out acos Math/acos)
(radians-out atan Math/atan)
