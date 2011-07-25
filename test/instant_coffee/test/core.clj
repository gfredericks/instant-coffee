(ns instant-coffee.test.core
  (:require [instant-coffee.jcoffeescript :as jc])
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config
        instant-coffee.core))

; warm up the compiler
(jc/compile-coffee "x=5")

(def-fs-test basic-coffee-compile-test
  (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
  (-main)
  (doseq [filename ["fumble" "foo" "bar" "baz/booje"]]
    (is (.exists (file "public/javascripts" (str filename ".js"))))))

(def-watcher-test basic-coffee-watch-test
  (Thread/sleep 700)
  (let [tgt (file "public/javascripts/fumble.js")]
    (is (not (.exists tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
    (Thread/sleep 700)
    (is (.exists tgt))
    (is (re-find #"Wallaby" (slurp tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Horton'")
    (Thread/sleep 800)
    (is (.exists tgt))
    (is (not (re-find #"Wallaby" (slurp tgt))))
    (is (re-find #"Horton" (slurp tgt)))))

(def-watcher-test deleting-test
  (Thread/sleep 700)
  (let [tgt (file "public/javascripts/bar.js")]
    (is (.exists tgt))
    (is (.exists (file "coffeescripts/bar.coffee")))
    (.delete (file "coffeescripts/bar.coffee"))
    (Thread/sleep 800)
    (is (not (.exists tgt)))))

(def-watcher-test compile-error-test
  (Thread/sleep 700)
  (let [src (file "coffeescripts/bar.coffee"),
        tgt (file "public/javascripts/bar.js")]
    (is (.exists tgt))
    (spit src "hu === ? * 98329873#&*38799872")
    (Thread/sleep 800)
    (check-and-clear-output #"Error!")
    (is (not (.exists tgt)))
    (spit tgt "x = 'foo'")
    (Thread/sleep 800)
    (is (.exists tgt))))

(def-fs-test basic-haml-compile-test
  (spit (file "templates/foo.js.haml") "%div Fantabulous")
  (-main)
  (is (.exists (file "public/templates.js")))
  (is (re-find #"Fantabulous" (slurp (file "public/templates.js")))))
