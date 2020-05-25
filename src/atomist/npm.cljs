(ns atomist.npm
  (:require [clojure.string :as s]
            [atomist.sha :as sha]
            [atomist.json :as json]
            [cljs-node-io.core :as io]
            [goog.string :as gstring]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [cljs-node-io.proc :as proc]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn library-name->name [s]
  (-> s
      (s/replace #"@" "")
      (s/replace #"/" "::")))

(defn data->sha [data]
  (sha/sha-256 (json/->str data)))

(defn npm-update
  [project f n v]
  (go
    (let [baseDir (:path project)]
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

(defn apply-library-editor
  "apply a library edit inside of a PR

    params
      project - the SDM project
      pr-opts - must conform to {:keys [branch target-branch title body]}
      library-name - leiningen library name string
      library-version - leiningen library version string

    returns channel"
  [project target-fingerprint]
  (go
    (try
      (let [f (io/file (:path project) "package.json")
            [library-name library-version] (:data target-fingerprint)]
        (<! (npm-update project f library-name library-version)))
      :success
      (catch :default ex
        (log/error "failure updating project.clj for dependency change" ex)
        :failure))))

(defn extract [project]
  (go
    (let [f (io/file (:path project) "package.json")]
      (when (.exists f)
        (let [json-data (js->clj (.parse js/JSON (io/slurp f)))]
          (for [[lib version] (merge
                               {}
                               (get json-data "dependencies")
                               (get json-data "devDependencies")) :let [data [lib version]]]
            {:type "npm-project-deps"
             :name (library-name->name lib)
             :abbreviation "npmdeps"
             :version "0.0.1"
             :data data
             :sha (data->sha data)
             :displayName lib
             :displayValue (nth data 1)
             :displayType "NPM dependencies"}))))))

(comment
  (extract #js {:baseDir "/Users/slim/atomist/atomist-skills/update-npm-dependencies-skill"}))