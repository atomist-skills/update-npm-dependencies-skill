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
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn validate-dependency [handler]
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

(defn validate-npm-policy [handler]
  (fn [request]
    (handler request)))

(defn- deconstruct-name [s]
  (if-let [[_ owner lib] (re-find #"^([^:]+)(::.*)?$" s)]
    (if (not lib)
      owner
      (gstring/format "@%s/%s" owner (s/replace lib "::" "")))))

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
        (npm-update project f library-name library-version))
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