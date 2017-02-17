(defproject units2 "2.5"
  :description "A Clojure library for quantities with units."
  :url "https://github.com/mfey/units2"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [clj-science/jscience "4.3.1"]
                 [org.apache.commons/commons-math3 "3.5"] ; optional: for integrals
                 [org.clojure/test.check "0.9.0"]
                 [incanter "1.5.6"]] ; optional: for derivatives/integrals
  :plugins [[lein-marginalia "0.9.0"]
            ]
)
