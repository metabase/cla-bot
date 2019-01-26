(ns cla-bot.github.jwt
  (:require [buddy.sign.jwt :as jwt]
            [cla-bot
             [config :as config]
             [util :as u]]))

(defn- jwt-payload []
  (let [now-seconds (int (/ (.getTime (u/now-utc)) 1000))]
    {:iat now-seconds
     :exp (+ now-seconds (* 10 60))
     :iss (config/github-app-id)}))

(defn jwt []
  (jwt/sign (jwt-payload) (config/github-secret) {:alg :rs256}))
