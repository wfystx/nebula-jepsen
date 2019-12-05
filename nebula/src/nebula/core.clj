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
(def logfile "/root/nebula.log")
(def pidfile "/root/pids/nebula-storaged.pid")
(def datafile "/data/storage/nebula/*")

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
  [node]
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
            "/usr/local/nebula/etc/nebula-storaged.conf")
    (info node "started")))

(defn stop-nebula!
  "Stops nebula storage service."
  [node]
  (info "stopping storage node" node)
  (c/su
    (cu/grepkill! :nebula-storaged)
    (c/exec :rm :-rf pidfile)))

(defn flag-file
  []
  (str dir "etc/nebula-storaged.conf"))

(defn create-space
  "Not using yet. Test space will be created by ./up.sh script"
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
      (c/su 
        (start-nebula! node) ;start nebula storage in each node
        (Thread/sleep 3000)))

    (teardown! [_ test node]
      (stop-nebula! node) ;stop
      (Thread/sleep 3000)
      (c/su 
        (c/exec :rm :-rf (c/lit "/data/storage/nebula/*")))) ;clean data stored in storage node
        
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
    (assoc this :conn (nclient/init default-meta-host default-meta-port))) ; :conn is a object of StorageClient

  (invoke! [this test op]
    (try
        (case (:f op)
          :read (assoc op :type :ok, :value (-> conn
                                                (nclient/get "f"))) ; get the value of a specific key from storage
          :write (do (nclient/put conn "f" (:value op)) ; put a key : value pair to storage
                              (assoc op :type :ok)))
          (catch java.lang.NullPointerException e
            (assoc op :type :fail, :error :nullpointer_exception)))) ; basically this will happen when there is no that key

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
          :nemesis nemesis/noop
          :checker (checker/compose
                     {:perf     (checker/perf)
                      :timeline (timeline/html)
                      :linear   (checker/linearizable {:model (model/register)
                                                       :algorithm :linear})})
          :generator (->> (gen/mix [r w])
                          (gen/stagger 3)
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