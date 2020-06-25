(ns edi.receiver.backend.javamail
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [edi.receiver.backend.protocol :as protocol])
  (:import (java.net PasswordAuthentication)
           (java.util Properties)
           (javax.mail Authenticator Message$RecipientType Session Transport)
           (javax.mail.internet InternetAddress MimeMessage)))


(defn- session [{:keys [host port username password starttsl]}]
  (Session/getInstance
    (doto (Properties.)
      (.putAll (cond-> {"mail.smtp.host" host
                        "mail.smtp.port" (or port 25)}
                       username
                       (assoc
                         "mail.smtp.auth" true
                         "mail.smtp.user" username
                         "mail.smtp.starttls.enable" (boolean starttsl)))))
    (when username
      (proxy [Authenticator] []
        (getPasswordAuthentication []
          (PasswordAuthentication. username password))))))


(defn- send! [^Session session {:keys [from to subject text]}]
  (Transport/send (doto (MimeMessage. session)
                    (.setText text)
                    (.setSubject subject)
                    (.setFrom (InternetAddress. from))
                    (.setRecipients (Message$RecipientType/TO)
                                    (InternetAddress/parse to)))))


(defn- render [topic message]
  (format "Topic: %s\nMessage:\n%s" topic (json/encode message {:pretty true})))


(deftype JavaMailBackend [config session]

  protocol/Backend

  (send-message [_ topic message]
    (let [{:keys [from to subject]} config]
      (log/debugf "proxying message to %s, topic %s" to topic)
      (send! session (-> config
                         (select-keys [:from :to :subject])
                         (assoc :text (render topic message))))))

  (close [_]))


(defn create [config]
  (log/info "Initializing java mail backend")
  (JavaMailBackend. config (session config)))
