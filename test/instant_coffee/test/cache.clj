(ns instant-coffee.test.cache
  (:use clojure.test
        instant-coffee.cache))

(deftest memcache-test
  (let [mc (create-memcache)]
    (is (nil? (cache-get mc "abc123")))
    (cache-set mc "abc123" "tompkins")
    (is (= {:code "tompkins"} (cache-get mc "abc123")))
    (cache-set-error mc "abc456" "Ewwy gross")
    (is (= {:error "Ewwy gross"} (cache-get mc "abc456")))))
