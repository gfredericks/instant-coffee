(ns instant-coffee.core
  (:require [instant-coffee.config :as config]
            [instant-coffee.jcoffeescript :as jc])
  (:require [clojure.string :as string])
  (:import org.apache.commons.io.FileUtils)
  (:use [instant-coffee.config :only [file]])
  (:gen-class))

(defn- single-string-array
  [s]
  (let [a (make-array String 1)]
    (aset a 0 s)
    a))

(defn source-files
  [suffix dir]
  ; recursive seek
  (let [prefix-length (inc (count (.getPath (file dir))))]
    (for [file (seq (FileUtils/listFiles (file dir) (single-string-array suffix) true))]
      (let [path (.getPath file)]
        (.substring path prefix-length)))))


;; MAIN API

(defn build-once
  [config]
  (let [{{src-dir :src, target-dir :target} :coffeescript} config,
        coffees (source-files "coffee" src-dir)]
    (doseq [coffee coffees]
      (let [target-filename (string/replace coffee #"\.coffee$" ".js"),
            target-file (file target-dir target-filename),
            target-dir (.getParentFile target-file)]
        (when-not (.exists target-dir)
          (.mkdirs target-dir))
        (spit target-file
          (jc/compile-coffee (slurp (file src-dir coffee))))))))

(def halter (atom nil))

(defn build-and-watch
  [config]
  (loop []
    (if @halter
      (reset! halter nil)
      (do
        (build-once config)
        (Thread/sleep 250)
        (recur)))))

(defn -main
  [args]
  (let [config (config/read-config-file)]
    ((if (= (last args) "watch") build-and-watch build-once)
       config)))
