(ns units2.astro
  (:require [units2.core :refer :all]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit Unit SI NonSI)))

(set! *warn-on-reflection* true)

;; ## Length [L]


(defunit-with-SI-prefixes pc (->IFnUnit NonSI/PARSEC))
(defunit-with-SI-prefixes m (->IFnUnit SI/METER))

(defunit AU (->IFnUnit NonSI/ASTRONOMICAL_UNIT))
;; ## Time [T]

;; Seconds are not shortened to `s` as in the SI because `ns` is the Clojure namspace macro. The astrophsyical sec is `arcsec`.

(defunit-with-SI-prefixes sec (->IFnUnit SI/SECOND))
(defunit-with-SI-prefixes yr (->IFnUnit NonSI/YEAR_SIDEREAL))

;; ## Mass [M]


(defunit-with-SI-prefixes g (->IFnUnit SI/GRAM))

;; Solar Mass
(defunit Msol (AsUnit (kg 1.98855E30)))

;; ## Charge [Q]

(defunit coulomb (->IFnUnit SI/COULOMB))
;; fundamental unit of charge in Quantum Electrodynamics.
(defunit positroncharge (AsUnit (coulomb 1.6022e-19)))

;; ## Temperature [K]

(defunit-with-SI-prefixes K (->IFnUnit SI/KELVIN))
(defunit celsius (offset K (K 273.15)))
(defunit fahrenheit (rescale (offset celsius (celsius -17.777)) (/ 5 9))) ; for pedagogical purposes only.

;; ## (Solid) Angle [a]

(def solidangle "Solid angle of a unit sphere in D dimensions. The full formula involves fractional powers of Pi and Gamma functions, the first few are precomuted and tabulated here for convenience."
  {1 2
   2 (* 2 Math/PI)
   3 (* 4 Math/PI)
   4 (* 2 Math/PI Math/PI)
   5 (* (/ 8 3) Math/PI Math/PI)
   6 (* Math/PI Math/PI Math/PI)})

;; ### Angles
(defunit rad (makebaseunit "a")) ; a for angle

;; full circle
(defunit rot (rescale rad (solidangle 2))) ; aka revolution/turn
(defunit deg (rescale rot (/ 1 360))) ; 360 degrees in a circle

;; ### Solid Angles

(defunit sr (power rad 2))

;; full 2-sphere
(defunit sky (rescale sr (solidangle 3))) ; aka spat
;; full 3-sphere (physically unnecessary but enlightening)
(defunit glome (rescale (power rad 3) (solidangle 4)))

;; ## Velocity [L/T]

(defunit lightspeed (->IFnUnit NonSI/C))

;; ## Energy [E] = [M L^2 / T^2]

(defunit-with-SI-prefixes eV (->IFnUnit NonSI/ELECTRON_VOLT))
(defunit-with-SI-prefixes J (->IFnUnit SI/JOULE))
(defunit erg (->IFnUnit NonSI/ERG))

;; ## Luminosity (radiant flux)

;; Solar Luminosity
(defunit Lsol (AsUnit (->amount 3.846E26 (->IFnUnit SI/WATT))))

;; ## Flux [L^-2 T^-1]

(defunit Flux (unit-from-powers {cm -2 sec -1}))
(defunit Intensity (unit-from-powers {cm -2 sec -1 sr -1}))

;; ## Spectral Flux [L^-2 T^-1 E^-1]

(defunit SpectralFlux (unit-from-powers {cm -2 sec -1 GeV -1}))
(defunit SpectralIntensity (unit-from-powers {cm -2 sec -1 sr -1 GeV -1}))

;; ## Redshifts

;; These can be given as z or as 1+z, both are useful in cosmology. Unitizing the `+1` is probably overkill, but not having to worry about which convention is used in function args is totally worth it.
(defunit zee (makebaseunit "z"))
(defunit onepluszee (offset zee (zee -1)))

;; ## No units

;; define this last, just in case
(defunit dimensionless (unit-from-powers {m 0}))
