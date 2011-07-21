(ns instant-coffee.test.core
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.core))

(def-fs-test source-files-test
  (is (= (source-files "coffee" "coffeescripts")
         ["foo.coffee" "bar.coffee" "baz/booje.coffee"])))
