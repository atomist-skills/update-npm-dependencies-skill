(ns atomist.main
  (:require [cljs.core.async :refer [<!]]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.npm :as npm]
            [atomist.deps :as deps]
            [atomist.config :as config])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (deps/deps-handler
   data
   sendreponse
   :deps-command/show "ShowNpmDependencies"
   :deps-command/sync "SyncNpmDependency"
   :deps-command/update "UpdateNpmDependency"
   :deps/type "npm-project-deps"
   :deps/apply-library-editor npm/apply-library-editor
   :deps/extract npm/extract
   :deps/->library-version identity
   :deps/->data identity
   :deps/->sha npm/data->sha
   :deps/->name npm/library-name->name
   :deps/validate-policy config/validate-npm-policy
   :deps/validate-command-parameters (api/compose-middleware
                                      [config/set-up-target-configuration]
                                      [config/validate-dependency]
                                      [api/check-required-parameters {:name "dependency"
                                                                      :required true
                                                                      :pattern ".*"
                                                                      :validInput "{lib: version}"}]
                                      [api/extract-cli-parameters [[nil "--dependency dependency" "{lib: version}"]]])))
