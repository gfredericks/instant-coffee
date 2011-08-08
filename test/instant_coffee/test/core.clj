(ns instant-coffee.test.core
  (:import java.util.regex.Pattern
           java.io.StringWriter)
  (:require [instant-coffee.jcoffeescript :as jc]
            [clojure.string :as string])
  (:use clojure.test
        instant-coffee.test.helpers
        instant-coffee.config
        instant-coffee.core))

(defn run-main
  []
  (let [sw (new StringWriter)]
    (binding [*out* sw] (-main))
    (str sw)))

; warm up the compiler
(jc/compile-coffee "x=5")

(def-fs-test basic-coffee-compile-test
  (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
  (run-main)
  (doseq [filename ["fumble" "foo" "bar" "baz/booje"]]
    (is (.exists (file "public/javascripts" (str filename ".js"))))))

(def-watcher-test basic-coffee-watch-test
  (Thread/sleep 700)
  (let [tgt (file "public/javascripts/fumble.js")]
    (is (not (.exists tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Wallaby'")
    (Thread/sleep 800)
    (is (.exists tgt))
    (is (re-find #"Wallaby" (slurp tgt)))
    (spit (file "coffeescripts/fumble.coffee") "x = 'Horton'")
    (Thread/sleep 800)
    (is (.exists tgt))
    (is (not (re-find #"Wallaby" (slurp tgt))))
    (is (re-find #"Horton" (slurp tgt)))))

(def-watcher-test deleting-test
  (Thread/sleep 700)
  (let [tgt (file "public/javascripts/bar.js")]
    (is (.exists tgt))
    (is (.exists (file "coffeescripts/bar.coffee")))
    (.delete (file "coffeescripts/bar.coffee"))
    (Thread/sleep 800)
    (check-and-clear-output #"Deleting bar.js")
    (is (not (.exists tgt)))))

(def-watcher-test compile-error-test
  (Thread/sleep 700)
  (let [src (file "coffeescripts/bar.coffee"),
        tgt (file "public/javascripts/bar.js")]
    (is (.exists tgt))
    (spit src "hu === ? * 98329873#&*38799872")
    (Thread/sleep 800)
    (check-and-clear-output #"Error!")
    (is (not (.exists tgt)))
    (spit tgt "x = 'foo'")
    (Thread/sleep 800)
    (is (.exists tgt))))

(def-fs-test basic-haml-compile-test
  (.mkdir (file "templates"))
  (spit (file "templates/foo.js.haml") "%div Fantabulous")
  (run-main)
  (is (.exists (file "public/templates.js")))
  (is (re-find #"Fantabulous" (slurp (file "public/templates.js")))))

(def-fs-test hidden-files-test
  (spit (file "coffeescripts/.#emacs-file.coffee") "x = 10")
  (run-main)
  (is (not (.exists (file "public/javascripts/.#emacs-file.js")))))

(def-fs-test single-target-file-test
  (run-main)
  (let [js (slurp (file "required.js"))]
    (is (re-find #"Thomas" js))
    (is (re-find #"mice" js))))

(def-watcher-test coffeescript-requires-test
  (Thread/sleep 2200)
  (let [contains-in-order?
          (fn [s & ss]
            (let [s (string/join " " (string/split s #"\n"))]
              (boolean
                (re-find
                  (re-pattern (string/join ".*" (for [s ss] (Pattern/quote s))))
                  s)))),
        f1 (file "cs-requires/foo1.coffee"),
        f2 (file "cs-requires/foo2.coffee"),
        t  (file "required.js")]
    (spit f1 "# {\"requires\": [\"foo2\"]}\nbar1=12")
    (spit f2 "bar2=5")
    (Thread/sleep 1100)
    (is (contains-in-order? (slurp t) "bar2" "bar1"))
    (spit f1 "bar1=4")
    (spit f2 "# {\"requires\": [\"foo1\"]}\nbar2=3")
    (Thread/sleep 700)
    (is (contains-in-order? (slurp t) "bar1" "bar2"))
    (spit f1 "# {\"requires\": [\"foo2\"]}\nbar1=1")
    (spit f2 "bar2=3")
    (Thread/sleep 700)
    (is (contains-in-order? (slurp t) "bar2" "bar1"))))

(def-watcher-test requirement-warning-test
  (let [f1 (file "cs-requires/foo.coffee")]
    (spit f1 "# {\"requires\": [\"barmaid\"]}\nx = y = z = 12")
    (Thread/sleep 1750)
    (check-and-clear-output #"WARNING.*Bad dependency declaration")))
