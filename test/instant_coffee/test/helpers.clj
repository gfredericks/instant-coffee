(ns instant-coffee.test.helpers
  (:import org.apache.commons.io.FileUtils)
  (:import java.io.File)
  (:use instant-coffee.config
        instant-coffee.core)
  (:use [clojure.contrib.java-utils :only [delete-file-recursively]])
  (:use clojure.test))

(defn fs-test
  [f]
  (let [tmp-dir-name (str "/tmp/instant-coffee-test-" (* (rand-int 99999999) (rand-int 99999999))),
        tmp-dir (new File tmp-dir-name)]
    (FileUtils/copyDirectory
      (new File "test-resources/sample-project")
      tmp-dir)
    (let [current-root @root-dir]
      (try
        (reset! root-dir tmp-dir-name)
        (f)
        (finally
          (reset! root-dir current-root)
          (FileUtils/deleteDirectory tmp-dir))))))

(defmacro def-fs-test
  [name & body]
  `(deftest ~name (fs-test (fn [] ~@body))))

(defn fs-watcher-test
  [f]
  (fs-test
    (fn []
      (.start (new Thread (fn [] (-main (read-config-file)))))
      (try
        (f)
        (finally (reset! halter true))))))

(defmacro def-watcher-test
  [name & body]
  `(deftest ~name (fs-watcher-test (fn [] ~@body))))
