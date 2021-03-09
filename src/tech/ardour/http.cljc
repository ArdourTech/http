(ns tech.ardour.http
  (:require
    [reitit.core :as r]
    [sieppari.core :as s]
    [tech.ardour.http.interceptor.logging :as logging-interceptor]
    [tech.ardour.http.interceptor.stage :as stage-interceptor]
    [tech.ardour.http.interceptor.exception :as exception-interceptor]
    [tech.ardour.http.interceptor.misc :as misc-interceptor]))

(def default-cors-headers stage-interceptor/default-cors-headers)
(def default-response-headers stage-interceptor/default-response-headers)
(def default-headers {:cors     default-cors-headers
                      :response default-response-headers})

(defn ->handler [routes & [{:keys                   [inject-interceptors
                                                     request-ids?
                                                     request-logging?
                                                     throw-exceptions?
                                                     lazy-body-decode?
                                                     content-negotiation?]
                            {:keys [cors response]} :headers
                            :or                     {request-ids?         true
                                                     request-logging?     true
                                                     throw-exceptions?    false
                                                     lazy-body-decode?    true
                                                     content-negotiation? true}
                            :as                     opts}]]
  (let [router (r/router routes)
        response-headers (get-in opts [:headers :response] default-response-headers)
        cors-headers (get-in opts [:headers :cors] default-cors-headers)
        chain-map (cond-> (sorted-map)
                    request-ids? (assoc 100 logging-interceptor/request-id)
                    request-logging? (assoc 200 logging-interceptor/request)
                    response-headers (assoc 300 (stage-interceptor/create-add-default-headers response-headers))
                    cors-headers (assoc 400 (stage-interceptor/create-pre-flight cors-headers))
                    (not throw-exceptions?) (assoc 500 exception-interceptor/server-error)
                    lazy-body-decode? (assoc 600 misc-interceptor/lazy-map)
                    ;TODO
                    ;content-negotiation? (assoc 700 (content-interceptor/negotiation content-types))
                    inject-interceptors inject-interceptors)]
    (assert (and (map? chain-map)
                 (sorted? chain-map)))
    (partial s/execute (concat
                         [(stage-interceptor/create-match-handler router)]
                         (vals chain-map)
                         [{:name  ::handler
                           :enter (fn [{:keys [request-handler request] :as ctx}]
                                    (request-handler ctx))}]))))
