(ns instant-coffee.haml-js
  (:import org.mozilla.javascript.Context
           java.io.StringReader)
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

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

(defn compile-to-js
  [s]
  (format "function(_ob){with(_ob || {}){\nreturn %s\n}}" (compile-haml s)))

(def escape-html-definition
  (str
    "function(t){return (t+'')"
    ".replace(/&/g,'&amp;')"
    ".replace(/</g,'&lt;')"
    ".replace(/>/g,'&gt;')"
    ".replace(/\\\"/g,'&quot;');}"))

(defn combine-compilations
  [compilations assignment-variable-name]
  (let [nested-compilations
          (reduce
            (fn [m [filename js]]
              (assoc-in m (string/split filename #"/") js))
            {}
            compilations),
        to-literal
          (fn to-literal
            [m]
            (if (string? m)
              m
              (str
                "{"
                (string/join ","
                  (for [[k v] m]
                    (format "%s : %s"
                      (->> k (re-matches #"(.*?)(\.js\.haml)?") second pr-str)
                      (to-literal v))))
                "}")))]
    (format "var %s = (function(){var _escape_html=%s;return %s;})();"
      assignment-variable-name
      escape-html-definition
      (to-literal nested-compilations))))
