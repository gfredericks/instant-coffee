(ns instant-coffee.jcoffeescript
  (:import org.jcoffeescript.JCoffeeScriptCompiler
           org.jcoffeescript.Option))

(def #^{:dynamic true} *function-wrapper* false)

;; WARNING: If we ever start compiling several coffeescripts
;;          in parallel, I doubt we can use the same compiler
;;          object for all of them
(let [get-compiler
        (memoize
          (fn [& args]
            (if (empty? args)
              (new JCoffeeScriptCompiler)
              (new JCoffeeScriptCompiler (first args)))))]
  (defn compile-coffee
    [s]
    (let [compiler
            (if *function-wrapper*
              (get-compiler)
              (get-compiler [Option/BARE]))]
      (.compile compiler s))))
