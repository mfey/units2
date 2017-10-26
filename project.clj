(defproject units2 "2.7"
  :description "A Clojure library for units of measurement."
  :url "https://github.com/mfey/units2"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/spec.alpha "0.1.123"]
                 [clj-science/jscience "4.3.1"]
                 ;[javax.measure/unit-api "1.0"]
                 ;[tec.units/unit-ri "1.0.3"]
                 [org.clojure/test.check "0.9.0"]
               ; [org.apache.commons/commons-math3 "3.5"] ; optional: for integrals
               ; [incanter "1.5.6"] ; optional: for derivatives/integrals
                ]
  :plugins [[lein-marginalia "0.9.0"]
            [lein-cloverage "1.0.9"] ; bash <(curl -s https://codecov.io/bash) -f target/coverage/codecov.json -t token -C commitSHA
            [lein-kibit "0.1.5"]
            ]
  ;:repl-options {:init (do (require '[units2.core :refer :all])
  ;                         (require '[units2.stdlib :refer :all]))}
  ; :global-vars {*warn-on-reflection* true} ; I'm too forgetful for regular `lein check`
)
