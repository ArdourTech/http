(ns tech.ardour.http.middleware.params
  (:require
    [cemerick.url :refer [query->map]]
    [clojure.string :as str]
    [tech.ardour.negotiator :as n]
    [tech.ardour.negotiator.convert :as convert]))

(defn wrap-decode [handler]
  (fn [{:keys [query-string] :as request}]
    (handler
      (if (str/blank? query-string)
        request
        (assoc request :query-params (delay (->> query-string
                                                 (query->map)
                                                 (n/transform-keys string? (comp keyword convert/->kebab-case)))))))))
