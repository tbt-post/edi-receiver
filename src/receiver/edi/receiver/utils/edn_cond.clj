(ns edi.receiver.utils.edn-cond
  (:require [clojure.edn :as edn]
            [clojure.string :as string]))


(defn- resolve-fn [symbol]
  (case (name symbol)
    ; replace macros to work as functions
    "and" (fn [& args] (every? identity args))
    "or" (fn [& args] (or (some identity args) false))
    ; TODO: maybe restrict resolve?
    (resolve symbol)))


(defn- compile-edn [edn]
  (cond (list? edn)
        ; make vector [f & args]
        (vec (cons (resolve-fn (first edn))
                   (mapv compile-edn (next edn))))

        (symbol? edn)
        ; make fn (get-in message path)
        (let [path (->> (string/split (name edn) #"\.")
                        (mapv keyword))]
          #(get-in % path))

        :else
        ; make (fn [message] ...) returning constant
        (constantly edn)))


(defn compile [text]
  (-> text
      edn/read-string
      compile-edn))


(defn evaluate [edn message]
  (if (vector? edn)
    ; evaluate [f & args]
    (apply (first edn)
           (mapv #(evaluate % message) (next edn)))
    ; evaluate (get-in message ...) or constant
    (edn message)))


#_(clojure.pprint/pprint
    (evaluate (compile "(and (= sender \"tbt\") (= payload.quantity 10))")
              {:sender  "tbt"
               :payload {:quantity 10}}))
