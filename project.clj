(defproject edi-receiver (-> "resources/edi-receiver.VERSION" slurp .trim)
  :description "EDI receiver"
  :url "https://tbt-post.net/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [cheshire "5.10.1"
                  :exclusions [com.fasterxml.jackson.core/jackson-core
                               com.fasterxml.jackson.dataformat/jackson-dataformat-cbor
                               com.fasterxml.jackson.dataformat/jackson-dataformat-smile]]
                 [com.fasterxml.jackson.core/jackson-core "2.13.1"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.13.1"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.13.1"]

                 [org.clojure/java.data "1.0.95"]

                 ;; jdbc
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 [org.postgresql/postgresql "42.3.1"]
                 [mysql/mysql-connector-java "8.0.27"]

                 ;; HTTP client
                 [org.eclipse.jetty/jetty-client "9.4.35.v20201120"] ; same version as pedestal.jetty uses

                 ;; Logger
                 ; TODO: move logger to dev
                 [ch.qos.logback/logback-classic "1.2.10"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.32"]
                 [org.slf4j/jcl-over-slf4j "1.7.32"]
                 [org.slf4j/log4j-over-slf4j "1.7.32"]
                 [org.clojure/tools.logging "1.2.3"]]

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
                                        [org.clojure/tools.namespace "1.2.0"]
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
                                       [luposlip/json-schema "0.3.2"]

                                       ;; HTTP server
                                       [io.pedestal/pedestal.service "0.5.9"]
                                       [io.pedestal/pedestal.jetty "0.5.9"]
                                       ; Note: With reitit 0.5.11 have compile error:
                                       ; ClassNotFoundException: com.fasterxml.jackson.core.util.JacksonFeature
                                       ; error may be related with update to jsonista 0.3.0 with jackson 2.12.0
                                       [metosin/reitit "0.5.15"
                                        :exclusions [com.fasterxml.jackson.datatype/jackson-datatype-jsr310]]
                                       [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.13.1"]
                                       [metosin/reitit-pedestal "0.5.15"]

                                       ;; Kafka
                                       [net.tbt-post/clj-kafka-x "0.6.0"
                                        :exclusions [com.fasterxml.jackson.dataformat/jackson-dataformat-csv
                                                     com.fasterxml.jackson.datatype/jackson-datatype-jdk8
                                                     com.fasterxml.jackson.module/jackson-module-scala_2.12]]
                                       [com.fasterxml.jackson.dataformat/jackson-dataformat-csv "2.13.1"]
                                       [com.fasterxml.jackson.datatype/jackson-datatype-jdk8 "2.13.1"]
                                       [com.fasterxml.jackson.module/jackson-module-scala_2.13 "2.13.1"]

                                       ;; SMTP
                                       [com.sun.mail/javax.mail "1.6.2"]]}}

  :plugins [[lein-ancient "0.7.0"]])
