(ns instant-coffee.cache
  (:use [instant-coffee.config :only [file]])
  (:import java.security.MessageDigest))

(def hexes "0123456789abcdef")

(defn byte-to-hex-string
  [b]
  (let [lower (bit-and b 15),
        upper (bit-and (bit-shift-right b 4) 15)]
    (str (nth hexes upper) (nth hexes lower))))

(defn byte-array-to-hex-string
  [ba]
  (apply str (map byte-to-hex-string (seq ba))))

(defn sha1
  [s]
  (let [md (MessageDigest/getInstance "SHA-1")]
    (.update md (.getBytes s "iso-8859-1") 0 (count s))
    (byte-array-to-hex-string (.digest md))))


(defprotocol ICache
  (cache-get       [this key] "Retrieves the compiled code based on the source's hash")
  (cache-set       [this key value] "Sets the you know whatever.")
  (cache-set-error [this key error-msg]))

(defn- good-file
  [key]
  (file (format "tmp/ic-cache/%s/%s"
                (.substring key 0 2)
                (.substring key 2))))
(defn- bad-file
  [key]
  (file (format "tmp/ic-cache/%s/%s.error"
                (.substring key 0 2)
                (.substring key 2))))

(defrecord FSCache [dir]
  ICache
  (cache-get
    [this key]
    (cond
      (.exists (good-file key))
        {:code (slurp (good-file key))}
      (.exists (bad-file key))
        {:error (slurp (bad-file key))}))
  (cache-set
    [this key value]
    (let [f (good-file key)]
      (.mkdirs (.getParentFile f))
      (spit f value)))
  (cache-set-error
    [this key error-msg]
    (let [f (bad-file key)]
      (.mkdirs (.getParentFile f))
      (spit f error-msg))))

(defrecord
  #^{:doc "Argument 'mem' should be an atom initialized to an empty map."}
  MemCache [mem]
  ICache
  (cache-get
    [this key]
    (@mem key))
  (cache-set
    [this key value]
    (swap! mem assoc key {:code value}))
  (cache-set-error
    [this key error-msg]
    (swap! mem assoc key {:error error-msg})))

(defn create-memcache [] (new MemCache (atom {})))

(defn create-fs-cache
  []
  (.mkdirs (file "tmp/ic-cache"))
  (new FSCache nil))
