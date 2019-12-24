(ns nebula.runner
  "Runs Nebula Graph tests."
  (:gen-class)
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :refer :all]
            [jepsen.cli :as jc]
            [jepsen.core :as jepsen]
            [nebula.nemesis :as nemesis]
            [nebula.register :as register]
            [nebula.multi-register :as multi-register]
            [nebula.cas :as cas-register]))

(def tests
  "A map of test names to test constructors."
  {"single-register"  register/test
   "multi-register" multi-register/test
   "cas-register" cas-register/test})

(def nemesis-types
  {"noop"                   nemesis/noop
   "partition-random-node"  nemesis/partition-random-node
   "kill-node"              nemesis/kill-node})

(def opt-spec
  "Command line options for tools.cli"
  [(jc/repeated-opt "-t" "--test NAME" "Test(s) to run" [] tests)
   ["-nemesis" "--nemesis Nemesis"
    "What Nemesis to use"
    :default  nemesis/noop
    :parse-fn nemesis-types
    :validate [identity (jc/one-of nemesis-types)]]])

(defn log-test
  [t]
  (info "Testing\n" (with-out-str (pprint t)))
  t)

(defn test-cmd
  []
  {"test" {:opt-spec (into jc/test-opt-spec opt-spec)
           :opt-fn (fn [parsed]
                     (-> parsed
                         jc/test-opt-fn
                         (jc/rename-options {
                          :test :test-fns})))
           :usage (jc/test-usage)
           :run (fn [{:keys [options]}]
                  (pprint options)
                  (doseq [i        (range (:test-count options))
                          test-fn  (:test-fns options)]
                    ; Rehydrate test and run
                    (let [
                          test (-> options
                                   test-fn
                                   log-test
                                   jepsen/run!)]
                      (when-not (:valid? (:results test))
                        (System/exit 1)))))}})

(defn -main
  [& args]
  (jc/run! (merge (jc/serve-cmd)
                  (test-cmd))
           args))