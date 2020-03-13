(defproject edi-receiver "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 [mount "0.1.16"]
                 [luposlip/json-schema "0.1.8"]
                 [clj-http "3.10.0"]
                 [cheshire "5.10.0"]

                 [aleph "0.4.6"]
                 [metosin/reitit "0.3.9"]

                 ;[org.clojure/tools.nrepl "0.2.13"]

                 ; log
                 ;; Logger
                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.28"]
                 [org.slf4j/jcl-over-slf4j "1.7.28"]
                 [org.slf4j/log4j-over-slf4j "1.7.28"]
                 [org.clojure/tools.logging "0.5.0"]
                 [ring-logger "1.0.1"]

                 ; dev
                 [org.clojure/tools.namespace "0.3.1"]
                 [hawk "0.2.11"]

                 [local.1st/clj-helpers-common "0.1.25"]]

  :repl-options {:init-ns edi-receiver.core}
  :repositories [["private-jars" "http://local.repo:9180/repo"]])
