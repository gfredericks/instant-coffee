(ns instant-coffee.test.core
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config
        instant-coffee.core))

(def-fs-test basic-coffee-compile-test
  (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
  (-main [])
  (doseq [filename ["fumble" "foo" "bar" "baz/booje"]]
    (is (.exists (file "public/javascripts" (str filename ".js"))))))

(def-watcher-test basic-coffee-watch-test
  (let [tgt (file "public/javascripts/fumble.js")]
    (is (not (.exists tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
    (Thread/sleep 3000)
    (is (.exists tgt))
    (is (re-find #"Wallaby" (slurp tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Horton'")
    (Thread/sleep 1500)
    (is (.exists tgt))
    (is (not (re-find #"Wallaby" (slurp tgt))))
    (is (re-find #"Horton" (slurp tgt)))))

(def-watcher-test deleting-test
  (Thread/sleep 2000)
  (let [tgt (file "public/javascripts/bar.js")]
    (is (.exists tgt))
    (is (.exists (file "coffeescripts/bar.coffee")))
    (.delete (file "coffeescripts/bar.coffee"))
    (Thread/sleep 1000)
    (is (not (.exists tgt)))))

(def-watcher-test compile-error-test
  (Thread/sleep 2000)
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
