(ns instant-coffee.test.helpers
  (:import org.apache.commons.io.FileUtils)
  (:import java.io.File
           java.io.StringWriter)
  (:use instant-coffee.config
        instant-coffee.core)
  (:use [clojure.contrib.java-utils :only [delete-file-recursively]])
  (:use clojure.test))

(defn fs-test
  [f]
  (let [tmp-dir-name (str "/tmp/instant-coffee-test-" (* (rand-int 99999999) (rand-int 99999999))),
        tmp-dir (new File tmp-dir-name)]
    (FileUtils/copyDirectory
      (new File "test-resources/test-project")
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

(defn- finish-watching
  []
  (swap! watcher-status
    (fn [v]
      (if (nil? v)
        :quit
        v)))
  (loop []
    (when (= :quit @watcher-status)
      (Thread/sleep 50)
      (recur)))
  (let [v @watcher-status]
    (reset! watcher-status nil)
    (if (instance? Throwable v)
      (throw v))))

(def *watcher-output* nil)

(defn check-and-clear-output
  [re]
  (let [b (.getBuffer *watcher-output*),
        s (str b)]
    (is (re-find re s))
    (.delete b 0 (count s))))

(defn fs-watcher-test
  [f]
  (fs-test
    (fn []
      (let [sw (new StringWriter)]
        (.start (new Thread (fn [] (binding [*out* sw] (-main "watch")))))
        (try
          (binding [*watcher-output* sw] (f))
          (finally
            (finish-watching)))))))

(defmacro def-watcher-test
  [name & body]
  `(deftest ~name (fs-watcher-test (fn [] ~@body))))
