(ns instant-coffee.test.stuff
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config
        instant-coffee.stuff))

(def-fs-test source-files-test
  (is (= (sort (source-files "coffee" "coffeescripts"))
         (sort ["foo.coffee" "bar.coffee" "baz/booje.coffee"]))))
