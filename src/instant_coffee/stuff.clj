(ns instant-coffee.stuff
  (:require (instant-coffee
              [jcoffeescript :as jc]
              [haml-js :as haml]
              [annotations :as ann]
              [sass :as sass]))
  (:import org.apache.commons.io.FileUtils)
  (:import org.jcoffeescript.JCoffeeScriptCompileException)
  (:require [clojure.string :as string]
            [clojure.set :as sets])
  (:use slingshot.core)
  (:use [instant-coffee [config :only [file]] cache annotations dependencies]))

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
      (for [file (seq (FileUtils/listFiles (file dir) (single-string-array suffix) true)),
            :when (not (.isHidden file))]
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

(defn- compile-coffeescript-with-metadata
  "Compiles coffeescript, passing any comment-header annotations from the
  source to the target code."
  [src]
  (let [data (ann/read-annotation "#" src),
        js (jc/compile-coffee src)]
    (if data
      (ann/add-annotation "////" js data)
      js)))

(defn- create-cached-compiler
  "input compiler function should take src as input and return either
  {:code <string>} or {:error <string>}"
  [compiler]
  (let [cache (create-fs-cache)]
    (fn [src src-hash]
      (let [v (cache-get cache src-hash)]
        (cond
          (:code v)
            (:code v)
          (:error v)
            (throw+ {:compile (:error v)})
          :else
            (let [v (compiler src)]
              (if-let [e (:error v)]
                (do
                  (cache-set-error cache src-hash e)
                  (throw+ {:compile e}))
                (do
                  (cache-set cache src-hash (:code v))
                  (:code v)))))))))

(def create-cached-coffeescript-compiler
  (partial create-cached-compiler
    (fn [src]
      (try
        {:code (compile-coffeescript-with-metadata src)}
        (catch JCoffeeScriptCompileException e
          {:error (.getMessage e)})))))

(def create-cached-haml-compiler
  (partial create-cached-compiler
    (fn [src]
      (try
        {:code (haml/compile-to-js src)}
        (catch Exception e
          {:error (.getMessage e)})))))

(def create-cached-sass-compiler
  (partial create-cached-compiler
    (fn [src]
      (try
        {:code (sass/compile-sass src)}
        (catch Exception e
          {:error (.getMessage e)})))))

(defn- file-watcher
  "Takes three functions -- a file finder (a nullary function that returns a
  list of filenames) and two file-handlers. The first handler is a two-arg
  function that takes a filename and the file's contents and does something with
  it. The second takes a filename, and is called when the file has been deleted.
  file-watcher returns a function that, when invoked, will fetch the files and
  call file-handler for any files which have changed since the last time
  file-handler was called.

  An initialization function can be passed as an optional final argument. This
  changes the behavior the first time the file-watcher is called -- instead
  of calling the change-handler for each file it finds, it will call the
  initalization function a single time, passing it a map from src-names to src.
  The motivation for this was to keep the dependency-resolution piece from
  detecting and reporting bad requirements at startup as the files are loaded
  in one by one."
  [src-dir file-finder change-handler delete-handler & [initialize]]
  (let [last-compiled* (atom {}),
        last-compiled (fn [filename] (-> last-compiled* deref (get filename) first)),
        last-compiled-value (fn [filename] (-> last-compiled* deref (get filename) second)),
        initialized (atom false)]
    (fn []
      (let [srcs (set (file-finder))
            deleted-srcs (sets/difference (-> last-compiled* deref keys set) srcs)]
        (if (and initialize (not @initialized))
          (let [src-map
                  (reduce
                    (fn [m src-filename]
                      (let [slurped-at (System/currentTimeMillis),
                            src (slurp (file src-dir src-filename)),
                            hashed-src (sha1 src)]
                        (swap! last-compiled* assoc src-filename [slurped-at hashed-src])
                        (assoc m src-filename src)))
                    {}
                    srcs)]
            (initialize src-map)
            (reset! initialized true))
          (do
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
              (delete-handler src-filename))))))))

