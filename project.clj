(defproject tech.ardour/http "0.0.2-SNAPSHOT"
  :description "Ardour Tech HTTP Library"
  :url "https://github.com/ArdourTech/http"
  :license {:name         "Eclipse Public License - v 1.0"
            :url          "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments     "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [aleph "0.4.6"]
                 [metosin/reitit "0.5.11"]
                 [metosin/sieppari "0.0.0-alpha13"]
                 [tech.ardour/negotiator "0.0.2"]
                 [tech.ardour/logging "0.0.1"]
                 [io.jesi/url "0.3.0"]
                 [malabarba/lazy-map "1.3"]]
  :profiles {:test {:dependencies [[lambdaisland/kaocha "1.0.732"]]}}
  :source-paths ["src"]
  :test-paths ["test"]
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :sign-releases false}]]
  :aliases {"test" ["with-profile" "+test" "run" "-m" "kaocha.runner"]})
