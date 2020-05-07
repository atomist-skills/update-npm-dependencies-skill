(ns user
  (:require [atomist.main]
            [atomist.local-runner :refer [set-env call-event-handler fake-push fake-command-handler]]
            [atomist.cljs-log :as log]))

(enable-console-print!)
(set-env :prod-github-auth)

(comment
 ;; this should fail with a bad JSON
 (-> (fake-push "AK748NQC5" "atomisthqa" "express-test-0" "master")
     (assoc :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                           {:name "dependencies" :value "{express \"4.1.2\"}"}]}])
     (call-event-handler atomist.main/handler))

 (-> (fake-command-handler "AK748NQC5" "UpdateNpmDependency" "npm update" "CUCEERLBH" "UDF0NFB5M")
     (assoc :parameters [{:name "dependency"
                          :value "{\"express\": \"4.1.2\"}"}])
     (call-event-handler atomist.main/handler))

 (-> (fake-command-handler "T29E48P34" "ShowNpmDependencies" "npm fingerprints" "C012WB9MCCB" "U2ATJPCSK")
     (assoc :configurations [{:name "test"
                              :enabled true
                              :parameters [{:name "policy" :value "manualConfiguration"}
                                           {:name "dependencies" :value "{\"@google-cloud/pubsub\": \"1.7.2\"}"}]}])
     (call-event-handler atomist.main/handler))

 (-> (fake-command-handler "T29E48P34" "SyncNpmDependency" "npm sync" "C012WB9MCCB" "U2ATJPCSK")
     (assoc :configurations [{:name "test"
                              :enabled true
                              :parameters [{:name "policy" :value "manualConfiguration"}
                                           {:name "dependencies" :value "{\"@google-cloud/pubsub\": \"1.7.2\"}"}]}])
     (call-event-handler atomist.main/handler)))
