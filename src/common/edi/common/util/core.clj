(ns edi.common.util.core
  (:import (java.nio ByteBuffer)
           (java.util UUID)))

(defn uuid->byte-array [^UUID v]
  (let [buffer (ByteBuffer/wrap (byte-array 16))]
    (doto buffer
      (.putLong (.getMostSignificantBits v))
      (.putLong (.getLeastSignificantBits v)))
    (.array buffer)))


