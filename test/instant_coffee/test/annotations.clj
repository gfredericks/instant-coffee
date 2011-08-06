(ns instant-coffee.test.annotations
  (:use clojure.test
        instant-coffee.annotations))

(deftest simple-write-read-test
  (let [data {:tommy ["Hilfigger" "has" "tommies"]},
        code "x = 12; \n y = [1,2,4]; alert('WAAAAt');",
        annotated (add-annotation "////" code data)]
    (is (= data (read-annotation "////" annotated)))))
