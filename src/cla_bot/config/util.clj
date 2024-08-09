(ns cla-bot.config.util
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [cla-bot.config.aws :refer [fetch-secret]])
  (:import java.text.NumberFormat))

;; Overcomplicated! But I wanted to experiment with some ideas I had that might make it into Metabase core.

;;; ---------------------------------------------------- Sources -----------------------------------------------------

;; TODO - the main problem here is that by the time *sources* is defined

(defonce ^:dynamic ^{:doc "List of sources to look for (in order) for configuration values. You can change this value
  to change where config values may be resolved from; for example, if you define a new source, you can add it to the
    list of places to check here."}
    *sources*
    (atom [:env :secretsmanager]))


(defmulti parse
  "Parse a string value `v` as `type`. Add support for new types by adding more implementations of this method."
  {:arglists '([class v])}
  (fn [klass _]
    klass))

(defmethod parse :default [klass _]
  (throw (IllegalArgumentException. (str "Don't know how to parse " klass))))

(defmethod parse nil     [_ v] v) ; same as String
(defmethod parse String  [_ v] v)
(defmethod parse Number  [_ v] (some->> v (.parse (NumberFormat/getInstance))))
(defmethod parse Boolean [_ v] (some-> v Boolean/parseBoolean))


(defmulti value
  "Attempt to fetch config value of `type` named by keyword `k` from a `source` (such as `:env`), returning `not-found`
  if not found.

    (value :env :github-repo :string nil) ;-> \"metabase/metabase\" (from env var GITHUB_REPO)

  By default, the only source is `:env`. You can add more sources for configuration values by adding new
  implementations of this multimethod; you can change the sources currently in use with `set-sources!`.

  Default supported classes are `String` (the default), Number, and Boolean; add support for additional types by
  adding additional implementations of `parse`. If your source returns all values as strings (e.g. environment
  variables), you can call `parse` to convert the string values into the appropriate type. For sources that support
  different types, you can usually ignore the `class` argument.

  You shouldn't call this method directly; instead use `config` or the `defconfig` macro."
  {:arglists '([source k klass not-found])}
  (fn [source & _]
    source))

(defmethod value :default [source _ _ _]
  (throw (IllegalArgumentException. (str "No such configuration source: " source))))


(defn- config-env-var
  "Convert keyword to env var name."
  [k]
  (str/upper-case (munge (name k))))

;; Fetch value from environment variable -- the only default source
(defmethod value :env [_ k klass not-found]
  (if-let [v (System/getenv (config-env-var k))]
    (parse klass v)
    not-found))

;; Fetch value from AWS Secrets Manager
(defmethod value :secretsmanager [_ k klass not-found]
  (if-let [v (fetch-secret (config-env-var k))]
    (parse klass v)
    not-found))

;; Fetch value from any source in `*sources*` -- this implementation
(defmethod value :all [_ k klass not-found]
  (loop [[source & more] @*sources*]
    (let [v (value source k klass not-found)]
      (cond
        (not= v not-found)
        v

        (seq more)
        (recur more)

        :else
        not-found))))

(defn config
  "Fetch a config value."
  [k {:keys [default required?], klass :class}]
  (if-let [v (value :all k klass default)]
    v
    (when required?
      (throw (Exception. (format "Missing config value: %s (please set env var: %s)" k (config-env-var k)))))))

(s/def ::defconfig
  (s/or
   :no-doc (s/cat :k symbol?, :default   (s/? any?))
   :doc    (s/cat :k symbol?, :docstring (s/? string?), :default (s/? any?))))

(defmacro defconfig
  "Define a function that fetches config values of the same name. Metadata on `name` determines behavior -- `:required`
  makes the function throw an Exception when called if value is not set; a tag (e.g. `^Integer`) determines how values
  are parsed."
  {:arglists '([name] [name default] [name docstring default])}
  [& args]
  (let [[_ {:keys [k docstring default]}] (s/conform ::defconfig args)
        {:keys [required tag]}           (meta k)]
    `(def ~(vary-meta k assoc :doc docstring)
       (partial deref (delay (config ~(keyword k) {:required? ~required, :default ~default, :class ~tag}))))))
