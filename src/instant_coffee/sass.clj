(ns instant-coffee.sass
  (:use [instant-coffee.jruby]))

(defn compile-something
  [sass-lang src]
  (jruby-eval
    (str
      "require 'rubygems'
       require 'sass'
       Sass::Engine.new(" (pr-str src) ",
                         :syntax => '" (name sass-lang)
      "').render")))

(def compile-sass (partial compile-something "sass"))
(def compile-scss (partial compile-something "scss"))
