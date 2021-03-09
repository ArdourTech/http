(ns tech.ardour.http.lambda
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [tech.ardour.http :as http]
    [tech.ardour.negotiator.js :as n-js]
    [tech.ardour.negotiator.json :as n-json]
    [tech.ardour.utensil :as u]))

(defn- ^js ->lambda-proxy-response
  "Converts a Ring Response into a Lambda Proxy Response"
  [response]
  (-> response
      (set/rename-keys {:status :status-code})
      (assoc-in [:headers "Content-Type"] "application/json")
      (assoc :is-base64-encoded false)
      (u/update-some :body n-json/write)
      n-js/write))

(defn- lambda-proxy-event->request
  "Converts a Lambda Proxy Event into a Ring Request"
  [^js event]
  (let [{:keys          [is-base64-encoded]
         {:keys [http]} :request-context
         :as            request} (n-js/read event)]
    (-> request
        (set/rename-keys {:query-string-parameters :query-params})
        (assoc
          :uri (some-> http :path)
          :request-method (some-> http :method str/lower-case keyword))
        (u/update-some :body #(when (string? %)
                                (cond-> %
                                  is-base64-encoded js/atob
                                  :always n-json/read))))))

(defn create-handler
  "High-order function to create an AWS lambda handler"
  [routes & [http-opts]]
  (let [handler (http/->handler routes http-opts)]
    (fn -lambda-handler [^js event ^js _ callback]
      (let [cb (comp (partial callback nil) ->lambda-proxy-response)]
        (-> event
            lambda-proxy-event->request
            (handler cb cb))))))
