(ns instant-coffee.test.core
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config
        instant-coffee.core))

(def-fs-test source-files-test
  (is (= (sort (source-files "coffee" "coffeescripts"))
         (sort ["foo.coffee" "bar.coffee" "baz/booje.coffee"]))))

(def-fs-test basic-coffee-compile-test
  (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
  (-main [])
  (doseq [filename ["fumble" "foo" "bar" "baz/booje"]]
    (is (.exists (file "public/javascripts" (str filename ".js"))))))

(def-watcher-test basic-coffee-watch-test
  (let [tgt (file "public/javascripts/fumble.js")]
    (is (not (.exists tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
    (Thread/sleep 500)
    (is (.exists tgt))
    (is (re-find #"Wallaby" (slurp tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Horton'")
    (Thread/sleep 500)
    (is (.exists tgt))
    (is (not (re-find #"Wallaby" (slurp tgt))))
    (is (re-find #"Horton" (slurp tgt)))))
