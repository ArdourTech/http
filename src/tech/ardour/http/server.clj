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

(defn handler [routes]
  (let [router (r/router routes)]
    (-> (fn [{:keys [request-handler] :as request}]
          (request-handler request))
        params/wrap-decode
        content/wrap-negotiation
        misc/wrap-lazy-map
        stage/wrap-pre-handler
        (stage/wrap-match-handler router)
        exception/wrap-handler
        logging/wrap-timing
        logging/wrap-request-id)))

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
