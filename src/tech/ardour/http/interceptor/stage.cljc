(ns tech.ardour.http.interceptor.stage
  (:require
    [clojure.string :as str]
    [reitit.core :as r]
    [sieppari.context :as sc]
    [tech.ardour.negotiator.edn :as edn]
    [tech.ardour.negotiator.json :as json]
    [tech.ardour.logging.core :as log]))

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
  (log/debug "Getting all allowed CORS values")
  (let [keys (as-> (set keys) $
                   (if (contains? $ :handler)
                     (-> $ (disj :handler) (conj :all))
                     $)
                   (if (contains? $ :all)
                     (-> $ (disj :all) (conj :get :head :post :put :patch :delete))
                     $))]
    (log/debug "Allowed CORS values" {:values keys})
    (some->> keys
             seq
             (map (comp str/upper-case name))
             (str/join ", "))))

(def default-cors-headers {"Accept"                       (str/join "," [json/mime-type edn/mime-type])
                           "Accept-Encoding"              "gzip"
                           "Accept-Language"              "en-us,en;q=0.5"
                           "Access-Control-Allow-Headers" "Content-Type"
                           "Access-Control-Allow-Origin"  "*"
                           "Access-Control-Max-Age"       3600
                           "Vary"                         "Accept-Encoding, Origin"})

(defn create-pre-flight [cors-headers]
  (let [allow-methods (memoize cors-allow-method-value)]
    {:name  ::pre-flight
     :leave (fn pre-flight-leave [{:as ctx}]
              (assoc-in ctx
                [:response :headers "Access-Control-Allow-Origin"] (get cors-headers "Access-Control-Allow-Origin")))
     :enter (fn pre-flight-enter [{:keys [request-handler id] :as ctx}]
              (log/info "Validating Request Pre-Flight" {:request-id id})
              (let [match (:match ctx)
                    {:keys [uri request-method]} (get ctx :request)]
                (let [data (:data match)]
                  (cond
                    (and (= :options request-method)
                         (some? data))
                    (do
                      (log/info "Matched Pre-Flight" {:request-id id
                                                      :uri        uri})
                      (sc/terminate ctx
                        {:status  204
                         :headers (assoc cors-headers
                                    "Access-Control-Allow-Methods" (allow-methods (set (keys data))))}))

                    request-handler
                    ctx

                    :else
                    (do
                      (log/warn "Invalid Pre-Flight Request" {:request-id id
                                                              :uri        uri
                                                              :method     request-method})
                      (sc/terminate ctx
                        {:status  405
                         :headers {"Allow" (allow-methods (set (keys data)))}
                         :body    {:errors [{:message    (str "The method " request-method " is not allowed for uri " uri)
                                             :type       "http/method-not-allowed"
                                             :code       405
                                             :request-id id}]
                                   :data   {:uri    uri
                                            :method request-method}}}))))))}))

(def default-response-headers {"Cache-Control"   "no-cache"
                               "Connection"      "Keep-Alive"
                               "Accept-Encoding" "gzip,deflate"})

(defn create-add-default-headers [headers]
  {:name  ::add-default-headers
   :leave (fn add-default-headers-leave [{:as ctx}]
            (update-in ctx [:response :headers] #(merge headers %)))})

(defn create-match-handler [router]
  {:name  ::route-match-handler
   :enter (fn route-match-handler-enter [{:as ctx}]
            (let [request (get ctx :request)
                  [{:as match} request-handler] (match-handler router request)]
              (-> ctx
                  (assoc
                    :match match
                    :request-handler request-handler)
                  (assoc-in [:request :parameters] {:path  (:path-params match)
                                                    :query (:query-params request)}))))})
