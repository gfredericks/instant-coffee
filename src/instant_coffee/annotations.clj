(ns instant-coffee.annotations
  "Functions for adding/reading metadata from comment-headers of compiled code"
  (:import [java.io File StringWriter])
  (:require [clojure.string :as string])
  (:use [clojure.data.json :only (json-str write-json read-json pprint-json)]))

(defn- capture-output
  [f]
  (let [sw (new StringWriter)]
    (binding [*out* sw] (f))
    (str sw)))

(defn- comment-header
  [comment-prefix data]
  (let [json (capture-output #(pprint-json data)),
        json-lines (string/split json #"\n")]
    (apply str
           (for [line json-lines] (str comment-prefix " " line "\n")))))

(defn add-annotation
  [comment-prefix code-string data]
  (str (comment-header comment-prefix data) code-string))

; TODO: Allow reading from a file (and don't slurp the whole thing)
(defn read-annotation
  [comment-prefix code]
  (let [code (if (instance? File code) (slurp code) code),
        prefix-length (count comment-prefix),
        lines (string/split code #"\n"),
        comment-header
          (take-while #(= comment-prefix (.substring % 0 prefix-length)) lines),
        json (string/join (for [line comment-header] (.substring line prefix-length)))]
    (read-json json)))
