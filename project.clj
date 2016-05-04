(defproject units2 "2.5"
  :description "A Clojure library for quantities with units."
  :url "https://github.com/mfey/units2"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-science/jscience "4.3.1"]
                 [org.apache.commons/commons-math3 "3.5"]
               ;  [org.clojure/core.typed "0.3.23"]
                 [incanter "1.5.6"]]
  :plugins [[lein-marginalia "0.9.0"]
            ;[lein-typed "0.3.5"]
            ]
 ; :core.typed {:check [;units2.core     ; Something fishy
 ;                      ;units2.IFnUnit  ; when calling
 ;                      ;units2.ops      ; `typed coverage`
 ;                      ;units2.calc     ; with two ns's
 ;                      ;units2.astro    ; that see protocols
 ;                      ;units2.prob     ; in core... probably
 ;                      ]}               ; silly java stuff
)
