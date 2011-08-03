(defproject instant-coffee "0.0.2"
  :description "a dev tool to compile coffeescript to javascript, manage templating with haml-js and someday scss"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [com.gfredericks/jcoffeescript "1.1.0"]
                 [gfrlog/clj-yaml "0.3.0"]
                 [commons-io/commons-io "2.0.1"]
                 [slingshot "0.2.0"]]
  :main instant-coffee.core)
