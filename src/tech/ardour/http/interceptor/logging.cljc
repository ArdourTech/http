(ns tech.ardour.http.interceptor.logging
  (:refer-clojure :exclude [uuid])
  (:require
    [tech.ardour.logging.core :as log])
  #?(:clj (:import
            (java.util UUID)
            (java.time Instant))))

(defn epoch-milli []
  #?(:clj  (-> (Instant/now)
               (.toEpochMilli))
     :cljs (system-time)))

(defn- uuid []
  #?(:clj  (UUID/randomUUID)
     :cljs (random-uuid)))

(def request-id
  {:name  ::request-id
   :enter (fn assoc-request-id-enter [{:as ctx}]
            (assoc ctx :id (str (uuid))))
   :leave (fn assoc-request-id-leave [{:as ctx}]
            (assoc-in ctx
              [:response :headers "Request-Id"] (get ctx :id)))})

(def request
  (let [log-ctx (fn [ctx]
                  {:request-id     (get ctx :id)
                   :uri            (get-in ctx [:request :uri])
                   :request-method (get-in ctx [:request :request-method])})]
    {:name  ::request
     :enter (fn request-enter [{{:keys [uri request-method id]} :request
                                :as                             ctx}]
              (log/info "Starting Request" (log-ctx ctx))
              (assoc ctx ::start-time (epoch-milli)))
     :leave (fn request-leave [{:as ctx}]
              (log/info "Finished Request" (assoc (log-ctx ctx)
                                             :duration (- (epoch-milli) (get ctx ::start-time))
                                             :status (get-in ctx [:response :status])))
              ctx)}))
