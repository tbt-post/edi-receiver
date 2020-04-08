(defproject edi-receiver "0.1.0"
  :description "EDI receiver"
  :url "https://tbt-post.net/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]

                 ;; JSON
                 [cheshire "5.10.0"]
                 [luposlip/json-schema "0.1.8"]

                 ;; postgres
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.postgresql/postgresql "42.2.12"]
                 [com.mchange/c3p0 "0.9.5.5"]

                 ;; HTTP
                 [io.pedestal/pedestal.service "0.5.7"]
                 [io.pedestal/pedestal.jetty "0.5.7"]
                 [metosin/reitit "0.4.2"]
                 [metosin/reitit-pedestal "0.4.2"]
                 [org.eclipse.jetty/jetty-client "9.4.18.v20190429"] ; same version as pedestal uses

                 ;; Logger
                 ; TODO: move logger to dev
                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 [org.slf4j/jcl-over-slf4j "1.7.30"]
                 [org.slf4j/log4j-over-slf4j "1.7.30"]
                 [org.clojure/tools.logging "1.0.0"]]


  ;:omit-source true
  ;:warn-on-reflection true

  :profiles {:dev     {:dependencies [[com.stuartsierra/component.repl "0.2.0"]
                                      [org.clojure/tools.namespace "1.0.0"]
                                      [hawk "0.2.11"]]
                       :source-paths ["dev"]}
             :uberjar {:aot  :all
                       :omit-source true
                       :main edi-receiver.core}}

  :plugins [[lein-ancient "0.6.15"]])
