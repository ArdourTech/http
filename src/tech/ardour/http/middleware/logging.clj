(ns tech.ardour.http.middleware.logging
  (:require
    [tech.ardour.logging.core :as log])
  (:import
    (java.util UUID)
    (java.time Instant)))

(defn epoch-milli []
  (-> (Instant/now)
      (.toEpochMilli)))

(defn wrap-request-id [handler]
  (fn [request]
    (-> request
        (assoc :id (UUID/randomUUID))
        handler)))

(defn wrap-timing [handler]
  (fn [{:keys [uri request-method id] :as request}]
    (log/info "Starting Request" {:request-id     id
                                  :uri            uri
                                  :request-method request-method})
    (let [n (epoch-milli)
          response (handler request)]
      (log/info "Finished Request" {:request-id     id
                                    :uri            uri
                                    :request-method request-method
                                    :status         (:status response)
                                    :duration       (- (epoch-milli) n)})
      response)))
