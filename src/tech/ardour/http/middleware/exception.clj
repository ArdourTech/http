(ns tech.ardour.http.middleware.exception
  (:require
    [slingshot.slingshot :refer [try+]]
    [tech.ardour.http.middleware.content :as content]
    [tech.ardour.logging.core :as log]))

(defn wrap-handler [handler]
  (fn [{:keys [id] :as request}]
    (try+
      (handler request)
      ;TODO Add More reasons
      (catch Exception e
        (log/error e "Unhandled Exception")
        (content/encode request {:status 500
                                 :body   {:request-id id}})))))
