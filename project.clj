(defproject units2 "2.6"
  :description "A Clojure library for units of measurement."
  :url "https://github.com/mfey/units2"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [clj-science/jscience "4.3.1"]   ; for javax.measure, the javaworld implementation of units
                 [org.clojure/test.check "0.9.0"]
               ; [org.apache.commons/commons-math3 "3.5"] ; optional: for integrals
               ; [incanter "1.5.6"] ; optional: for derivatives/integrals
                ]
  :plugins [[lein-marginalia "0.9.0"]
          ; [lein-cloverage "1.0.9"]
            ]
  ; :global-vars {*warn-on-reflection* true} ; I'm too forgetful for regular `lein check`
)
