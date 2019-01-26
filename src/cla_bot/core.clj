(ns cla-bot.core
  (:gen-class
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler])
  (:require [cheshire.core :as json]
            [cla-bot
             [github :as github]
             [google :as google]
             [logs :as logs]]
            [clojure.java.io :as io])
  (:import com.amazonaws.services.lambda.runtime.Context
           [java.io InputStream OutputStream]))

(defn update-status! [pr-num]
  (logs/log "Updating status of PR" pr-num)
  (github/update-pr-checks! pr-num (google/cla-usernames))
  :done)

(defn -handler ^String [args]
  (with-out-str
    (logs/log (for [arg args]
               [(class arg) arg]))))

#_(defn -main []
  (logs/log "HELLO WORLD!"))

;; TODO - it seems a lot of the stuff actually comes in in the paylod
#_(defn- body->config [{{installation-id :id} :installation
                      {repo :full_name}     :repository}]
  {:github-installation-id installation-id
   :github-repo            repo})

(defn handle-event [{:keys [body]}]
  #_(logs/log "EVENT ::")
  #_(pprint/pprint event)
  (logs/with-logs
    (try
      (let [body   (json/parse-string body keyword)
            pr-num (-> body :pull_request :number)]
        (when pr-num
          (update-status! pr-num))
        {:statusCode 200
         :headers    {:Content-Type "application/json"}
         :body       (json/generate-string
                      {:updated-pr pr-num
                       :logs       (logs/logs)})})
      (catch Throwable e
        (logs/log e)
        {:statusCode 500
         :headers    {:Content-Type "application/json"}
         :body       (json/generate-string
                      {:exception  (str (class e))
                       :message    (.getMessage e)
                       :logs       (logs/logs)
                       :stacktrace (for [frame (.getStackTrace e)]
                                     (str frame))})}))))

(defn -handleRequest [this, ^InputStream input-stream, ^OutputStream output-stream, ^Context context]
  #_(logs/log "CONTEXT ::")
  #_(pprint/pprint context)
  (with-open [reader (io/reader input-stream)]
    (let [event    (json/parse-stream reader keyword)
          response (handle-event event)]
      (with-open [writer (json/generate-stream response (io/writer output-stream))]
        (.flush writer))))
  (logs/log "DONE!"))
