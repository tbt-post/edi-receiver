(ns edi.receiver.utils.jetty-client
  (:import (clojure.lang Keyword)
           (java.util Base64)
           (java.util.concurrent TimeUnit)
           (org.eclipse.jetty.client HttpClient)
           (org.eclipse.jetty.client.api Request ContentResponse)
           (org.eclipse.jetty.client.util StringContentProvider)
           (org.eclipse.jetty.util.ssl SslContextFactory$Client)))


(defn base64-encode [s]
  (.encodeToString (Base64/getEncoder) (.getBytes s)))


(defn ^HttpClient client []
  (let [^HttpClient client (HttpClient. (SslContextFactory$Client.))]
    (.start client)
    client))


(defn- set-query-params [^Request request params]
  (doseq [[^String k ^String v] params]
    (.param request k v))
  request)


(defn- set-headers [^Request request headers]
  (doseq [[^String k ^String v] headers]
    (.header request k v))
  request)


(defn- set-auth [headers {:keys [type username password]}]
  (case type
    "basic" (assoc headers "Authorization" (str "Basic " (base64-encode (str username ":" password))))
    identity))


(defn request [^HttpClient client {:keys [^String uri
                                               ^Keyword method
                                               query-params
                                               headers
                                               auth
                                               ^String body
                                               ^long timeout
                                               ^boolean throw-for-status]}]
  (let [^ContentResponse
        response (-> (cond-> ^Request (.newRequest client uri)
                             method (.method (name method))
                             timeout (.timeout timeout TimeUnit/MILLISECONDS)
                             query-params (set-query-params query-params)
                             headers (set-headers (cond-> headers
                                                          (:enabled auth) (set-auth auth)))
                             body (.content (StringContentProvider. body)))
                     .send)
        status   (-> response .getStatus)
        result   {:status status
                  #_:headers #_(->> response .getHeaders
                                    (map (fn [^HttpField h] [(.getName h) (.getValue h)]))
                                    (into {}))
                  :body   (-> response .getContentAsString)
                  :reason (-> response .getReason)}]
    (if (and throw-for-status (>= status 300))
      (throw (ex-info (format "Http status %s" status) result))
      result)))
