(defproject instant-coffee "0.0.4"
  :description "a dev tool to compile coffeescript to javascript, manage templating with haml-js and someday scss"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [gfrlog/clj-yaml "0.3.0"]
                 [commons-io/commons-io "2.0.1"]
                 [org.clojure/data.json "0.1.1"]
                 [slingshot "0.2.0"]
                 [org.jruby/jruby-complete "1.6.4"]
                 ; These last two are installed locally with maven. The
                 ; jcoffeescript jar can be downloaded from its github
                 ; page, while the sass-gems jar can be compiled using
                 ; the script in the script directory.
                 [com.gfredericks/jcoffeescript "1.1.0"]
                 [com.gfredericks/sass-gems "3.1.7"]]

  :main instant-coffee.core)
