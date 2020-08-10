(ns edi.receiver.utils.transform
  (:require [edi.receiver.utils.expression :as expression]
            [medley.core :as medley]))


#_[:restrictive
   [aa (str (+ aa 10)) (> aa 10)]
   [bb bb]
   [cc cc (some? cc)]]

; wide by default
#_[[aa (str (+ aa 10)) (> aa 10)]
   [aa :dissoc (<= aa 10)]
   [cc :dissoc (nil? cc)]]

#_[[payload.quantity
    (str (+ payload.quantity 10))
    (and (= sender "tbt") (= payload.quantity 10))]]


(defn- prepare-rule [edn]
  (let [[path expression condition] edn]
    [(expression/prepare-path path)
     (cond-> expression
             (not (keyword? expression)) expression/prepare)
     (when (some? condition)
       (expression/prepare condition))]))


(defn prepare [rules]
  (let [modifiers (set (filter keyword? rules))]
    {:restrictive? (boolean (modifiers :restrictive))
     :not-strip?   (not (modifiers :strip))
     :rules        (map prepare-rule (remove keyword? rules))}))


(defn transform [{:keys [restrictive? not-strip? rules]} source]
  (let [target (atom (if restrictive? {} source))]
    (doseq [[path expression condition] rules]
      (when (or (nil? condition)
                (expression/evaluate condition source))
        (if (= :dissoc expression)
          (swap! target #(medley/dissoc-in % path))
          (let [value (expression/evaluate expression source)]
            (when (or not-strip? (some? value))
              (swap! target #(assoc-in % path value)))))))
    @target))


#_(prn "-----------------------------------")
#_(-> (str
        "[:restrictive"
        " [a (str (+ aa 10)) (> aa 10)]"
        " [b bb]"
        " [c cc (some? cc)]]")
      clojure.edn/read-string
      prepare
      (transform {:aa 11 :bb 2 :cc 3})
      clojure.pprint/pprint)
#_(-> (str
        "[[a (str (+ aa 10)) (> aa 10)]"
        " [b bb]"
        " [c cc (some? cc)]"
        " [aa :dissoc]"
        " [bb :dissoc]"
        " [cc :dissoc]]")
      edn/read-string
      prepare
      (transform {:aa 11 :bb 2 :cc 3})
      clojure.pprint/pprint)
