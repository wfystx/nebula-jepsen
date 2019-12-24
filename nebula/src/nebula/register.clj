(ns nebula.register
  "Single atomic register test"
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.logging   :refer :all]
            [jepsen [checker         :as checker]
                    [client          :as client]
                    [nemesis         :as nemesis]
                    [independent     :as independent]]
            [jepsen.checker.timeline :as timeline]
            [knossos.model           :as model]
            [nebula.client           :as nclient]
            [nebula.core             :as ncore])
  (:import [com.vesoft.nebula.storage.client StorageClientImpl]
           (clojure.lang ExceptionInfo)))

(def default-meta-host "172.28.1.1")
(def default-meta-port 45500)

(def key "f")

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})

(defrecord Client [conn]
  client/Client

  (open! [this test node] this)

  (setup! [this test node]
    (assoc this :conn (nclient/init default-meta-host default-meta-port))) ; :conn is a object of StorageClient

  (invoke! [this test op]
    (try
        (case (:f op)
          :read (let [v (nclient/nget conn key)]
                    (assoc op :type (if (= v nil) :fail :ok), :value v)) ; get the value of a specific key from storage
          :write (let [res (nclient/nput conn key (:value op))] ; put a key : value pair to storage
                    (assoc op :type (if (false? res) :fail :ok))))
          (catch java.lang.NullPointerException e
            (assoc op :type :fail, :error :nullpointer_exception)))) ; basically this will happen when there is no that key

  (teardown! [this test])

  (close! [_ test]))

(defn test
  [opts]
  (ncore/basic-test
    (merge
      {:name      "register-test"
       :client    (Client. nil)
       :checker   (checker/compose
                     {:perf     (checker/perf)
                      :timeline (timeline/html)
                      :linear   (checker/linearizable {:model (model/register)
                                                       :algorithm :linear})})
       :generator (ncore/generator [r w] (:time-limit opts))}
      opts)))