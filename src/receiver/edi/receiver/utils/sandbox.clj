(ns edi.receiver.utils.sandbox
  (:refer-clojure :only [< <= = == > >= not= not identical?
                         + - * / quot rem mod inc dec max min
                         true? false? nil? some?
                         zero? pos? neg? even? odd?
                         re-matches re-find
                         count subs
                         str format])
  (:require [clojure.string :refer [capitalize lower-case upper-case
                                    trim trim-newline triml trimr
                                    blank? starts-with? ends-with? includes?
                                    replace replace-first index-of last-index-of]]
            [edi.receiver.utils.coerce :refer :all]
            [edi.receiver.utils.sandbox-overrides :refer :all]))
