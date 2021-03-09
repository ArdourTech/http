(ns tech.ardour.http.interceptor.misc
  (:require
    #?(:clj
       [lazy-map.core :refer [->LazyMap]])))

(def lazy-map
  {:name  ::lazy-map
   :enter (fn lazy-map-enter [{:as ctx}]
            #?(:cljs ctx
               :clj (update ctx :request ->LazyMap)))})
