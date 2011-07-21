(ns instant-coffee.jcoffeescript
  (:import org.jcoffeescript.JCoffeeScriptCompiler
           org.jcoffeescript.Option))

(def #^{:dynamic true} *function-wrapper* false)

(defn compile-coffee
  [s]
  (let [compiler
          (if *function-wrapper*
            (new JCoffeeScriptCompiler)
            (new JCoffeeScriptCompiler [Option/BARE]))]
    (.compile compiler s)))
