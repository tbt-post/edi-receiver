(defproject edi-receiver "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "1.0.194"]

                 ;; JSON
                 [luposlip/json-schema "0.1.8"]
                 [cheshire "5.10.0"]

                 ; TODO: aleph -> pedestal
                 ;; HTTP
                 [org.eclipse.jetty/jetty-client "9.4.18.v20190429"]
                 [aleph "0.4.6"]
                 [metosin/reitit "0.3.9"]

                 ;; Logger
                 ; TODO: move logger to dev
                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.28"]
                 [org.slf4j/jcl-over-slf4j "1.7.28"]
                 [org.slf4j/log4j-over-slf4j "1.7.28"]
                 [org.clojure/tools.logging "0.5.0"]]

  :main edi-receiver.core
  :omit-source true
  ;:warn-on-reflection true
  :profiles {:uberjar {:aot :all}}
  :resource-paths ["resources"]
  :repl-options {:init-ns edi-receiver.core})
