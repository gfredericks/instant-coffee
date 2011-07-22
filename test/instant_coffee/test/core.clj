(ns instant-coffee.test.core
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config
        instant-coffee.core))

(def-fs-test source-files-test
  (is (= (sort (source-files "coffee" "coffeescripts"))
         (sort ["foo.coffee" "bar.coffee" "baz/booje.coffee"]))))

(def-fs-test basic-coffee-compile-test
  (-main [])
  (doseq [filename ["foo" "bar" "baz/booje"]]
    (is (.exists (file "public/javascripts" (str filename ".js"))))))
