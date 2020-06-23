(ns edi.receiver.utils.expression
  (:require [clojure.string :as string]))


(def ^:private fns {"and" (fn [& args] (every? identity args))
                    "or"  (fn [& args] (or (some identity args) false))})


(defn- resolve-fn [symbol]
  (or (get fns (name symbol))
      (resolve symbol)
      (throw (ex-info (format "unknown function: %s\nknown functions are: %s"
                              (name symbol)
                              (->> fns keys sort (string/join " "))) {}))))


(defn prepare-path [edn]
  (->> (string/split (name edn) #"\.")
       (mapv keyword)))


(defn prepare [edn]
  (cond (list? edn)
        ; make vector [f & args]
        (vec (cons (resolve-fn (first edn))
                   (mapv prepare (next edn))))

        (symbol? edn)
        ; make fn (get-in message path)
        #(get-in % (prepare-path edn))

        :else
        ; make (fn [message] ...) returning constant
        (constantly edn)))


(defn evaluate [edn message]
  (if (vector? edn)
    ; evaluate [f & args]
    (apply (first edn)
           (mapv #(evaluate % message) (next edn)))
    ; evaluate (get-in message ...) or constant
    (edn message)))


#_(clojure.pprint/pprint
    (evaluate (prepare (edn/read-string "(and (= sender \"tbt\") (= payload.quantity 10))"))
              {:sender  "tbt"
               :payload {:quantity 10}}))
