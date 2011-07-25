(ns instant-coffee.haml-js
  (:import org.mozilla.javascript.Context
           java.io.StringReader)
  (:require [clojure.java.io :as io]))

(def haml-js-js (slurp (io/resource "javascripts/haml.js")))

(defn- create-compiler
  []
  (let [context (Context/enter)]
    (.setOptimizationLevel context -1)
    (let [global-scope (.initStandardObjects context)]
      (try
        (.evaluateString context global-scope haml-js-js "haml.js" 0 nil)
        (finally (Context/exit)))
      {:global-scope global-scope})))

(defn compile-haml
  ([s] (compile-haml (create-compiler) s))
  ([{global-scope :global-scope} s]
    (let [context (Context/enter),
          compile-scope (.newObject context global-scope)]
      (.setParentScope compile-scope global-scope)
      (.put compile-scope "hamlSource" compile-scope s)
      (let [ret (.evaluateString
                  context
                  compile-scope
                  "Haml.optimize(Haml.compile(hamlSource));"
                  "HamlCompiler"
                  0
                  nil)]
        (Context/exit)
        ret))))
