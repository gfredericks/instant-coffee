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
  (let [f (file "config.yml")]
    (if (.exists f)
      (yaml/parse-string (slurp (file "config.yml")))
      (throw+ :missing-config))))
