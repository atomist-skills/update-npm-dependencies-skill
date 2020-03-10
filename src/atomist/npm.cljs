(ns atomist.npm
  (:require [clojure.string :as s]
            [atomist.sha :as sha]
            [atomist.json :as json]
            [atomist.api :as api]
            [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [goog.string :as gstring]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [cljs-node-io.proc :as proc]
            [cljs.core.async :refer [<!]]
            [atomist.deps :as deps])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn validate-dependency
  "dependency from a user should be a proper application/json map with string values"
  [handler]
  (fn [request]
    (if-let [dependency (:dependency request)]
      (try
        (let [d (json/->obj dependency :keywordize-keys false)]
          (if (and (->> (keys d) (every? string?))
                   (->> (vals d) (every? string?)))
            (handler request)
            (api/finish request :failure (gstring/format "%s is not a valid maven coordinate" dependency))))
        (catch :default ex
          (api/finish request :failure (gstring/format "%s is not a valid npm dependency formatted JSON doc" dependency))))
      (api/finish request :failure "this request requires a dependency to be configured"))))

(defn update-dependency
  "transform the dependencies parameter in a configuration from application/json to the edn format"
  [configuration]
  (letfn [(json->edn [s]
            (->> (try
                   (json/->obj s :keywordize-keys false)
                   (catch :default ex
                     (throw (ex-info "dependencies configuration was not valid JSON"
                                     {:policy "manualconfiguration"
                                      :message (gstring/format "bad JSON:  %s" s)}))))
                 (map (fn [[k v]] [(str k) (str v)]))
                 (into [])
                 (pr-str)))]
    (update configuration :parameters (fn [parameters]
                                        (->> parameters
                                             (map #(if (= "dependencies" (:name %))
                                                     (update % :value json->edn)
                                                     %)))))))

(defn validate-npm-policy
  "validate npm dependency configuration
    all configurations with a policy=manualConfiguration should have a dependency which is an application/json map
    all configurations with other policies use a dependency which is an array of strings"
  [handler]
  (fn [request]

    (try
      (let [configurations (->> (:configurations request)
                                (map #(if (= "manualConfiguration" (deps/policy-type %))
                                        (update-dependency %)
                                        %))
                                (map deps/validate-policy))]
        (if (->> configurations
                 (filter :error)
                 (empty?))
          (handler (assoc request :configurations configurations))
          (api/finish request :failure (->> configurations
                                            (map :error)
                                            (interpose ",")
                                            (apply str)))))
      (catch :default ex
        (log/error ex)
        (api/finish request :failure (-> (ex-data ex) :message))))))

(defn npm-update
  [project f n v]
  (go
   (let [baseDir (. ^js project -baseDir)]
     (io/spit f (s/replace
                 (io/slurp f)
                 (re-pattern (gstring/format "\"%s\":\\s*\"(.*)\"" n))
                 (gstring/format "\"%s\": \"%s\"" n v)))
     (let [[err stdout stderr] (<! (proc/aexec "npm install" {:cwd baseDir}))]
       (if (nil? err)
         :success
         (do
           (log/info stdout)
           (log/error stderr)
           :failure))))))

(defn- apply-library-editor
  "apply a library edit inside of a PR

    params
      project - the SDM project
      pr-opts - must conform to {:keys [branch target-branch title body]}
      library-name - leiningen library name string
      library-version - leiningen library version string

    returns channel"
  [project library-name library-version]
  (go
   (try
     (let [f (io/file (. ^js project -baseDir) "package.json")]
       (<! (npm-update project f library-name library-version)))
     :success
     (catch :default ex
       (log/error "failure updating project.clj for dependency change" ex)
       :failure))))

(defn extract [project]
  (let [f (io/file (. project -baseDir) "package.json")]
    (when (fs/fexists? (.getPath f))
      (let [json-data (js->clj (.parse js/JSON (io/slurp f)))]
        (for [[lib version] (merge
                             {}
                             (get json-data "dependencies")
                             (get json-data "devDependencies")) :let [data [lib version]]]
          {:type "npm-project-deps"
           :name (-> lib
                     (s/replace-all #"@" "")
                     (s/replace-all #"/" "::"))
           :abbreviation "npmdeps"
           :version "0.0.1"
           :data data
           :sha (sha/sha-256 (json/->str data))
           :displayName lib
           :displayValue (nth data 1)
           :displayType "NPM dependencies"})))))

(comment
 (extract #js {:baseDir "/Users/slim/atomist/atomist-skills/update-npm-dependencies-skill"}))