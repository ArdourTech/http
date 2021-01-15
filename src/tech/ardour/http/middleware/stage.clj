(ns tech.ardour.http.middleware.stage
  (:require
    [clojure.string :as str]
    [reitit.core :as r]
    [tech.ardour.negotiator.edn :as edn]
    [tech.ardour.negotiator.json :as json]))

(defn- match-handler [router {:keys [uri request-method]}]
  (when-let [{:keys [data] :as match} (r/match-by-path router uri)]
    [match
     (cond
       (fn? data) data
       (map? data) (or (get-in data [request-method :handler])
                       (get data request-method)
                       (get data :handler)
                       (get-in data [:all :handler])
                       (get data :all))
       :else nil)]))

(defn- cors-allow-method-value [keys]
  (let [keys (as-> (set keys) $
                   (if (contains? $ :handler)
                     (-> $ (disj :handler) (conj :all))
                     $)
                   (if (contains? $ :all)
                     (-> $ (disj :all) (conj :get :head :post :put :patch :delete))
                     $))]
    (->> keys
         (map (comp str/upper-case name))
         (str/join ", "))))

(def default-cors-headers {"accept"                       (str/join "," [json/mime-type edn/mime-type])
                           "accept-encoding"              "gzip"
                           "accept-language"              "en-us,en;q=0.5"
                           "access-control-allow-headers" "content-type"
                           "access-control-allow-origin"  "*"
                           "access-control-max-age"       3600
                           "vary"                         "accept-encoding, origin"})

(defn wrap-pre-flight-handler [cors-headers handler]
  (let [allow-methods (memoize cors-allow-method-value)]
    (fn [{::keys [match]
          :keys  [id uri request-method request-handler] :as request}]
      (let [data (:data match)]
        (cond
          (and (= :options request-method)
               (some? data))
          {:status  204
           :headers (assoc cors-headers
                      "access-control-allow-methods" (allow-methods (set (keys data))))}

          request-handler
          (assoc-in
            (handler request)
            [:headers "access-control-allow-origin"] (get cors-headers "access-control-allow-origin"))

          :else {:status 405
                 :body   {:uri        uri
                          :request-id id
                          :method     request-method}})))))

(def default-response-headers {"cache-control"   "no-cache"
                               "connection"      "keep-alive"
                               "accept-encoding" "gzip,deflate"})

(defn wrap-response-headers [headers handler]
  (fn [{:as request}]
    (let [response (handler request)]
      (update response :headers #(merge headers %)))))

(defn wrap-match-handler [router handler]
  (fn [{:as request}]
    (let [[{:as match} request-handler] (match-handler router request)]
      (handler (assoc request
                 :request-handler request-handler
                 :path-params (:path-params match)
                 ::match match)))))
