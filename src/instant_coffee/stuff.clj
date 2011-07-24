(ns instant-coffee.stuff
  (:require [instant-coffee.jcoffeescript :as jc])
  (:import org.apache.commons.io.FileUtils)
  (:require [clojure.string :as string]
            [clojure.set :as sets])
  (:use [instant-coffee.config :only [file]]))

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

(defn- now
  []
  (System/currentTimeMillis))

(defn config-to-iteration
  "Given a configuration map, returns a function that can be called repeatedly
  for watching, or once for build-once.

  For the moment only deals with coffeescript."
  [config]
  (let [{{src-dir :src, target-dir :target} :coffeescript} config,
        last-compiled (atom {})]
    (fn []
      (let [srcs (set (source-files "coffee" src-dir))
            deleted-srcs (sets/difference (-> last-compiled deref keys set) srcs)]
        (doseq [coffee srcs]
          (let [src-file (file src-dir coffee)]
            (when (or (nil? (@last-compiled coffee))
                      (FileUtils/isFileNewer src-file (@last-compiled coffee)))
              (let [target-filename (string/replace coffee #"\.coffee$" ".js"),
                    target-file (file target-dir target-filename),
                    target-dir (.getParentFile target-file),
                    src (slurp (file src-dir coffee)),
                    compiled (jc/compile-coffee src)]
                (when-not (.exists target-dir)
                  (.mkdirs target-dir))
                (spit target-file compiled))
              (swap! last-compiled assoc coffee (now)))))
        (doseq [coffee deleted-srcs]
          (let [target-file (file target-dir (string/replace coffee #"\.coffee$" ".js"))]
            (when (.exists target-file)
              (.delete target-file))
            (swap! last-compiled dissoc coffee)))))))
