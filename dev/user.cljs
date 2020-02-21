(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_STAGING))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(println (.. js/process -env -GRAPHQL_ENDPOINT))

(comment
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "express-test-0"
                                                  :org {:owner "atomisthqa"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                           {:name "dependencies" :value "[[\"express\" \"4.1.2\"]]"}]}]
                            :extensions {:team_id "AK748NQC5"}})


 (atomist.main/handler #js {:command "UpdateNpmDependency"
                            :source {:slack {:channel {:id "CUCEERLBH"}
                                             :user {:id "UDF0NFB5M"}}}
                            :team {:id "AK748NQC5"}
                            :parameters [{:name "dependency"
                                          :value "[\"express\" \"4.1.2\"]"}]
                            :raw_message "npm update"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args))))
