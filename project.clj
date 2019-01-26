(defproject cla-bot "1.0.0"
  :dependencies
  [[org.clojure/clojure "1.10.0"]
   [buddy/buddy-sign "3.0.0"]           ; for JWTs
   [cheshire "5.8.1"]                   ; JSON encoding
   [clj-http "3.9.1"]                   ; HTTP client
   [com.amazonaws/aws-lambda-java-core "1.0.0"]
   #_[http-kit "2.3.0"]]

  :main ^:skip-aot cla-bot.core

  :profiles
  {:uberjar
   {:auto-clean true
    :aot :all}})
