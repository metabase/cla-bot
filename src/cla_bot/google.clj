(ns cla-bot.google
  (:require [cheshire.core :as json]
            [cla-bot
             [config :as config]
             [logs :as logs]
             [util :as u]]
            [clj-http.client :as client]
            [clojure.string :as str]))

(defn- GET [url]
  (-> (client/get url {:headers {"Accept" "application/json"}})
      u/parse-body))

(defn- POST [url request]
  (let [request (-> request
                    (assoc-in [:headers "Accept"] "application/json")
                    (assoc-in [:headers "Content-Type"] "application/json")
                    (update :body json/generate-string))]
    (-> (client/post url request)
        u/parse-body)))

;;; -------------------------------------------- Google API Access Tokens --------------------------------------------

(defonce access-token (atom ""))

(defn- new-access-token []
  (logs/log "Refreshing Google API access token")
  (-> (POST "https://www.googleapis.com/oauth2/v4/token"
            {:body {:client_id     (config/google-client-id)
                    :client_secret (config/google-client-secret)
                    :refresh_token (config/google-refresh-token)
                    :grant_type    :refresh_token}})
      :body
      :access_token))

(defn- refresh-access-token! []
  (let [new-token (new-access-token)]
    (logs/log "new token:" new-token)
    (reset! access-token new-token)))

;;; ----------------------------------------------- Spreadsheet Values -----------------------------------------------

(defn- spreadsheet-values-url [spreadsheet-id range-expression]
  (format
   "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s?access_token=%s"
   spreadsheet-id
   range-expression
   @access-token))

(defn- spreadsheet-values
  ([]
   (spreadsheet-values (config/google-spreadsheet-id) (config/google-sheet-range)))
  ([spreadsheet-id range-expression]
   (logs/log "Fetching values from CLA spreadsheet" spreadsheet-id "range expression:" range-expression)
   (u/auto-retry
    (-> (GET (spreadsheet-values-url spreadsheet-id range-expression))
        :body
        :values)
    (refresh-access-token!))))

;;; -------------------------------------------- Putting it all together ---------------------------------------------


(defn- format-username [value]
  (-> (str/trim value)
      str/lower-case
      (str/replace #"^@" "")
      (str/replace #".*github\.com/" "")))

(defn cla-usernames []
  (logs/log "Fetching CLA signers...")
  (set
   (for [[value] (spreadsheet-values)
         :when   (some? value)]
     (format-username value))))
