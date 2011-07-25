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

(defn- file-watcher
  "Takes three functions -- a file finder (a nullary function that returns a
  list of filenames) and two file-handlers. The first handler is a two-arg
  function that takes a filename and the file's contents and does something with
  it. The second takes a filename, and is called when the file has been deleted.
  file-watcher returns a function that, when invoked, will fetch the files and
  call file-handler for any files which have changed since the last time
  file-handler was called."
  [src-dir file-finder change-handler delete-handler]
  (let [last-compiled* (atom {}),
        last-compiled (fn [filename] (-> last-compiled* deref (get filename) first)),
        last-compiled-value (fn [filename] (-> last-compiled* deref (get filename) second))]
    (fn []
      (let [srcs (set (file-finder))
            deleted-srcs (sets/difference (-> last-compiled* deref keys set) srcs)]
        (doseq [src-filename srcs]
          (let [src-file (file src-dir src-filename)]
            (when (or (nil? (last-compiled src-filename))
                    (FileUtils/isFileNewer
                      src-file
                      (round-down-milliseconds (last-compiled src-filename))))
              (let [slurped-at (now),
                    src (slurp (file src-dir src-filename)),
                    hashed-src (sha1 src)]
                (when-not (= hashed-src (last-compiled-value src-filename))
                  (swap! last-compiled* assoc src-filename [slurped-at hashed-src])
                  (change-handler src-filename src))))))
        (doseq [src-filename deleted-srcs]
          (swap! last-compiled* dissoc src-filename)
          (delete-handler src-filename))))))

(defmethod subcompiler :coffeescript
  [_ coffee-config]
  (let [{src-dir :src, target-dir :target} coffee-config,
        maybe-compile (create-cached-coffeescript-compiler)]
    (file-watcher
      src-dir
      (partial source-files "coffee" src-dir)
      (fn [coffee src]
        (let [target-filename (string/replace coffee #"\.coffee$" ".js"),
              target-file (file target-dir target-filename)]
          (try+
            (let [target-dir (.getParentFile target-file),
                  hashed-src (sha1 src)]
              (when-not (.exists target-dir)
                (.mkdirs target-dir))
              (print (format "Compiling %s..." coffee))
              (.flush *out*)
              (spit target-file (maybe-compile src hashed-src))
              (print "\n")
              (.flush *out*))
            (catch #(and (map? %) (contains? % :compile)) {msg :compile}
              (println "Error! " msg)
              (if (.exists target-file)
                (.delete target-file))))))
      (fn [coffee]
        (let [target-file (file target-dir (string/replace coffee #"\.coffee$" ".js"))]
          (when (.exists target-file)
            (println (format "Deleting %s..." coffee))
            (.delete target-file)))))))

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
