;; Copyright © 2020 Atomist, Inc.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns user
  (:require [atomist.main]
            [atomist.local-runner :refer [set-env call-event-handler fake-push fake-command-handler]]))

(enable-console-print!)
(set-env :prod-github-auth)

(comment
  (-> (fake-push "AEIB5886C" "slimslender" "nextjs-blog" "master")
      (assoc :configurations [{:name "follow the leader"
                               :parameters [{:name "policy" :value "manualConfiguration"}
                                            {:name "dependencies" :value "{\"react\": \"16.13.1\"}"}]}
                              {:name "Update NPM Dependencies"
                               :parameters [{:name "scope" :value "{}"}]}])
      (call-event-handler atomist.main/handler))
 ;; this should fail with a bad JSON
  (-> (fake-push "AK748NQC5" "atomisthqa" "express-test-0" "master")
      (assoc :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                            {:name "dependencies" :value "{express \"4.1.2\"}"}]}])
      (call-event-handler atomist.main/handler))

 ;; bot should ask user to authorize
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
