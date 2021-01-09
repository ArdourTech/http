(ns tech.ardour.http.middleware.misc
  (:require
    [lazy-map.core :refer [lazy-map ->LazyMap]]))

(defn wrap-lazy-map [handler]
  (fn [request]
    (handler (->LazyMap request))))
