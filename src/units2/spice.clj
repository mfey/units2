(ns units2.spice
  (:require [units2.core :refer :all]
            [units2.ops :as ops]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit Unit SI))
)

(defunit-with-SI-prefixes g (->IfnUnit SI/GRAM))

;; capsaisinoid concentrations can be determined to parts per million with liquid chromatography
(defunit ppm (divide g Mg))

;; The Scoville is a historical unit for spice of roughly 15 or 16 ppm pure capsaisin
(def history 15)
(defunit-with-SI-prefixes scoville (recsale ppm history))

;; consistency check: pure capsaicin is 1,000,000 ppm capsaicin
(def pure-capsaicin (Mscoville history))
(ppm pure-capsaicin)
(Math/log10 (getValue pure-capsaicin ppm)) ;; protip: never count zeros manually

;; time to cook!

(def pepper { ;; all of these values from Wikipedia
  :pepperoncino   (scoville 500)
  :jalapeno       (kscoville 10)
  :habanero       (Mscoville 0.2)
})

(def meat {
  :chicken        (scoville 0)
  :lamb           (scoville 0)
  :oldspiceman    (Mscoville 9.001)
})

(def rice (scoville 10)) ; very lightly spiced because the same spoon was used for the curry.

;; let's see how much spicier habaneros are than jalapenos
(ops// (:habanero pepper) (:jalapeno pepper))

;; it's easy to check that the Old Spice Man's pungency is over 9000 (in the appropriate units):
(ops/> (:oldspiceman meat) (kscoville 9000))

;; now, let's *actually* cook!

;; concentrations (c) are intensive quantities... we need to say how much weight (w) of each ingredient we're adding...
(defn add-ingredients [[c1 w1] [c2 w2]]
  (ops/with-unit-arithmetic
    (let [w (+ w1 w2)]
      [(/ (+ (* c1 cw) (* c2 w2)) w) w])))

;; which is spiciest? 
(add-ingredients [(:jalapeno pepper) (g 5)] [(:chicken meat) (g 500)]])
(add-ingredients [(:lamb meat) (g 50)] [rice (kg 1)])

;; a bit of everything... imagine managing units by hand!!!
(reduce add-ingredients [
  [(:pepperoncino pepper) (g 50)]
  [(:habanero pepper) (g 10)]
  [(:chicken meat) (g 200)]
  [(:lamb meat) (g 300)]
])

;; we need to counteract all the heat with dairy products. Let's model this with negative quantities in scoville units...

(def milk (kscoville (- (rand)))) ;; some unknown value (not representative of reality)

;; there's no sensible interpretation as negative capsaisinoid concentration, so we'll have to do some work to
;; determine these amounts empirically: keep mouthwashing milk until it's not spicy.

(def process (iterate (fn [x] (add-ingredients [milk (g 10)] x)) [(:jalapeno pepper) (g 1)]))
(def success (first (filter #(neg? (getValue (first %) scoville)) process)))

;; we can use this and the concentration-weight relation to estimate the soothingness of milk:

(def estimated-milk
  (let [cmix (first success)
        wmix (second success)
        cj   (:jalapeno pepper)
        wj   (g 1)
       ]
  (ops/with-unit-arithmetic
    (/ (- (* cmix wmix) (* cj wj)) (- wmix wj)))))
))

;; how far off was our empirical estimation?
(ops// (ops/- estimated-milk milk) milk)
