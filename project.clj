(defproject cla-bot "1.0.0"
  :dependencies
  [[org.clojure/clojure "1.10.0"]
   [buddy/buddy-sign "3.0.0"]           ; for JWTs
   [cheshire "5.8.1"]                   ; JSON encoding
   [clj-http "3.9.1"]                   ; HTTP client
   [com.amazonaws/aws-lambda-java-core "1.0.0"]
   [com.amazonaws/aws-java-sdk-secretsmanager "1.12.118"]
   [com.fasterxml.jackson.core/jackson-core "2.12.3"]
   [com.fasterxml.jackson.core/jackson-databind "2.12.3"]
   [com.fasterxml.jackson.core/jackson-annotations "2.12.3"]
   #_[http-kit "2.3.0"]]

  :main ^:skip-aot cla-bot.core

  :profiles
  {:uberjar
   {:auto-clean true
    :aot :all}})
