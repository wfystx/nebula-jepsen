(ns nebula.core
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [checker :as checker]
                    [cli :as cli]
                    [client :as client]
                    [control :as c]
                    [db :as db]
                    [generator :as gen]
                    [nemesis :as nemesis]
                    [independent :as independent]
                    [util :refer [timeout meh]]
                    [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.centos :as centos]
            [jepsen.checker.timeline :as timeline]
            [knossos.model :as model]
            [nebula.client :as nclient])
  (:import [com.vesoft.nebula.storage.client StorageClientImpl]
           [com.vesoft.nebula.graph.client GraphClientImpl]))

(def dir     "/opt/nebula")
(def binary  "nebula")
(def logfile (str dir "/nebula.log"))
(def pidfile (str dir "/nebula.pid"))

(def default-port 44500)
(def default-space 1)
(def default-meta-host "172.28.1.1")
(def default-meta-port 45500)
(def default-graph-host "172.28.3.1")
(def default-graph-port 3699)
(def default-username "user")
(def default-password "password")

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})

(defn create-space
  "Not using yet. Test space will be created by ./start script"
  [graphHost graphPort]
  (let [graphClient (GraphClientImpl. graphHost graphPort)]
       (.connect graphClient default-username default-password)
       (.execute graphClient "create space test(partition_num=5,replica_factor=3)"))
)

(defn get-random-key
  []
  (+ (rand-int 5) 1))

(defn db
  "Nebula DB"
  [version]
  (reify db/DB
    (setup! [_ test node]
      ;(info node "Depolying Nebula KV Test Environment")
      )

    (teardown! [_ test node]
      (info "tearing down Nebula" node)
      (c/su (c/exec :rm :-rf dir)))
      
    db/LogFiles
    (log-files [_ test node]
      [logfile])))

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(defrecord Client [conn]
  client/Client

  (open! [this test node] this)

  (setup! [this test node]
    (assoc this :conn (nclient/init default-meta-host default-meta-port)))

  (invoke! [this test op]
    (let [k (get-random-key)
          crash (if (= :read (:f op)) :fail :info)]
      (try
          (case (:f op)
            :read (assoc op :type :ok, :value (-> conn
                                                  (nclient/get k)))
            :write (do (nclient/put conn k (:value op))
                                (assoc op :type :ok)))
            (catch java.lang.NullPointerException e
              (assoc op :type :fail, :error :nullpointer_exception)))))

  (teardown! [this test])

  (close! [_ test]))

(defn nebula-test
  "Given an options map from the command-line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:name "Nebula"
          :os   centos/os
          :db   (db "v1.0.0")
          :client (Client. nil)
          :nemesis (nemesis/partition-random-halves)
          :checker (checker/compose
                     {:perf     (checker/perf)
                      :timeline (timeline/html)
                      :linear (checker/linearizable {:model (model/register)
                                                     :algorithm :linear})})
          :generator (->> (gen/mix [r w])
                          (gen/stagger 1/2)
                          (gen/nemesis
                            (gen/seq (cycle [(gen/sleep 5)
                                             {:type :info, :f :start}
                                             (gen/sleep 5)
                                             {:type :info, :f :stop}])))
                          (gen/time-limit (:time-limit opts)))}))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn nebula-test})
                   (cli/serve-cmd))
            args))