;; Calculus on functions using the (generically defined) `units2.core.amount` type.

;; This implementation assumes:
;;
;;   1. that the values in `amount` are sensible numbers.
;;   2. that the units in `amount` have an implementation of `getConverter` (but not necessarily a full implementation of the `Unitlike` protocol) in order to use `getValue`.
;;   3. nothing else (not even arithmetic from `units2.ops`)!
;;
;; As such, it should be trivial to reuse this code for other implementations.


(ns units2.calc
  (:require [units2.core :refer :all]
           ;; [incanter.optimize :only [derivative integrate]]
            ))

;; ## Abstract Algebra of Units in Calculus

;; i.e. Batteries-not-included logic for integrating and differentiating functions (in 1D) with units

;; Notice that we're really just defining the algebra on the units here;
;; the numerical heavy-lifting of actually performing derivatives and
;; integrals is left to external libraries and/or user-defined schemes.

(defn decorate-differentiator [numerical-differentiation-scheme] ; the scheme is a function (scheme f x args) -> (df/dx x) that doesn't know about units.
  (fn [f x args] ; f is the function to be differentiated at a position x (with possible extra args to the scheme)
    (let [scheme numerical-differentiation-scheme
          in  (amount? x)
          out (amount? (f x))
          ux  (if in  (getUnit x))
          uf  (if out (getUnit (f x)))]
      (cond ; choose how to differentiate based on units of input/output of f
        (and (not in) (not out))
           (scheme f x args)
        (and in out)
           (->amount (scheme (comp (from uf) f ux) (getValue x ux) args)
                     (divide uf ux))
        (and (not in) out)
           (->amount (scheme (comp (from uf) f) x args)
                     uf)
        (and in (not out))
           (->amount (scheme (comp f ux) (getValue x ux) args)
                     (inverse ux))
      ))))

(defn decorate-integrator [numerical-integration-scheme] ; the scheme is a function (scheme f [xmin xmax] args) -> (- (F xmax) (F xmin)) that doesn't know about units.
  (fn [f [xmin xmax] args] ; f is the function to be integrated from xmin to max (with possible extra args to the scheme)
    (let [scheme numerical-integration-scheme
          in  (amount? xmin)           ; check whether inputs/outputs
          out (amount? (f xmin))       ; of the integrand function have
          ux  (if in  (getUnit xmin))  ; any units; if so, extract them
          uf  (if out (getUnit (f xmin)))] ;
      (cond ; choose how to integrate based on units of input/output of f
        (and (not in) (not out))
           (scheme f [xmin xmax] args)
        (and in out)
           (->amount (scheme (comp (from uf) f ux) [(getValue xmin ux) (getValue xmax ux)] args)
                     (times uf ux))
        (and in (not out))
           (->amount (scheme (comp f ux) [(getValue xmin ux) (getValue xmax ux)] args)
                     ux)
        (and (not in) out)
           (->amount (scheme (comp (from uf) f) [xmin xmax] args)
                     uf)
       ))))

;; ## Concrete Differentiation and Integration routines

;; (naive)

(def ^:private naive-differentiate
  (decorate-differentiator
    (fn [f x [dx]]
        (/ (- (f (+ x dx)) (f x)) dx))))

(def ^:private naive-integrate
  (decorate-integrator
    (fn [f [xmin xmax] [Neval]]
      (let [step (/ (- xmax xmin) Neval)]
        (* (apply + (map f (range xmin xmax step))) step)))))


;; (using Incanter, which internally is basically the same as the above)


(comment

(def incanter-differentiate
  (decorate-differentiator
    (fn [f x args]
      ((incanter.optimize/derivative (fn [x] (apply f x args))) x))))

(def incanter-integrate
  (decorate-integrator
    (fn [f [xmin xmax] args]
      (incanter.optimize/integrate (fn [x] (apply f x args)) xmin xmax))))

)

;; (using Apache Commons)

(comment

(def apache-integrate
  (decorate-integrator
    (fn [f [xmin xmax] [integrator-object maxeval more-args]]
      (.integrate integrator-object maxeval
                  (proxy
                    [org.apache.commons.math3.analysis.UnivariateFunction]
                    []
                    (value [x] (apply f x more-args)))
                  xmin xmax))))

)

;; e.g.
;; (apache-integrate #(* % 2) [1 2]
;;                  [(org.apache.commons.math3.analysis.integration.RombergIntegrator.)
;;                   1e4
;;                   []])



(defn derivative [f]
  (fn [x] (naive-differentiate f x 1e-4)))

(def differentiate naive-differentiate)
(def integrate naive-integrate)
