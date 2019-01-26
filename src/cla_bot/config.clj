(ns cla-bot.config
  (:require [buddy.core.keys :as keys]
            [cla-bot.config.util :as config.u :refer [defconfig]]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io Reader StringReader]))

;;; ----------------------------------------------------- Google -----------------------------------------------------

(defconfig ^:required google-client-id)
(defconfig ^:required google-client-secret)
(defconfig ^:required google-refresh-token)
(defconfig ^:required google-spreadsheet-id)
(defconfig ^:required google-sheet-range)

;;; ----------------------------------------------------- GitHub -----------------------------------------------------

(defconfig ^Integer github-app-id 23642)

(defconfig ^:required ^Integer github-installation-id
  "ID of the installation of this GitHub application (installation means this app being 'installed' into a certain repo)"
  nil)

(defconfig ^:required ^:private github-repo)

(defn github-base-url []
  (format "https://api.github.com/repos/%s/" (github-repo)))

(defconfig ^:private github-private-key-filename)
(defconfig ^:private github-private-key)

(defn- fix-private-key-newlines
  "Saving contents of a PEM file as an env var stomps on very important™ newlines so put things back the way they should
  be."
  ^String [private-key]
  (-> private-key
      (str/replace #"\s+" "\n")
      (str/replace #"BEGIN\sRSA\sPRIVATE\sKEY" "BEGIN RSA PRIVATE KEY")
      (str/replace #"END\sRSA\sPRIVATE\sKEY" "END RSA PRIVATE KEY")))

(defn github-secret []
  (with-open [^Reader reader (or (some-> (github-private-key-filename) io/reader)
                                 (some-> (github-private-key) fix-private-key-newlines StringReader.)
                                 (throw (Exception. "You must set either GITHUB_PRIVATE_KEY_FILENAME or GITHUB_PRIVATE_KEY.")))]
    (keys/private-key reader)))

(defconfig github-core-contributors-whitelist)
