(ns cla-bot.util
  (:require [cheshire.core :as json])
  (:import java.text.SimpleDateFormat
           [java.util Calendar TimeZone Date]))

(defn- utc-calendar ^Calendar []
  (Calendar/getInstance (TimeZone/getTimeZone "UTC")))

(defn- iso-8601-formatter ^SimpleDateFormat []
  (doto (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")
    (.setCalendar (utc-calendar))))

(defn now-utc ^Date []
  (.getTime (utc-calendar)))

(defn now-iso-8601 ^String []
  (.format (iso-8601-formatter) (now-utc)))

(defn parse-body [response]
  (update response :body #(json/parse-string % keyword)))

(defn do-auto-retry [f on-retry]
  (try
    (f)
    (catch Throwable e
      (when-not on-retry
        (throw e))
      (on-retry)
      (do-auto-retry f nil))))

(defmacro auto-retry [do on-retry]
  `(do-auto-retry (fn [] ~do) (fn [] ~on-retry)))
