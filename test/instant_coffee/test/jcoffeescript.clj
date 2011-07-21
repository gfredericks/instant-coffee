(ns instant-coffee.test.jcoffeescript
  (:use clojure.test)
  (:use instant-coffee.jcoffeescript))

(deftest simple-compile-coffee-test
  (is (= (compile-coffee "x = 12") "var x;\nx = 12;")))
