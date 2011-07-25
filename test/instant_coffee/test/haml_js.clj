(ns instant-coffee.test.haml-js
  (:use clojure.test)
  (:use instant-coffee.haml-js))

(deftest it-works-test
  (is (compile-haml "%hr")))

(deftest attribute-expressions-test
  (let [js (compile-haml "%span(foo=bar)")]
    (is (= js "\"<span foo=\\\"\" + bar + \"\\\">\" + \n\"</span>\""))))
