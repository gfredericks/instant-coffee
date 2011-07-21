(ns instant-coffee.test.helpers
  (:import org.apache.commons.io.FileUtils)
  (:import java.io.File)
  (:use instant-coffee.config)
  (:use [clojure.contrib.java-utils :only [delete-file-recursively]])
  (:use clojure.test))

(defn fs-test
  [f]
  (let [tmp-dir-name (str "/tmp/instant-coffee-test-" (* (rand-int 99999999) (rand-int 99999999))),
        tmp-dir (new File tmp-dir-name)]
    (FileUtils/copyDirectory
      (new File "test-resources/sample-project")
      tmp-dir)
    (try
      (binding [*root-dir* tmp-dir-name]
        (f))
      (finally
        (FileUtils/deleteDirectory tmp-dir)))))

(defmacro def-fs-test
  [name & body]
  `(deftest ~name (fs-test (fn [] ~@body))))
