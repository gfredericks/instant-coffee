(ns instant-coffee.stuff
  (:require [instant-coffee.jcoffeescript :as jc])
  (:import org.apache.commons.io.FileUtils)
  (:import org.jcoffeescript.JCoffeeScriptCompileException)
  (:require [clojure.string :as string]
            [clojure.set :as sets])
  (:use slingshot.core)
  (:use [instant-coffee.config :only [file]]
        instant-coffee.cache))

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

(defmulti subcompiler
  "Given a keyword (the key in the config file) and an object (the value
  in the config file), returns an iteration function."
  (comp first list))

(defn config-to-iteration
  "Given a configuration map, returns a function that can be called repeatedly
  for watching, or once for build-once."
  [config]
  (let [fns (for [[k v] config] (subcompiler k v))]
    (fn []
      (doseq [f fns] (f)))))

(defn round-down-milliseconds
  [m]
  (- m (rem m 1000) 1))

(defn- create-cached-coffeescript-compiler
  []
  (let [cache (create-memcache)]
    (fn [src src-hash]
      (let [v (cache-get cache src-hash)]
        (cond
          (:code v)
            (:code v)
          (:error v)
            (throw+ {:compile (:error v)})
          :else
            (try
              (let [v (jc/compile-coffee src)]
                (cache-set cache src-hash v)
                v)
              (catch JCoffeeScriptCompileException e
                (cache-set-error cache src-hash (.getMessage e))
                (throw+ {:compile (.getMessage e)}))))))))

(defmethod subcompiler :coffeescript
  [_ coffee-config]
  (let [{src-dir :src, target-dir :target} coffee-config,
        last-compiled* (atom {}),
        last-compiled (fn [filename] (-> last-compiled* deref (get filename) first)),
        last-compiled-value (fn [filename] (-> last-compiled* deref (get filename) second)),
        maybe-compile (create-cached-coffeescript-compiler)]
    (fn []
      (let [srcs (set (source-files "coffee" src-dir))
            deleted-srcs (sets/difference (-> last-compiled* deref keys set) srcs)]
        (doseq [coffee srcs]
          (let [src-file (file src-dir coffee)]
            (when (or (nil? (last-compiled coffee))
                    (FileUtils/isFileNewer
                      src-file
                      (round-down-milliseconds (last-compiled coffee))))
              (let [target-filename (string/replace coffee #"\.coffee$" ".js"),
                    target-file (file target-dir target-filename),
                    slurped-at (now)]
                (try+
                  (let [target-dir (.getParentFile target-file),
                        src (slurp (file src-dir coffee)),
                        hashed-src (sha1 src)]
                    (when-not (= hashed-src (last-compiled-value coffee))
                      (when-not (.exists target-dir)
                        (.mkdirs target-dir))
                      (swap! last-compiled* assoc coffee [slurped-at hashed-src])
                      (print (format "Compiling %s..." coffee))
                      (.flush *out*)
                      (spit target-file (maybe-compile src hashed-src))
                      (print "\n")
                      (.flush *out*)))
                  (catch #(and (map? %) (contains? % :compile)) {msg :compile}
                    (println "Error! " msg)
                    (if (.exists target-file)
                      (.delete target-file))))))))
        (doseq [coffee deleted-srcs]
          (let [target-file (file target-dir (string/replace coffee #"\.coffee$" ".js"))]
            (when (.exists target-file)
              (println (format "Deleting %s..." coffee))
              (.delete target-file))
            (swap! last-compiled* dissoc coffee)))))))

(defmethod subcompiler :haml
  [_ haml-config]
  (constantly nil))

(defmethod subcompiler :scss
  [_ scss-config]
  (constantly nil))

(defmethod subcompiler :default
  [k v]
  (println (format "Warning: no implementation for configuration %s" (name k)))
  (constantly nil))
