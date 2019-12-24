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

(def dir     "/usr/local/nebula/")
(def binary  (str dir "bin/nebula-storaged"))
(def logfile (str dir "logs/nebula-storaged.log"))
(def pidfile "/root/pids/nebula-storaged.pid")
(def datafile "/data/storage/nebula/*")
(def flag-file (str dir "etc/nebula-storaged.conf"))

(def default-port 44500)
(def default-space 1)
(def default-meta-host "172.28.1.1")
(def default-meta-port 45500)
(def default-graph-host "172.28.3.1")
(def default-graph-port 3699)
(def default-username "user")
(def default-password "password")

(defn running?
  "Is the service running?"
  [binary pidfile]
  (try
    (c/exec :start-stop-daemon :--status
            :--pidfile pidfile
            :--exec    binary)
    true
    (catch RuntimeException _ false)))

(defn start-nebula! 
  "Starts nebula storage service."
  [node test]
  (info "starting storage node" node)
  (c/su
    (assert (not (running? binary pidfile)))
    (c/exec :start-stop-daemon :--start
            :--background
            :--make-pidfile
            :--pidfile  pidfile
            :--chdir    dir
            :--exec     binary
            :--
            "--flagfile"
            flag-file
            :>> logfile
            (c/lit "2>&1"))
    (info node "started")))

(defn stop-nebula!
  "Stops nebula storage service."
  [node test]
  (info "stopping storage node" node)
  (c/su
    (cu/grepkill! :nebula-storaged)
    (c/exec :rm :-rf pidfile)))

(defn create-space
  "Not using yet. Test space will be created by ./up.sh script"
  [graphHost graphPort]
  (let [graphClient (GraphClientImpl. graphHost graphPort)]
       (.connect graphClient default-username default-password)
       (.execute graphClient "create space test(partition_num=5,replica_factor=3)"))
)

(defn db
  "Nebula DB"
  [version]
  (reify db/DB
    (setup! [_ test node]
      (c/su 
        (start-nebula! node test) ;start nebula storage in each node
        (Thread/sleep 3000)))

    (teardown! [_ test node]
      (stop-nebula! node test) ;stop
      (Thread/sleep 3000)
      (c/su 
        (c/exec :rm :-rf (c/lit datafile)))) ;clean data stored in storage node
        
    db/LogFiles
      (log-files [_ test node]
        [logfile])))

(defn generator
  [operations time-limit]
  (->> (gen/mix operations)
       (gen/stagger 3)
       (gen/nemesis
        (gen/seq (cycle [(gen/sleep 10)
          {:type :info, :f :start}
          (gen/sleep 20)
          {:type :info, :f :stop}])))
       (gen/time-limit time-limit)))

(defn checker
  [details]
  (checker/compose
    {:perf     (checker/perf)
     :timeline (timeline/html)
     :details  details}))

(defn basic-test
  "Sets up the test parameters common to all tests."
  [options]
  (info :opts options)
  (merge tests/noop-test
    (dissoc options
      :test-fns)
    {:name    "basic-test"
     :os      centos/os
     :db      (db "v1.0.0")
     :nemesis (:nemesis options)}))