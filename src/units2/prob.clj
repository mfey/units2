(ns units2.prob
  (:require [units2.core :refer :all]
            [units2.IFnUnit :refer :all]))

(set! *warn-on-reflection* true)

;; ## Information

(defunit nat (makeaseunit "information"))
(defunit bit (rescale nat (Math/log 2)))

;; ## Probability

(defunit Kolmogorov (makebaseunit "probability"))
(defunit percent (rescale Kolmogorov 0.01))

;; ## No units

(defunit dimensionless (divide bit bit))


; p-value to number of sigma
; surprisal to p-value

; (def Jeffreys Scale [list of maps of bayesfactor/information/english])
