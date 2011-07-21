(ns instant-coffee.cache)

(defprotocol ICache
  (cache-get       [this key] "Retrieves the compiled code based on the source's hash")
  (cache-set       [this key value] "Sets the you know whatever.")
  (cache-set-error [this key error-msg]))

(defrecord FSCache [dir]
  ICache
  (cache-get
    [this key])
  (cache-set
    [this key value])
  (cache-set-error
    [this key error-msg]))

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
