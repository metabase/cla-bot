(ns cla-bot.config.aws
    (:require [cheshire.core :as json])
  (:import (com.amazonaws.services.secretsmanager AWSSecretsManagerClientBuilder)
           (com.amazonaws.services.secretsmanager.model GetSecretValueRequest)))

(def secret-cache (atom nil))

(defn get-secret-value [secret-id]
  (let [client (AWSSecretsManagerClientBuilder/defaultClient)
        request (doto (GetSecretValueRequest.)
                  (.withSecretId secret-id))
        response (.getSecretValue client request)
        secret-string (.getSecretString response)]
    (json/parse-string secret-string true)))

(defn fetch-secret [key]
  (let [secret-name (System/getenv "SECRET_NAME")]
    (if secret-name
      (let [cached-secret @secret-cache]
        (if cached-secret
          (get cached-secret (keyword key))
          (let [secret (get-secret-value secret-name)]
            (reset! secret-cache secret)
            (get secret (keyword key)))))
      nil)))