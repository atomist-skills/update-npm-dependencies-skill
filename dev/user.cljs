(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_STAGING))
#_(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_PROD))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(def team-id "APGF6BRLR")

(comment
 ;; this should fail with a bad JSON
  (atomist.main/handler #js {:data {:Push [{:branch "master"
                                            :repo {:name "express-test-0"
                                                   :org {:owner "atomisthqa"
                                                         :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                       :credential {:secret github-token}}}}
                                            :after {:message ""}}]}
                             :secrets [{:uri "atomist://api-key" :value token}]
                             :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                            {:name "dependencies" :value "{express \"4.1.2\"}"}]}]
                             :extensions {:team_id "AK748NQC5"}}
                        (fn [& args] (log/info "sendreponse " args)))

 ;; this should succeed
  (atomist.main/handler #js {:data {:Push [{:branch "master"
                                            :repo {:name "express-test-0"
                                                   :org {:owner "slimslender"
                                                         :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                       :credential {:secret github-token}}}}
                                            :after {:message ""}}]}
                             :secrets [{:uri "atomist://api-key" :value token}]
                             :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                            {:name "dependencies" :value "{\"express\": \"4.1.2\"}"}]}]
                             :extensions {:team_id team-id}}
                        (fn [& args] (log/info "sendreponse " args)))

 ;; running in a workspace linked to slimslender
  (atomist.main/handler #js {:data {:Push [{:branch "master"
                                            :repo {:name "npm1-test"
                                                   :org {:owner "slimslender"
                                                         :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                       :credential {:secret github-token}}}}
                                            :after {:message ""}}]}
                             :secrets [{:uri "atomist://api-key" :value token}]
                             :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                            {:name "dependencies" :value "{\"@slimslender/npm-test\": \"0.4.139\"}"}]}]
                             :extensions {:team_id team-id}}
                        (fn [& args] (log/info "sendreponse " args)))

  (atomist.main/handler #js {:command "UpdateNpmDependency"
                             :source {:slack {:channel {:id "CUCEERLBH"}
                                              :user {:id "UDF0NFB5M"}}}
                             :team {:id "AK748NQC5"}
                             :parameters [{:name "dependency"
                                           :value "{\"express\": \"4.1.2\"}"}]
                             :raw_message "npm update"
                             :secrets [{:uri "atomist://api-key" :value token}]}
                        (fn [& args] (log/info "sendreponse " args))))
