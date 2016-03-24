(ns units2.prob
  (:require [units2.core :refer :all]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit Unit SI NonSI)
           ))

(set! *warn-on-reflection* true)

;; ## Information

(defunit bit (->IFnUnit SI/BIT))
(defunit nat (rescale bit (/ (Math/log 2))))


;; ## Probability

;(defunit percent)

;; ## Odds (ratio of probabilities)

;; ## Odds ratio (ratio of odds)

;; ## No units

(defunit dimensionless (divide bit bit))
