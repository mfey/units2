;;
;; # Molecular Gastronomy in Clojure
;;

(ns units2.spice
  (:require [units2.core :refer :all]
            [units2.ops :as ops]
            [units2.IFnUnit :refer :all]
            [units2.stdlib :refer [mug g kg]]))

;; The spiciness of food is determined (amongst other things) by the amount of capsaisinoids they contain.
;; capsaisinoid concentrations can be determined to parts per million with liquid chromatography
(defunit ppm (divide mug g))

;; The Scoville is a historical unit for spice of roughly 15 or 16 ppm pure capsaisin
(def history (/ 15))
(defunit-with-SI-prefixes scoville (rescale ppm history))

;; consistency check: pure capsaicin is 15,000,000 Scoville
(def pure-capsaicin (ppm 1e6))
(Mscoville pure-capsaicin)

;; ## time to cook!

;; all of these values from Wikipedia
(def pepper {
  :pepperoncino   (scoville 500)
  :jalapeno       (kscoville 10)
  :habanero       (Mscoville 0.2)
})

;; all of these values reasonable, but unchecked
(def meat {
  :chicken        (scoville 0)
  :lamb           (scoville 0)
  :oldspiceman    (Mscoville 9.001) ; from the commercials
})

;; very lightly spiced because the same spoon was used for the curry.
(def rice (scoville 10))



;; let's see how much spicier habaneros are than jalapenos
(ops/divide-into-double (:habanero pepper) (:jalapeno pepper))

;; it's easy to check that the Old Spice Man's pungency is over 9000 (in the appropriate units):
(ops/> (:oldspiceman meat) (kscoville 9000))

;; ## now, let's *actually* cook!

;; concentrations (c) are intensive quantities... we need to say how much weight (w) of each ingredient we're adding...
(defn add-ingredients [[c1 w1] [c2 w2]]
  (ops/with-unit-arithmetic ; besides this macro, it's regular Clojure code.
    (let [w (+ w1 w2)]
      [(/ (+ (* c1 w1) (* c2 w2)) w) w])))

;; which is spiciest?
(let [recipe1 (add-ingredients [(:jalapeno pepper) (g 5)] [(:chicken meat) (kg 0.5)])
      recipe2 (add-ingredients [(:lamb meat) (g 50)] [rice (kg 1)])
     ]
  (map #(getValue % scoville) [(first recipe1) (first recipe2)])
)

;; a bit of everything... imagine managing units by hand!!!
(reduce add-ingredients [
  [(:pepperoncino pepper) (g 150)]
  [(:habanero pepper) (g 10)]
  [rice (g 600)]
  [(:chicken meat) (g 240)]
  [(:lamb meat) (kg 0.3)]
])

;; ## Too hot!!!

;; we need to counteract all the heat with dairy products.
;; Let's model this with negative quantities in scoville units...

;; some unknown value (not representative of reality)
(def milk (scoville (- (rand))))

;; there's no sensible interpretation as negative capsaisinoid concentration, so we'll have to do some work to
;; determine these amounts empirically: keep mouthwashing with milk until it's not spicy.

(def process (iterate (fn [x] (add-ingredients [milk (g 100)] x)) [(:jalapeno pepper) (g 4)]))
(def success (first (filter #(neg? (getValue (first %) scoville)) process)))

;; we can use this and the concentration-weight relation to estimate the soothingness of milk:

(def estimated-milk
  (let [cmix (first success)
        wmix (second success)
        cj   (:jalapeno pepper)
        wj   (g 4)
       ]
  (ops/with-unit-arithmetic
    (/ (- (* cmix wmix) (* cj wj)) (- wmix wj)))))

;; how far from the true value is our empirical estimation?
(Math/abs (ops/divide-into-double (ops/- estimated-milk milk) milk))
