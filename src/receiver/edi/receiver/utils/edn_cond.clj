(ns edi.receiver.utils.edn-cond
  (:require [clojure.edn :as edn]
            [clojure.string :as string]))


(def ^:private fns {"and"  (fn [& args] (every? identity args))
                    "or"   (fn [& args] (or (some identity args) false))
                    "not"  not
                    ">"    >
                    ">="   >=
                    "<"    <
                    "<="   <=
                    "="    =
                    "not=" not=})


(defn- resolve-fn [symbol]
  (or (get fns (name symbol))
      (throw (ex-info (format "unknown function: %s\nknown functions are: %s"
                              (name symbol)
                              (->> fns keys sort (string/join " "))) {}))))


(defn- prepare-edn [edn]
  (cond (list? edn)
        ; make vector [f & args]
        (vec (cons (resolve-fn (first edn))
                   (mapv prepare-edn (next edn))))

        (symbol? edn)
        ; make fn (get-in message path)
        (let [path (->> (string/split (name edn) #"\.")
                        (mapv keyword))]
          #(get-in % path))

        :else
        ; make (fn [message] ...) returning constant
        (constantly edn)))


(defn prepare [text]
  (-> text
      edn/read-string
      prepare-edn))


(defn evaluate [edn message]
  (if (vector? edn)
    ; evaluate [f & args]
    (apply (first edn)
           (mapv #(evaluate % message) (next edn)))
    ; evaluate (get-in message ...) or constant
    (edn message)))


#_(clojure.pprint/pprint
    (evaluate (prepare "(and (= sender \"tbt\") (= payload.quantity 10))")
              {:sender  "tbt"
               :payload {:quantity 10}}))
