(ns instant-coffee.config
  (:import java.io.File)
  (:use [slingshot.core :only [throw+]])
  (:require [clj-yaml.core :as yaml]
            [clojure.string :as string]))

(def root-dir (atom "."))

(defn file
  [& path-elements]
  (new File @root-dir (string/join "/" path-elements)))

(defn read-config-file
  []
  (let [f (first (filter #(.exists %) [(file "config.yml") (file "config/instant_coffee.yml")]))]
    (if f
      (yaml/parse-string (slurp f))
      (throw+ :missing-config))))
