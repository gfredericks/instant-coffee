(ns instant-coffee.core
  (:require [instant-coffee.config :as config])
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
  [config])

(defn build-and-watch
  [config])

(defn -main
  [args]
  (let [config (config/read-config-file)]
    ((if (= (last args) "watch") build-and-watch build-once)
       config)))
