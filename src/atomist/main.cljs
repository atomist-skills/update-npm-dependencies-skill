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

(defn set-up-target-configuration [handler]
  (fn [request]
    (log/infof "set up target dependency to converge on %s" (:dependency request))
    (let [d (json/->obj (:dependency request) :keywordize-keys false)
          dependencies (gstring/format "[%s %s]" (-> d keys first) (-> d vals first))]
      (log/info "use dependency " dependencies)
      (handler (assoc request
                 :configurations [{:parameters [{:name "policy"
                                                 :value "manualConfiguration"}
                                                {:name "dependencies"
                                                 :value dependencies}]}])))))

(defn check-for-targets-to-apply [handler]
  (fn [request]
    (if (not (empty? (-> request :data :CommitFingerprintImpact :offTarget)))
      (handler request)
      (api/finish request))))

(defn- handle-push-event [request]
  ((-> (api/finished :message "handling Push" :success "Successfully handled Push")
       (api/send-fingerprints)
       (api/run-sdm-project-callback compute-fingerprints)
       (npm/validate-npm-policy)
       (api/extract-github-token)
       (api/create-ref-from-push-event)) request))

(defn- handle-impact-event [request]
  ((-> (api/finished :message "handling CommitFingerprintImpact" :success "Successfully handled CommitFingerprintImpact")
       (api/extract-github-token)
       (api/create-ref-from-repo
        (-> request :data :CommitFingerprintImpact :repo)
        (-> request :data :CommitFingerprintImpact :branch))
       (check-for-targets-to-apply)) request))

(defn command-handler [request]
  ((-> (api/finished :message "handling CommandHandler" :success "Command Handler invoked")
       (api/show-results-in-slack :result-type "fingerprints")
       (api/run-sdm-project-callback just-fingerprints)
       (api/create-ref-from-first-linked-repo)
       (api/extract-linked-repos)
       (api/extract-github-user-token)
       (api/set-message-id)) (assoc request :branch "master")))

(defn update-command-handler [request]
  ((-> (api/finished :message "handling application CommandHandler")
       (api/show-results-in-slack :result-type "fingerprints")
       (api/run-sdm-project-callback compute-fingerprints)
       (set-up-target-configuration)
       (api/create-ref-from-first-linked-repo)
       (api/extract-linked-repos)
       (api/extract-github-user-token)
       (npm/validate-dependency)
       (api/check-required-parameters {:name "dependency"
                                       :required true
                                       :pattern ".*"
                                       :validInput "lib version"})
       (api/extract-cli-parameters [[nil "--dependency dependency" "[lib version]"]])
       (api/set-message-id)) (assoc request :branch "master")))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (api/make-request
   data
   sendreponse
   (fn [request]
     (cond
       ;; handle Push events
       (contains? (:data request) :Push)
       (handle-push-event request)
       ;; handle Commit Fingeprint Impact events
       (= :CommitFingerprintImpact (:data request))
       (handle-impact-event request)

       (= "ShowNpmDependencies" (:command request))
       (command-handler request)

       (= "UpdateNpmDependency" (:command request))
       (update-command-handler request)

       :else
       (api/finish request :failure "did not recognize this event")))))
