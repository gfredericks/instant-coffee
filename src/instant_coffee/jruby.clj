(ns instant-coffee.jruby
  "Codez for running JRuby."
  (:import org.jruby.embed.ScriptingContainer))

(defn jruby-eval
  [s]
  (let [sc (new ScriptingContainer)]
    (.runScriptlet sc s)))
