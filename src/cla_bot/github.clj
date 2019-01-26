(ns cla-bot.github
  (:require [cheshire.core :as json]
            [cla-bot
             [config :as config]
             [logs :as logs]
             [util :as u]]
            [cla-bot.github.jwt :as jwt]
            [clj-http.client :as client]
            [clojure
             [pprint :as pprint]
             [string :as str]]))

;;; ---------------------------------------- GitHub Application Access Tokens ----------------------------------------

;; See https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-an-installation

(defn new-access-token! []
  (-> (client/post (format "https://api.github.com/app/installations/%s/access_tokens" (config/github-installation-id))
                   {:headers {"Accept"        "application/vnd.github.machine-man-preview+json"
                              "Authorization" (format "Bearer %s" (jwt/jwt))}})
      u/parse-body
      :body
      :token))

(defonce ^:private access-token* (atom nil))

(defn- reset-access-token! []
  (reset! access-token* (new-access-token!)))

(defn- access-token []
  (when-not @access-token*
    (reset-access-token!))
  @access-token*)


;;; ---------------------------------------------------- Helpers -----------------------------------------------------

(defn- GET [endpoint]
  (-> (client/get (str (config/github-base-url) endpoint))
      u/parse-body))

(defn- POST {:style/indent 1} [endpoint request]
  (u/auto-retry
   (let [request (-> request
                     (update :body #(some-> % json/generate-string))
                     (assoc-in [:headers "Authorization"] (format "token %s" (access-token))))]
     (-> (client/post (str (config/github-base-url) endpoint) request)
         u/parse-body))
   (reset-access-token!)))


;;; ---------------------------------------------------- PR info -----------------------------------------------------

(defn- pr->commits [pr-num]
  (logs/log "Fetching commits for PR" pr-num)
  (-> (GET (format "pulls/%d/commits" pr-num))
      :body))

(defn- commits->usernames [commits]
  (set (for [commit commits]
         (-> commit :author :login))))


;;; --------------------------------------------- Updating Check Status ----------------------------------------------

;; See https://developer.github.com/v3/checks/runs/#create-a-check-run

(defn- POST-check-runs [body]
  (POST "check-runs"
    {:headers {"Accept" "application/vnd.github.antiope-preview+json"}
     :body    (merge
               {:name         "Cam's Next-Level CLA Bot"
                :status       "completed"
                :completed_at (u/now-iso-8601)}
               body)}))

(defn- update-check-status! [& {:keys [sha username signed? whitelisted?]}]
  (let [success? (or signed? whitelisted?)]
    (POST-check-runs
     {:head_sha   sha
      :conclusion (if success? "success" "failure")
      :output     {:title   (str username " " (cond
                                                whitelisted? "does not need to sign the CLA"
                                                signed?      "has signed the CLA"
                                                :else        "has not signed the CLA"))
                   :summary (if success?
                              "Thanks for your contribution!"
                              "Please sign the [Contributor License Agreement](https://docs.google.com/a/metabase.com/forms/d/1oV38o7b9ONFSwuzwmERRMi9SYrhYeOrkbmNaq9pOJ_E/viewform).")}})))

(defn- format-stacktrace [^Throwable e]
  (format "### %s\n\n```\n%s\n```"
          (.getMessage e)
          (with-out-str (pprint/pprint (vec (.getStackTrace e))))))

(defn- update-check-status-with-error-message!
  ([sha, ^Throwable e]
   (let [{:keys [title message]
          :or   {title   "Could not update CLA signing status"
                 message (format-stacktrace e)}} (ex-data e)]
     (update-check-status-with-error-message! sha title message)))
  ([sha title message]
   (logs/log "Error!" title "\n" message)
   (POST-check-runs
    {:head_sha   sha
     :conclusion "failure"
     :output     {:title title, :summary message}})))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                            Putting it all together!                                            |
;;; +----------------------------------------------------------------------------------------------------------------+

(defn- whitelisted-users []
  (when-let [whitelist (config/github-core-contributors-whitelist)]
    (set (for [username (str/split whitelist #",")]
           (str/lower-case username)))))

(defn- check-no-nil-usernames [usernames]
  (when (some nil? usernames)
    (throw (ex-info "Missing usernames"
             {:title   "Some commits are missing usernames"
              :message (str "One or more commits in this PR were made by someone who is not a GitHub user. "
                            "We cannot determine whether they have signed the CLA. If you are the PR author, "
                            "please amend the commits and change the author to one associated with a GitHub account. "
                            "For members of the Metabase core team, consider manually verifying CLA status instead.")}))))

(defn update-pr-checks! [pr-num cla-usernames]
  (logs/log "Sending checks statuses to GitHub for PR" pr-num)
  (let [commits                (pr->commits pr-num)
        usernames              (commits->usernames commits)
        {last-commit-sha :sha} (last commits)]
    (try
      (logs/log "Most recent commit SHA:" last-commit-sha)
      (logs/log "PR usernames:" usernames) ; NOCOMMIT
      (check-no-nil-usernames usernames)
      (doseq [username usernames
              :let     [lowercased-username (str/lower-case username)
                        whitelisted?        (contains? (whitelisted-users) lowercased-username)
                        signed?             (contains? cla-usernames       lowercased-username)]]
        (logs/log "username:" username "whitelisted?" whitelisted? "signed?" signed?)
        (update-check-status!
         :sha          last-commit-sha
         :username     username
         :whitelisted? whitelisted?
         :signed?      signed?))
      (catch Throwable e
        (update-check-status-with-error-message! last-commit-sha e)))))
