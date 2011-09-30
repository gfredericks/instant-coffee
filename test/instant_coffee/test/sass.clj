(ns instant-coffee.test.sass
  (:use instant-coffee.sass)
  (:use clojure.test))

(deftest compile-sass-test
  (is (= (compile-sass "body\n  font-family: arial")
         "body {\n  font-family: arial; }\n")))