(defn- one-to-one-compiler
  [src-dir
   extension
   target-dir
   target-extension
   compiler-fn]
  (let [filename-converter
          (fn [filename]
            (string/replace
              filename
              (re-pattern (str "\\." extension "$"))
              (str "." target-extension)))]
    (file-watcher
      src-dir
      (partial source-files extension src-dir)
      (fn [filename src]
        (let [target-file (file target-dir (filename-converter filename)),
              hashed-src (sha1 src)]
          (try+
            (let [target-dir (.getParentFile target-file)]
              (when-not (.exists target-dir)
                (.mkdirs target-dir))
              (print-and-flush (format "Compiling %s..." filename))
              (spit target-file
                    (compiler-fn src hashed-src))
              (print-and-flush "done!\n"))
            (catch #(and (map? %) (contains? % :compile)) {msg :compile}
              (println "Error! " msg)
              (FileUtils/deleteQuietly target-file)))))
      (fn [filename]
        (let [target-filename (filename-converter filename),
              target-file (file target-dir target-filename)]
          (when (.exists target-file)
            (println (format "Deleting %s..." target-filename))
            (FileUtils/deleteQuietly target-file)))))))

(defn- many-to-one-compiler
  "Creates a compiler for compiling a group of files to a single target file."
  [src-dir srcs-fn compile-fn write-fn]
  (let [compilations (atom {})]
    (file-watcher
      src-dir
      srcs-fn
      (fn [filename src]
        (print-and-flush (format "Compiling %s..." filename))
        (try+
          (let [compiled (compile-fn src)]
            (swap! compilations assoc filename compiled)
            (write-fn @compilations)
            (print-and-flush "done!\n"))
          (catch #(and (map? %) (contains? % :compile)) {msg :compile}
            (println "Error! " msg))))
      (fn [filename]
        (println (format "Deleting %s..." filename))
        (swap! compilations dissoc filename)
        (write-fn @compilations))
      (fn [srcs]
        (doseq [[filename src] srcs]
          (print-and-flush (format "Compiling %s..." filename))
          (try+
            (let [compiled (compile-fn src)]
              (swap! compilations assoc filename compiled)
              (print-and-flush "\n"))
            (catch #(and (map? %) (contains? % :compile)) {msg :compile}
              (println "Error! " msg))))
        (when-not (empty? @compilations)
          (write-fn @compilations)
          (println "Done!"))))))

(defn- has-src-hash?
  "Checks if the target file exists and has the given hash value in its
  :src-hash metadata."
  [file hash]
  (and
    (.exists file)
    (let [annotation (read-annotation "////" file)]
      (and annotation
           (= hash (:src-hash annotation))))))

(defmethod subcompiler :coffeescript
  [_ coffee-config]
  (let [{src-dir :src, target-dir :target} coffee-config,
        maybe-compile (create-cached-coffeescript-compiler),
        srcs-fn (partial source-files "coffee" src-dir)]
    (if target-dir
      (one-to-one-compiler
        src-dir
        "coffee"
        target-dir
        "js"
        maybe-compile)
      (let [target-file (file (:target-file coffee-config))]
        (many-to-one-compiler
          src-dir
          srcs-fn
          (fn [src]
            (maybe-compile src (sha1 src)))
          (fn [compilations]
            (try+
              (let [js (join-with-dependency-resolutions compilations)]
                (spit target-file js))
              (catch #{:circular-dependency} _
                (println "Circular dependency detected! Deleting target file...")
                (FileUtils/deleteQuietly target-file)))))))))

(defmethod subcompiler :haml
  [_ haml-config]
  (let [{src-dir :src, target-filename :target-file} haml-config,
        target-file (file target-filename),
        assignment-variable (or (:template-variable haml-config) "Templates")]
    (many-to-one-compiler
      src-dir
      (partial source-files "js.haml" src-dir)
      (let [cc (create-cached-haml-compiler)]
        (fn [src] (cc src (sha1 src))))
      (fn [compilations]
        (spit target-file
          (haml/combine-compilations compilations assignment-variable))))))

(defmethod subcompiler :sass
  [_ sass-config]
  (let [{src-dir :src, target-dir :target} sass-config]
    (one-to-one-compiler
      src-dir
      "sass"
      target-dir
      "css"
      (create-cached-sass-compiler))))

(defmethod subcompiler :default
  [k v]
  (println (format "Warning: no implementation for configuration %s" (name k)))
  (constantly nil))
