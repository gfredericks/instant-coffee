(ns instant-coffee.config
  (:import java.io.File)
  (:use [slingshot.core :only [throw+]])
  (:require [clj-yaml.core :as yaml]
            [clojure.string :as string]))

(def root-dir (atom "."))

(defn file
  [& path-elements]
  (new File @root-dir (string/join "/" path-elements)))

(def DEFAULT-CONFIG-FILE-LOCATIONS
  ["config.yml"
   "config/instant_coffee.yml"])

(defn read-config-file
  []
  (let [f (first (filter #(.exists %) (map file DEFAULT-CONFIG-FILE-LOCATIONS)))]
    (if f
      (yaml/parse-string (slurp f))
      (throw+ :missing-config))))

(def *global-config* nil)
