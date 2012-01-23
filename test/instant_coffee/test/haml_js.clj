(ns instant-coffee.test.haml-js
  (:use clojure.test)
  (:use instant-coffee.haml-js))

(deftest it-works-test
  (is (compile-haml "%hr")))

(deftest attribute-expressions-test
  (let [js (compile-haml "%span(foo=bar)")]
    (is (re-find #"\"<span foo=\\\"\" \+ _escape_html\(bar\) \+ \"\\\">\" \+ \n\"</span>\"" js))))

(deftest whitespace-test
  (let [js (compile-haml "%div\n  I have\n  a pet")]
    (is (not (re-find #"havea" js)))))