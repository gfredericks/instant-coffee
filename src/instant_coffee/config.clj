(ns instant-coffee.config
  (:import java.io.File)
  (:require [clj-yaml.core :as yaml]
            [clojure.string :as string]))

(def root-dir (atom "."))

(defn file
  [& path-elements]
  (new File @root-dir (string/join "/" path-elements)))

(defn read-config-file
  []
  (yaml/parse-string (slurp (file "config.yml"))))
