(ns instant-coffee.test.config
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config))

(def-fs-test config-file-read-test
  (let [m (read-config-file)]
    (is (:coffeescript m))
    (is (:haml m))))
