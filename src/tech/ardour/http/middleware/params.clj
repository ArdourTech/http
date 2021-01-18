(ns tech.ardour.http.middleware.params
  (:require
    [cemerick.url :refer [query->map]]
    [clojure.string :as str]
    [tech.ardour.negotiator :as n]
    [tech.ardour.negotiator.convert :as convert])
  (:import
    [java.net URLDecoder]
    [java.nio.charset Charset]))

(def ^:private ^Charset utf-8-charset (Charset/forName "utf-8"))

(defn wrap-decode [handler]
  (fn [{:keys [query-string] :as request}]
    (handler
      (if (str/blank? query-string)
        request
        (assoc request :query-params (delay (->> (URLDecoder/decode ^String query-string utf-8-charset)
                                                 (query->map)
                                                 (n/transform-keys string? (comp keyword convert/->kebab-case)))))))))
