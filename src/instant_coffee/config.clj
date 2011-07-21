(ns instant-coffee.config
  (:import java.io.File)
  (:require [clj-yaml.core :as yaml]))

(def #^{:dynamic true} *root-dir* ".")

(defn file
  [name]
  (new File *root-dir* name))

(defn read-config-file
  []
  (yaml/parse-string (slurp (file "config.yml"))))
