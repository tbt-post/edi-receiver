(defproject edi-receiver (-> "resources/edi-receiver.VERSION" slurp .trim)
  :description "EDI receiver"
  :url "https://tbt-post.net/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [cheshire "5.10.0"]
                 [org.clojure/java.data "1.0.78"]

                 ;; jdbc
                 [org.clojure/java.jdbc "0.7.11"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 [org.postgresql/postgresql "42.2.12"]
                 [mysql/mysql-connector-java "8.0.20"]

                 ;; Logger
                 ; TODO: move logger to dev
                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 [org.slf4j/jcl-over-slf4j "1.7.30"]
                 [org.slf4j/log4j-over-slf4j "1.7.30"]
                 [org.clojure/tools.logging "1.1.0"]]

  ; to remove default "src" from classpath
  :source-paths []
  :java-source-paths ["java"]
  :javac-options ["-source" "1.8" "-target" "1.8"]

  :profiles {:dev      [:r-deps
                        {:source-paths      ["dev" "src/common" "src/control" "src/receiver"]
                         :target-path       "target/%s/"
                         :dependencies      [[com.stuartsierra/component.repl "0.2.0"]
                                             [org.clojure/tools.namespace "1.0.0"]
                                             [hawk "0.2.11"]]}]
             :common   {:source-paths ["src/common"]
                        :target-path  "target/%s/"
                        :jar-name     "edi-common.jar"
                        :uberjar-name "edi-common-standalone.jar"
                        :aot          :all
                        :omit-source  true}
             :control  {:source-paths ["src/common" "src/control"]
                        :target-path  "target/%s/"
                        :jar-name     "edi-control.jar"
                        :uberjar-name "edi-control-standalone.jar"
                        :aot          :all
                        :omit-source  true
                        :main         edi.control.core}
             :receiver [:r-deps
                        {:source-paths      ["src/common" "src/receiver"]
                         :target-path       "target/%s/"
                         :jar-name          "edi-receiver.jar"
                         :uberjar-name      "edi-receiver-standalone.jar"
                         :aot               :all
                         :omit-source       true
                         :main              edi.receiver.core}]
             :r-deps   {:dependencies [[luposlip/json-schema "0.2.4"]

                                       ;; HTTP
                                       [io.pedestal/pedestal.service "0.5.8"]
                                       [io.pedestal/pedestal.jetty "0.5.8"]
                                       [metosin/reitit "0.5.2"]
                                       [metosin/reitit-pedestal "0.5.2"]
                                       [org.eclipse.jetty/jetty-client "9.4.18.v20190429"] ; same version as pedestal uses

                                       ;; Kafka
                                       [net.tbt-post/clj-kafka-x "0.4.0"]]}}

  :plugins [[lein-ancient "0.6.15"]])
