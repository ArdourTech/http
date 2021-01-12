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

(def default-cors-headers stage/default-cors-headers)
(def default-response-headers stage/default-response-headers)
(def default-headers {:cors     default-cors-headers
                      :response default-response-headers})

(defn handler [routes & [{:keys                   [inject-middleware
                                                   request-ids?
                                                   request-logging?
                                                   throw-exceptions?
                                                   lazy-body-decode?
                                                   content-negotiation?
                                                   decode-params?]
                          {:keys [cors response]} :headers
                          :or                     {throw-exceptions?    false
                                                   lazy-body-decode?    true
                                                   content-negotiation? true
                                                   decode-params?       true}
                          :as                     opts}]]
  (let [router (r/router routes)
        response-headers (get-in opts [:headers :response])
        cors-headers (get-in opts [:headers :cors])
        middleware (cond-> (sorted-map)
                     request-ids? (assoc 100 logging/wrap-request-id)
                     request-logging? (assoc 200 logging/wrap-logging)
                     response-headers (assoc 300 (partial stage/wrap-response-headers response-headers))
                     cors-headers (assoc 400 (partial stage/wrap-pre-flight-handler cors-headers))
                     (not throw-exceptions?) (assoc 500 exception/wrap-handler)
                     lazy-body-decode? (assoc 600 misc/wrap-lazy-map)
                     content-negotiation? (assoc 700 content/wrap-negotiation)
                     decode-params? (assoc 800 params/wrap-decode)
                     inject-middleware inject-middleware)]
    (assert (and (map? middleware)
                 (sorted? middleware)))
    (->> (concat
           [(fn [{:keys [request-handler] :as request}]
              (request-handler request))]
           (reverse (vals middleware))
           [(partial stage/wrap-match-handler router)])
         (reduce (fn [v h] (h v))))))

(defn start [{:keys [port routes compression?]
              :or   {port         8080
                     compression? true}
              :as   opts}]
  {:pre [(some? routes)]}
  (log/info "Starting Web Server" {:host "localhost"
                                   :port port})
  (http/start-server (handler routes opts) {:compression? compression?
                                            :port         port}))

(defn stop-server [^Closeable server]
  (log/info "Stopping Web Server")
  (.close server))
