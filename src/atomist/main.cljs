(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.npm :as npm]
            [atomist.deps :as deps]
            [atomist.json :as json]
            [goog.string :as gstring]
            [goog.string.format])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn just-fingerprints
  [request project]
  (go
    (try
      (npm/extract project)
      (catch :default ex
        (log/error "unable to compute npm fingerprints")
        (log/error ex)
        {:error ex
         :message "unable to compute npm fingerprints"}))))

(defn compute-fingerprints
  [request project]
  (go
   (try
     (let [fingerprints (npm/extract project)]
       ;; first create PRs for any off target deps
       (<! (deps/apply-policy-targets
            (assoc request :project project :fingerprints fingerprints)
            "npm-project-deps"
            npm/apply-library-editor))
       ;; return the fingerprints in a form that they can be added to the graph
       fingerprints)
     (catch :default ex
       (log/error "unable to compute npm fingerprints")
       (log/error ex)
       {:error ex
        :message "unable to compute npm fingerprints"}))))

(defn set-up-target-configuration
  ""
  [handler]
  (fn [request]
    (log/infof "set up target dependency to converge on %s" (:dependency request))
    (try
      (let [d (json/->obj (:dependency request) :keywordize-keys false)
            dependencies (gstring/format "[[\"%s\" \"%s\"]]" (-> d keys first) (-> d vals first))]
        (log/info "use dependency " dependencies)
        (handler (assoc request
                   :configurations [{:parameters [{:name "policy"
                                                   :value "manualConfiguration"}
                                                  {:name "dependencies"
                                                   :value dependencies}]}])))
      (catch :default ex
        (api/finish request :failure (gstring/format "%s was not a valid target dependency" (:dependency request)))))))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (deps/deps-handler data sendreponse
                     ["ShowNpmDependencies" just-fingerprints]
                     ["UpdateNpmDependency" compute-fingerprints
                      (api/compose-middleware
                       [set-up-target-configuration]
                       [npm/validate-dependency]
                       [api/check-required-parameters {:name "dependency"
                                                       :required true
                                                       :pattern ".*"
                                                       :validInput "{lib: version}"}]
                       [api/extract-cli-parameters [[nil "--dependency dependency" "{lib: version}"]]])]
                     npm/validate-npm-policy))
