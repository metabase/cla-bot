(ns cla-bot.logs
  (:require [clojure.string :as str]))

(def ^:dynamic *logs* nil)

(defn log [& args]
  (some->
   *logs*
   (swap! conj (str/join " " args))))

(defmacro with-logs [& body]
  `(binding [*logs* (atom [])]
     ~@body))

(defn logs []
  (some-> *logs* deref))
