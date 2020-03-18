(defproject edi-receiver "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]

                 ;; JSON
                 [cheshire "5.10.0"]
                 [luposlip/json-schema "0.1.8"]

                 ;; HTTP
                 [org.eclipse.jetty/jetty-client "9.4.18.v20190429"]  ; same version as in pedestal
                 [io.pedestal/pedestal.service "0.5.7"]
                 [io.pedestal/pedestal.jetty "0.5.7"]
                 [metosin/reitit "0.4.2"]
                 [metosin/reitit-pedestal "0.4.2"]

                 ;; Logger
                 ; TODO: move logger to dev
                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 [org.slf4j/jcl-over-slf4j "1.7.30"]
                 [org.slf4j/log4j-over-slf4j "1.7.30"]
                 [org.clojure/tools.logging "1.0.0"]]

  :main edi-receiver.core
  :omit-source true
  ;:warn-on-reflection true
  :profiles {:uberjar {:aot :all}}
  :resource-paths ["resources"]
  :repl-options {:init-ns edi-receiver.core}
  :plugins [[lein-ancient "0.6.15"]])
