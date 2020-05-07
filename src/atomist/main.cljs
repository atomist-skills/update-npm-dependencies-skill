(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.npm :as npm]
            [atomist.deps :as deps]
            [atomist.config :as config]
            [goog.string.format])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn just-fingerprints
  [request]
  (go
    (try
      (npm/extract (:project request))
      (catch :default ex
        (log/error "unable to compute npm fingerprints")
        (log/error ex)
        {:error ex
         :message "unable to compute npm fingerprints"}))))

(def apply-policy (partial deps/apply-policy-targets {:type "npm-project-deps"
                                                      :apply-library-editor npm/apply-library-editor
                                                      :->library-version identity
                                                      :->data identity
                                                      :->sha npm/data->sha
                                                      :->name npm/library-name->name}))

(defn compute-fingerprints
  [request]
  (go
    (try
      (let [fingerprints (npm/extract (:project request))]
        ;; first create PRs for any off target deps
        (<! (apply-policy
             (assoc request :fingerprints fingerprints)))
        ;; return the fingerprints in a form that they can be added to the graph
        fingerprints)
      (catch :default ex
        (log/error "unable to compute npm fingerprints")
        (log/error ex)
        {:error ex
         :message "unable to compute npm fingerprints"}))))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (deps/deps-handler data
                     sendreponse
                     ["ShowNpmDependencies"]
                     ["SyncNpmDependency"]
                     ["UpdateNpmDependency"
                      (api/compose-middleware
                       [config/set-up-target-configuration]
                       [config/validate-dependency]
                       [api/check-required-parameters {:name "dependency"
                                                       :required true
                                                       :pattern ".*"
                                                       :validInput "{lib: version}"}]
                       [api/extract-cli-parameters [[nil "--dependency dependency" "{lib: version}"]]])]
                     just-fingerprints
                     compute-fingerprints
                     config/validate-npm-policy))
