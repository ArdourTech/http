(ns tech.ardour.http.middleware.content
  (:require
    [byte-streams :as bs]
    [tech.ardour.negotiator :as negotiator]
    [tech.ardour.negotiator.json :as json]))

(defn encode [{{:strs [content-type accept]} :headers
               :as                           request}
              response]
  (let [response-content-type (get-in response [:headers "content-type"])
        content-type (or response-content-type
                         accept
                         content-type
                         json/mime-type)]
    (-> response
        (update :body #(some->> % (negotiator/encode content-type)))
        (assoc-in [:headers "content-type"] content-type))))

(defn wrap-negotiation [handler]
  (fn [{{:strs [content-type]} :headers
        :as                    request}]
    (encode request (-> request
                        (update :body #(delay (some->> %
                                                       bs/to-string
                                                       (negotiator/decode content-type))))
                        handler))))
