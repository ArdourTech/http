(ns tech.ardour.http.middleware.content
  (:require
    [byte-streams :as bs]
    [tech.ardour.negotiator :as negotiator]
    [tech.ardour.negotiator.json :as json]))

(defn encode [{{:strs [content-type accept]} :headers
               :as                           request}
              response]
  (let [accept (or accept content-type json/mime-type)]
    (-> response
        (update :body #(some->> % (negotiator/encode accept)))
        (assoc-in [:headers "content-type"] accept))))

(defn wrap-negotiation [handler]
  (fn [{{:strs [content-type]} :headers
        :as                    request}]
    (encode request (-> request
                        (update :body #(delay (some->> %
                                                       bs/to-string
                                                       (negotiator/decode content-type))))
                        handler))))
