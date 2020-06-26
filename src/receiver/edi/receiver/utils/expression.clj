(ns edi.receiver.utils.expression
  (:require [clojure.string :as string]
            [edi.receiver.utils.sandbox]))


(defn- resolve-fn [symbol]
  (or (ns-resolve 'edi.receiver.utils.sandbox symbol)
      (throw (ex-info (format "unknown function: %s" (name symbol)) {}))))


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
    (evaluate (prepare (clojure.edn/read-string "(and (= sender \"tbt\") (= payload.quantity 10))"))
              {:sender  "tbt"
               :payload {:quantity 10}}))
