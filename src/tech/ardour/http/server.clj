(ns tech.ardour.http.server
  (:require
    [aleph.http :as http]
    [reitit.core :as r]
    [tech.ardour.http.middleware.content :as content]
    [tech.ardour.http.middleware.exception :as exception]
    [tech.ardour.http.middleware.logging :as logging]
    [tech.ardour.http.middleware.misc :as misc]
    [tech.ardour.http.middleware.params :as params]
    [tech.ardour.http.middleware.stage :as stage]
    [tech.ardour.logging.core :as log])
  (:import
    (java.io Closeable)))

(defn handler [routes & [inject-middle-ware]]
  (let [router (r/router routes)
        default-middleware ((or inject-middle-ware identity)
                            (sorted-map
                              0 logging/wrap-request-id
                              10 logging/wrap-timing
                              20 exception/wrap-handler
                              40 stage/wrap-pre-flight-handler
                              50 misc/wrap-lazy-map
                              60 content/wrap-negotiation
                              70 params/wrap-decode))]
    (->> (concat
           [(fn [{:keys [request-handler] :as request}]
              (request-handler request))]
           (vals default-middleware)
           [(partial stage/wrap-match-handler router)])
         (reduce (fn [v h] (h v))))))

(defn start [{:keys [port]
              :or   {port 8080}
              :as   system}
             routes]
  (log/info "Starting Web Server" {:host "localhost"
                                   :port port})
  (http/start-server (handler routes) {:compression? true
                                       :port         port}))

(defn stop-server [^Closeable server]
  (log/info "Stopping Web Server")
  (.close server))
