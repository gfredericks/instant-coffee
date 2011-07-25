(ns instant-coffee.test.config
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config))

(def-fs-test config-file-read-test
  (let [{configs :source_groups} (read-config-file)]
    (is (some #(= "coffeescript" (:type %)) configs))
    (is (some #(= "haml" (:type %)) configs))))
