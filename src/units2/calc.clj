;; Calculus on functions using the (generically defined) `units2.core.amount` type.

;; This implementation assumes:
;;
;;   1. that the values in `amount` are sensible numbers.
;;   2. that the units in `amount` have an implementation of `getConverter` (but not necessarily a full implementation of the `Unitlike` protocol) in order to use `getValue`.
;;   3. nothing else.
;;
;; As such, it should be fairly easy to reuse most of this code for other implementations.


(ns units2.calc
  (:require [units2.core :refer :all]
            [units2.ops :as ops]
            [incanter.optimize :as optim])
  (:import (org.apache.commons.math3.analysis UnivariateFunction)
           (org.apache.commons.math3.analysis.integration RombergIntegrator))
)

(set! *warn-on-reflection* true)

;; ## Differentiation

(defn- low-level-derivative
  "Differentiate a function along it's first argument, which must be numeric (other arguments, if any exist, may be supplied with :args)"
  [f & {:keys [args dx] :or {args [] dx 1e-6}}]
  (optim/derivative (fn [x] (apply f x args)) :dx dx)
  )


(defn differentiate
  "This differentiation deals with units properly. TODO: pass extra arguments..."
  [f x & {:keys [args dx] :or {args [] dx 1e-6}}]
  (let [in  (amount? x)
        out (amount? (f x))
        ux  (if in  (getUnit x))
        uf  (if out (getUnit (f x)))]
    (cond ; choose how to differentiate based on units of input/output of f
      (and (not in) (not out))
         ((low-level-derivative f) x)
      (and in out)
         (->amount ((low-level-derivative (fn [x] (getValue  (f (->amount x ux)) uf)))
                  (getValue x ux)) (divide uf ux))
      (and (not in) out)
         (->amount ((low-level-derivative (fn [x] (getValue (f x) uf))) x) uf)
      (and in (not out))
         (->amount ((low-level-derivative (fn [x] (f (->amount x ux)))) (getValue x ux)) (inverse ux))
    )
  ))

(defn derivative [f]
  (fn [x] (differentiate f x)))

;; ## Integration

(defn- low-level-integrate
  "Integrate a function along its first argument, which must be numeric (other arguments, if any exist, may be supplied with :args)"
  [f [mini maxi] args maxeval integrator]
  (case integrator
    :mapreduce
      (let [step (/ (- maxi mini) maxeval)]
        (* (apply + (map f (range mini maxi step))) step))
    :incanter
      (optim/integrate (fn [x] (apply f x args)) mini maxi)
    :Romberg
     (.integrate (RombergIntegrator.) maxeval (proxy [UnivariateFunction] [] (value [x] (apply f x args))) mini maxi)
  )
)

(low-level-integrate (fn [x] (* 2 x)) [0 10] [] 30 :Romberg)

(defn- integrate-with-units
  "This integration function deals with units properly."
  [f [mini maxi] & {:keys [args maxeval integrator] :or {args [] maxeval 1e6 integrator :Romberg}}]

  (let [in  (amount? mini)           ; check whether inputs/outputs
        out (amount? (f mini))       ; of the integrand function have
        ux  (if in  (getUnit mini))  ; any units; if so, extract them
        uf  (if out (getUnit (f mini)))] ;
    (cond ; choose how to integrate based on units of input/output of f
      (and (not in) (not out))
         (low-level-integrate f [mini maxi] args maxeval integrator)
      (and in out)
         (ops/* (low-level-integrate (fn [^double x] (getValue (f (->amount x ux)) uf)) [(getValue mini ux) (getValue maxi ux)] args maxeval integrator)
                (->amount 1 uf)
                (->amount 1 ux))
      (and in (not out))
         (->amount (low-level-integrate (fn [^double x] (f (->amount x ux))) [(getValue mini ux) (getValue maxi ux)] args maxeval integrator)
                 ux)
      (and (not in) out)
         (->amount (low-level-integrate (fn [^double x] (getValue (f x) uf)) [mini maxi] args maxeval integrator)
                 uf))))

;; the number of options available is getting out of hand... pass a hash of options instead?

(defn integrate
  "This integration function deals with logarithmic ranges and units properly at the same time..."
  [f [mini maxi] & {:keys [args maxeval integrator log] :or {args [] maxeval 1e6 integrator :Romberg log false}}]
  (if (not log)
      (integrate-with-units f [mini maxi] :args args :maxeval maxeval :integrator integrator)
    ;; TODO: throw exception if negative or zero integration bounds...
      (let [in  (amount? mini)           ; check whether inputs/outputs
            out (amount? (f mini))       ; of the integrand function have
            ux  (if in  (getUnit mini))  ; any units; if so, extract them
            uf  (if out (getUnit (f mini)))
            ; force the integration to happen without units (we'll recover them later)
            lmini (Math/log (if in (getValue mini ux) mini))
            lmaxi (Math/log (if in (getValue maxi ux) maxi))
            lf  (if in
                  (fn [y] (ops/* (Math/exp y) (f (->amount (Math/exp y) ux))))
                  (fn [y] (ops/* (Math/exp y) (f (Math/exp y))))
                )
            i (integrate-with-units lf [lmini lmaxi] :args args :maxeval maxeval :integrator integrator)
            ]
        ; now recover the units
        (cond
          (and (not in) (not out))
             i
          (and in out)
             (ops/* (->amount 1 ux) i)
          (and in (not out))
             (->amount i ux)
          (and (not in) out)
             i
             ))))

;; (defmacro multi-integrate
;;   "can probably do this with a recursive function, but I found a macro that does what I want. Good enough for now..."
;;   [f bounds]
;;   (let [gs (into [] (take (count bounds) (repeatedly gensym)))
;;         xs (map list gs bounds)]
;;    ; xs
;;   (reduce #(list 'integrate (list 'fn [(first %2)] %1) (second %2)) `(apply ~f ~gs) xs)
;; ))

;; (macroexpand '(multi-integrate (fn [x y z] (+ x y z)) [[0 10] [-1 1] [0 1]]))
