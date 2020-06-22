(ns edi.receiver.utils.transform
  (:require [clojure.edn :as edn]))


#_[
   :input-key                                               ; path-trough key
   [:input-key :output-key]                                 ; path-trough, rename key

   [nil :output-key nil "output value"]                     ; inject constant pair

   [:input-key :output-key "input-value" "output-value"]]   ; inject if input value matches


(defn- g [d key]
  (get d key))


(defn- transform-item [d rule]
  (let [rule    (if (keyword? rule) [rule rule] rule)
        [in-key out-key in-value out-value] rule
        out-key (or out-key in-key)]

    (cond
      (and in-key out-key)
      ; translate
      (when (or (nil? in-value)
                (= in-value (g d in-key)))
        [out-key (if (nil? out-value)
                   (g d in-key)
                   out-value)])

      out-key
      [out-key out-value]

      :else
      (do
        (print "unknown case" rule)
        nil))))


(defn transform [rules d]
  (let [rules (if (string? rules)
                (edn/read-string rules)
                rules)]
    (->> rules
         (map (partial transform-item d))
         (into {}))))


#_(prn "-----------------------------------")
#_(clojure.pprint/pprint (transform [:a
                                     [:b :bb]
                                     [:c :cc 123 456]
                                     [:c :cc 3 333]
                                     [nil :new nil 77]]
                                    {:a 1
                                     :b 2
                                     :c 3}))
