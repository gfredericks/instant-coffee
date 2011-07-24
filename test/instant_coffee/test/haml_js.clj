(ns instant-coffee.test.haml-js
  (:use clojure.test)
  (:use instant-coffee.haml-js))

(deftest it-works-test
  (is (compile-haml "%hr")))
