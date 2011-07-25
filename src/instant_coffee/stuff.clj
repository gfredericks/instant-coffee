(ns instant-coffee.stuff
  (:require [instant-coffee.jcoffeescript :as jc]
            [instant-coffee.haml-js :as haml])
  (:import org.apache.commons.io.FileUtils)
  (:import org.jcoffeescript.JCoffeeScriptCompileException)
  (:require [clojure.string :as string]
            [clojure.set :as sets])
  (:use slingshot.core)
  (:use [instant-coffee.config :only [file]]
        instant-coffee.cache))

(defn- print-and-flush
  [s]
  (print s)
  (.flush *out*))

(defn- single-string-array
  [s]
  (let [a (make-array String 1)]
    (aset a 0 s)
    a))

(defn source-files
  [suffix dir]
  ; recursive seek
  (let [dir-file (file dir),
        prefix-length (inc (count (.getPath dir-file)))]
    (if (.exists dir-file)
      (for [file (seq (FileUtils/listFiles (file dir) (single-string-array suffix) true))]
        (let [path (.getPath file)]
          (.substring path prefix-length)))
      [])))

(defmulti subcompiler
  "Given a keyword (the key in the config file) and an object (the value
  in the config file), returns an iteration function."
  (comp first list))

(defn config-to-iteration
  "Given a configuration map, returns a function that can be called repeatedly
  for watching, or once for build-once."
  [config]
  (let [fns (for [{type :type, :as m} config]
              (subcompiler (keyword type) m))]
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
              (let [slurped-at (System/currentTimeMillis),
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
      (fn [filename src]
        (let [target-filename (string/replace filename #"\.coffee$" ".js"),
              target-file (file target-dir target-filename)]
          (try+
            (let [target-dir (.getParentFile target-file),
                  hashed-src (sha1 src)]
              (when-not (.exists target-dir)
                (.mkdirs target-dir))
              (print-and-flush (format "Compiling %s..." filename))
              (spit target-file (maybe-compile src hashed-src))
              (print-and-flush "\n"))
            (catch #(and (map? %) (contains? % :compile)) {msg :compile}
              (println "Error! " msg)
              (if (.exists target-file)
                (.delete target-file))))))
      (fn [filename]
        (let [target-file (file target-dir (string/replace filename #"\.coffee$" ".js"))]
          (when (.exists target-file)
            (println (format "Deleting %s..." filename))
            (.delete target-file)))))))

(defn- templates-to-js-object-literal
  "Takes a map from relative filenames to function-literal-strings,
  and produces an object literal, where nested directories are mapped
  to nested properties."
  [compilations]
  (let [nested-compilations
          (reduce
            (fn [m [filename js]]
              (assoc-in m (string/split filename #"/") js))
            {}
            compilations),
        to-literal
          (fn to-literal
            [m]
            (if (string? m)
              m
              (str
                "{"
                (string/join ","
                  (for [[k v] m]
                    (format "%s : %s"
                      (->> k (re-matches #"(.*?)(\.js\.haml)?") second pr-str)
                      (to-literal v))))
                "}")))]
    (to-literal nested-compilations)))

(defmethod subcompiler :haml
  [_ haml-config]
  (let [{src-dir :src, target-filename :target-file} haml-config,
        target-file (file target-filename),
        compilations (atom {})
        assignment-variable (or (:template-variable haml-config) "Templates"),
        write-to-file
          (fn []
            (spit target-file
              (format "%s = %s;"
                assignment-variable
                (templates-to-js-object-literal @compilations))))]
    (file-watcher
      src-dir
      (partial source-files "js.haml" src-dir)
      (fn [filename src]
        (print-and-flush (format "Compiling %s..." filename))
        (swap! compilations assoc filename (haml/compile-to-js src))
        (write-to-file)
        (print-and-flush "\n"))
      (fn [filename]
        (println (format "Deleting %s..." filename))
        (swap! compilations dissoc filename)
        (write-to-file)))))

(defmethod subcompiler :scss
  [_ scss-config]
  (constantly nil))

(defmethod subcompiler :default
  [k v]
  (println (format "Warning: no implementation for configuration %s" (name k)))
  (constantly nil))
