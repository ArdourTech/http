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

(def default-cors-headers {"Accept"                       (str/join "," [json/mime-type edn/mime-type])
                           "Accept-Encoding"              "gzip"
                           "Accept-Language"              "en-us,en;q=0.5"
                           "Access-Control-Allow-Headers" "Content-Type"
                           "Access-Control-Allow-Origin"  "*"
                           "Access-Control-Max-Age"       3600
                           "Vary"                         "Accept-Encoding, Origin"})

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
                      "Access-Control-Allow-Methods" (allow-methods (set (keys data))))}

          request-handler
          (assoc-in
            (handler request)
            [:headers "Access-Control-Allow-Origin"] (get cors-headers "Access-Control-Allow-Origin"))

          :else {:status 405
                 :body   {:uri        uri
                          :request-id id
                          :method     request-method}})))))

(def default-response-headers {"Cache-Control"   "no-cache"
                               "Connection"      "Keep-Alive"
                               "Accept-Encoding" "gzip,deflate"})

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
