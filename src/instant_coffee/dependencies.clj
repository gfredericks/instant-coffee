(ns instant-coffee.dependencies
  "Dependency resolution for coffeescript sources"
  (:use [slingshot.core :only [throw+]])
  (:require [instant-coffee.annotations :as ann]
            [clojure.string :as string]))

(defn- ensure-coffee-suffix
  [filename]
  (if (re-matches #".*\.coffee" filename)
    filename
    (str filename ".coffee")))

(defn- requirements
  [src]
  (->> src
    (ann/read-annotation "////")
    :requires
    (#(or % []))
    (map ensure-coffee-suffix)))

(defn- topologically-sort
  "This is just an algorithm I more or less read off of wikipedia. You
  simply pull sink nodes off the DAG until it's empty. Easy peasy."
  [dependencies]
  (loop [sorted [], dependencies dependencies]
    (if (empty? dependencies)
      sorted
      (let [no-reqs
              (->> dependencies
                (keys)
                (filter #(empty? (dependencies %)))
                (set)),
            rem-keys (remove no-reqs (keys dependencies))]
        (if (empty? no-reqs)
          (throw+ :circular-dependency)
          (recur
            (apply conj sorted no-reqs)
            (zipmap
              rem-keys
              (for [k rem-keys] (remove no-reqs (dependencies k))))))))))

(defn join-with-dependency-resolutions
  "Input is a map from filenames to compiled javascript with requirements
  in the comment header."
  [compilations]
  (let [with-requirements (zipmap (keys compilations) (->> compilations vals (map requirements)))]
    (string/join "\n\n"
      (for [filename (topologically-sort with-requirements)]
        (str "/************\n"
             " * BEGIN FILE\n"
             " * " filename "\n"
             " ************/\n"
             (compilations filename))))))
