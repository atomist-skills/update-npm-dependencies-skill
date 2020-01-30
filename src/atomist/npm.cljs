(ns atomist.npm
  (:require [clojure.string :as s]
            [atomist.sha :as sha]
            [atomist.json :as json]
            [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [cljs-node-io.file :as file]
            [goog.string :as gstring]
            [goog.string.format]
            [atomist.cljs-log :as log]))

(defn- deconstruct-name [s]
  (if-let [[_ owner lib] (re-find #"^([^:]+)(::.*)?$" s)]
    (if (not lib)
      owner
      (gstring/format "@%s/%s" owner (s/replace lib "::" "")))))

(defn apply-dependency [& args])

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
           :data data})))))

(comment
 (extract #js {:baseDir "/Users/slim/atomist/atomist-skills/update-npm-dependencies-skill"}))