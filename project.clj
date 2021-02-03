(defproject edi-receiver (-> "resources/edi-receiver.VERSION" slurp .trim)
  :description "EDI receiver"
  :url "https://tbt-post.net/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.2"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.cli "1.0.194"]
                 [cheshire "5.10.0"]
                 [org.clojure/java.data "1.0.86"]

                 ;; jdbc
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 [org.postgresql/postgresql "42.2.18"]
                 [mysql/mysql-connector-java "8.0.23"]

                 ;; HTTP client
                 [org.eclipse.jetty/jetty-client "9.4.18.v20190429"] ; same version as pedestal.jetty uses

                 ;; Logger
                 ; TODO: move logger to dev
                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 [org.slf4j/jcl-over-slf4j "1.7.30"]
                 [org.slf4j/log4j-over-slf4j "1.7.30"]
                 [org.clojure/tools.logging "1.1.0"]]

  :source-paths []
  :aliases {"repl"      ["do" "with-profile" "precomp" "javac," "repl"]
            "test"      ["do" "with-profile" "precomp" "javac," "test"]
            "build-all" ["do"
                         "with-profile" "control" "uberjar,"
                         "with-profile" "receiver" "uberjar"]}

  :profiles {:precomp  {:java-source-paths ["java"]}
             :dev      [:r-deps
                        {:source-paths ["dev" "src/common" "src/control" "src/receiver"]
                         :dependencies [[com.stuartsierra/component.repl "0.2.0"]
                                        [org.clojure/tools.namespace "1.1.0"]
                                        [hawk "0.2.11"]]}]
             :common   [:precomp
                        {:source-paths ["src/common"]
                         :target-path  "target/%s/"
                         :jar-name     "edi-common.jar"
                         :uberjar-name "edi-common-standalone.jar"
                         :aot          :all
                         :omit-source  true}]
             :control  [:precomp
                        {:source-paths ["src/common" "src/control"]
                         :target-path  "target/%s/"
                         :jar-name     "edi-control.jar"
                         :uberjar-name "edi-control-standalone.jar"
                         :aot          :all
                         :omit-source  true
                         :main         edi.control.core}]
             :receiver [:precomp :r-deps
                        {:source-paths ["src/common" "src/receiver"]
                         :target-path  "target/%s/"
                         :jar-name     "edi-receiver.jar"
                         :uberjar-name "edi-receiver-standalone.jar"
                         :aot          :all
                         :omit-source  true
                         :main         edi.receiver.core}]
             :r-deps   {:dependencies [[medley "1.3.0"]
                                       [luposlip/json-schema "0.2.9"]

                                       ;; HTTP server
                                       [io.pedestal/pedestal.service "0.5.8"]
                                       [io.pedestal/pedestal.jetty "0.5.8"]
                                       ; Note: With reitit 0.5.11 have compile error:
                                       ; ClassNotFoundException: com.fasterxml.jackson.core.util.JacksonFeature
                                       ; error may be related with update to jsonista 0.3.0 with jackson 2.12.0
                                       [metosin/reitit "0.5.10"]
                                       [metosin/reitit-pedestal "0.5.10"]

                                       ;; Kafka
                                       [net.tbt-post/clj-kafka-x "0.5.0"]

                                       ;; SMTP
                                       [com.sun.mail/javax.mail "1.6.2"]]}}

  :plugins [[lein-ancient "0.6.15"]])
