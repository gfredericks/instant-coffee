(ns instant-coffee.core
  (:require [instant-coffee.config :as config]
            [instant-coffee.stuff :as stuff])
  (:use [slingshot.core :only [try+]])
  (:gen-class))

;; MAIN API

(defn build-once
  "Just calls the iteration function."
  [iteration]
  (iteration))

; This atom and the associated code are used just for testing,
; where we run the watcher on a separate thread
(def watcher-status (atom nil))

(defn build-and-watch
  "Takes the iteration function and calls it once every quarter second."
  [iteration]
  (println "I'm watching you...")
  (try
    (loop []
      (when-not (= :quit @watcher-status)
        (iteration)
        (Thread/sleep 250)
        (recur)))
    (reset! watcher-status nil)
    (catch Throwable e
      (reset! watcher-status e))))

(defn -main
  [& args]
  (try+
    (let [config (:source_groups (config/read-config-file))]
      ((if (= (last args) "watch") build-and-watch build-once)
         (stuff/config-to-iteration config)))
    (catch
      #{:missing-config} _
      (println "Could not find a config.yml!"))))
