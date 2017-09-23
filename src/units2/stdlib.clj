(ns units2.stdlib
  (:require [units2.core :refer :all]
            [units2.IFnUnit :refer :all]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]))


;; # Commonly Used Units

;; ## Length [L]

(defbaseunit m ::length)
(defunit-with-SI-prefixes m m) ; redefining the base is not a problem

;; Imperial units...
(defunit inch (rescale cm 2.54))
(defunit foot (rescale inch 12))
(defunit yard (rescale foot 3))
(defunit mile (rescale yard 1760))

;; ## Time [T]

;; Seconds are not shortened to `s` as in the SI because
;; `ns` (nanosecond)  is already the Clojure namspace macro.
;; (The astrophysical unit of angle sec is `arcsec`)

(defbaseunit sec ::time)
(defunit-with-SI-prefixes sec sec)

(defunit minute (rescale sec 60))
(defunit hour   (rescale minute 60))
(defunit day    (rescale hour 24))
(defunit week   (rescale day 7))

;; ## Velocity [L/T]

(defunit mph (divide mile hour))
(make-dimensional-spec ::speed mph)
(spec/def ::velocity ::speed)

;; ## Acceleration [L/T^2]

(defunit standardgravity (rescale (divide m sec sec) 9.80665))
(make-dimensional-spec ::acceleration standardgravity)

;; ## Area [L^2]

(defunit barn (rescale (times m m) 1e-28))

;; ## Mass [M]

(defbaseunit g ::mass)
(defunit-with-SI-prefixes g g)

(defunit solarmass (rescale kg 2e30))

;; ## Charge [Q]

;; I know the SI is defined in terms of currents rather than charges,
;; but this is the way physicists do dimensional analysis.

(defbaseunit coulomb ::charge)

;; fundamental unit of charge in Quantum Electrodynamics.
(defunit positroncharge (AsUnit (coulomb 1.6022e-19)))

;; ## Temperature [K]

(defbaseunit K ::temperature)
(defunit-with-SI-prefixes K K)

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
(defbaseunit rad ::angle)

;; full circle
(defunit rot (rescale rad (solidangle 2))) ; aka revolution/turn
(defunit deg (rescale rot (/ 1 360))) ; 360 degrees in a circle

(defunit arcsec (rescale deg (/ 1 60))); 60 arcsec in a degree

;; ### Solid Angles

(defunit sr (power rad 2))
(make-dimensional-spec ::solidangle sr)

;; full 2-sphere
(defunit sky (rescale sr (solidangle 3))) ; aka spat
;; full 3-sphere (physically unnecessary but enlightening)
(defunit glome (rescale (power rad 3) (solidangle 4)))




;; ## Data Amount

(defbaseunit b ::data-amount)
(defunit-with-IEC-prefixes b b)
(defunit-with-IEC-prefixes o (rescale b 8)) ; octet, not byte.
; If your byte is 8 bits, you can just `(defunit-with-IEC-prefixes B o)` locally.

(defunit-with-IEC-prefixes bps (divide b sec))
(make-dimensional-spec ::data-rate bps)




; # Other Units


;; ## Typography

(defunit point (rescale inch (/ 1 72)))
(defunit pica (rescale inch (/ 1 6)))


;; ## Famous people names

(defunit-with-SI-prefixes hertz (inverse sec))
(make-dimensional-spec ::frequency hertz)
(defunit-with-SI-prefixes newton (divide (times m kg) sec sec))
(make-dimensional-spec ::force newton)
(defunit-with-SI-prefixes pascal (divide newton m m))
(make-dimensional-spec ::pressure pascal)
(defunit-with-SI-prefixes joule (times newton m))
(make-dimensional-spec ::energy joule)
(defunit-with-SI-prefixes watt (divide joule sec))
(make-dimensional-spec ::power watt)
(defunit-with-SI-prefixes ampere (divide coulomb sec))
(make-dimensional-spec ::electric-current ampere)

(defunit-with-SI-prefixes jansky (rescale (divide watt (times m m) sec) 1e-26))

(defunit batman (rescale kg 7.68)) ; a traditional unit of weight used in the Ottoman Empire

;; ## Index

; Etymologically, `first` is a contraction of `foremost` and `second` comes from the latin `sequor` (to follow)...
; so our beloved zero-based convention is consistent with etymology (if not with common usage)

(defbaseunit zerobased ::index) ; foremost element is "zero" (C/LISP convention)
(defunit onebased (offset zerobased (zerobased -1))) ; foremost element is "one" (Fortran/naive convention)

; TODO: (define nth [ls i] (clojure-nth ls (if (amount? i) (getValue i zerobased) i))) ; default to LISP convention


;; ## Redshifts

;; These can be given as z or as 1+z, both are useful in cosmology. Unitizing the `+1` is probably overkill, but not having to worry about which convention is used in function args is totally worth it.
(defbaseunit zee ::redshift)
(defunit onepluszee (offset zee (zee -1)))







;; ## No units

;; define this last, just in case
(defunit dimensionless (unit-from-powers {m 0}))
