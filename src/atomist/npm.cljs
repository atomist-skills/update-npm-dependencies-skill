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

(declare apply-library-editor)

(defn apply-dependencies [{:keys [project configurations fingerprints] :as request}]
  (go
   (let [targets (target-map configurations)]
     (doseq [{current-data :data :as fingerprint} fingerprints]
       (doseq [{target-data :data :as target} targets]
         (when (off-target? fingerprint target)
           (let [body (gstring/format "off-target clojure-project-deps %s/%s -> %s/%s"
                                      (nth current-data 0) (nth current-data 1)
                                      (nth target-data 0) (nth target-data 1))]
             (log/info body)
             (<! (apply-library-editor project
                                       {:branch (-> request :ref :branch)
                                        :target-branch (:library target)
                                        :title (gstring/format "%s:  update npm dependencies skill requesting change" (:library target))
                                        :body body}
                                       (nth target-data 0)
                                       (nth target-data 1)))))))
     :complete)))

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