(ns tech.ardour.http.server
  (:require
    [aleph.http :as aleph]
    [tech.ardour.http :as http]
    [tech.ardour.logging :as log])
  (:import
    (java.io Closeable)))

;TODO Make this work with interceptors
(defn start [{:keys [port routes compression?]
              :or   {port         8080
                     compression? true}
              :as   opts}]
  {:pre [(some? routes)]}
  (log/info "Starting Web Server" {:host "localhost"
                                   :port port})
  (aleph/start-server (http/->handler routes opts) {:compression? compression?
                                                    :port         port}))

(defn stop-server [^Closeable server]
  (log/info "Stopping Web Server")
  (.close server))
