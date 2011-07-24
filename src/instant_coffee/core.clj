(ns instant-coffee.core
  (:require [instant-coffee.config :as config]
            [instant-coffee.stuff :as stuff])
  (:use [slingshot.core :only [try+]])
  (:gen-class))

;; MAIN API

(defn build-once
  [iteration]
  (iteration))

(def halter (atom nil))

(defn build-and-watch
  [iteration]
  (try
    (loop []
      (when-not @halter
        (iteration)
        (Thread/sleep 250)
        (recur)))
    (finally
      (reset! halter nil))))

(defn -main
  [& args]
  (try+
    (let [config (config/read-config-file)]
      ((if (= (last args) "watch") build-and-watch build-once)
         (stuff/config-to-iteration config)))
    (catch
      #{:missing-config} _
      (println "Could not find a config.yml!"))))
