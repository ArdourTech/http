(ns tech.ardour.http.interceptor.logging
  (:require
    [tech.ardour.logging :as log]
    [tech.ardour.utensil :as u]))

(def request-id
  {:name  ::request-id
   :enter (fn assoc-request-id-enter [{:as ctx}]
            (assoc ctx :id (str (u/uuid))))
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
              (assoc ctx ::start-time (u/epoch-millis)))
     :leave (fn request-leave [{:as ctx}]
              (log/info "Finished Request" (assoc (log-ctx ctx)
                                             :duration (- (u/epoch-millis) (get ctx ::start-time))
                                             :status (get-in ctx [:response :status])))
              ctx)}))
