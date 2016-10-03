(ns units2.bake
  (:require [units2.core :refer :all]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit SI NonSI)))

;; ## Temperature [K]

(defunit-with-SI-prefixes K (->IFnUnit SI/KELVIN)) ; not used for baking :-D
(defunit celsius (offset K (K 273.15)))
(defunit fahrenheit (rescale (offset celsius (celsius -17.7777)) (/ 5 9)))

;; A good approximation valid for baking temperatures is
;;  <pre><code>
;;  (defunit approximate-celsius (rescale fahrenheit 2))
;;  </code></pre>

;; ## Weight [M]

(defunit-with-SI-prefixes g (->IFnUnit SI/GRAM))
(defunit lb (rescale g 453.6))

;; ## Fluid Volumes [L^3]

(defunit-with-SI-prefixes L (->IFnUnit NonSI/LITRE))

(defunit ounce_uk (->IFnUnit NonSI/OUNCE_LIQUID_UK))
(defunit ounce_us (->IFnUnit NonSI/OUNCE_LIQUID_US))
(defunit oz ounce_us)

(defunit tablespoon_metric (rescale mL 15))
(defunit tablespoon_us (rescale mL 14.8))
(defunit tbsp tablespoon_us)

(defunit teaspoon_metric (rescale tablespoon_metric (/ 1 3)))
(defunit teaspoon_us  (rescale tablespoon_us (/ 1 3)))
(defunit tsp teaspoon_us)

(defunit cup_metric (rescale g 250))
(defunit cup_us (rescale g 236.5882365)) ; wikipedia

; fun fact: teaspoon of sugar = 15 calories


;; TODO: something computational about the 1:1:1:1 ratio of pound cake.
