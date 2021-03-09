(ns tech.ardour.http.interceptor.exception
  (:require
    [tech.ardour.logging :as log]))

(def server-error
  {:name  ::server-error
   :error (fn server-error-error [{:keys [error id request] :as ctx}]
            (log/error error "Unhandled Exception")
            (-> ctx
                (update :response #(or % {:status 500
                                          :body   {:errors [{:message    "Something went wrong performing the request"
                                                             :type       "http/internal-server-error"
                                                             :code       500
                                                             :request-id id}]
                                                   :data   {:uri            (:uri request)
                                                            :request-method (:request-method request)}}}))
                (dissoc :error)))})
