(ns atomist.config
  (:require [goog.string :as gstring]
            [atomist.deps :as deps]
            [atomist.api :as api]
            [atomist.cljs-log :as log]
            [atomist.json :as json]))

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
