(ns nebula.multi-register
  "Multi-register test"
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen.client :as client]
            [jepsen.checker :as checker]
            [jepsen.generator :as gen]
            [jepsen.independent :as independent]
            [jepsen.util :as util]
            [jepsen.checker.timeline :as timeline]
            [knossos.model :as model]
            [nebula.client :as nclient]
            [nebula.core :as ncore])
  (:import  [com.vesoft.nebula.storage.client StorageClientImpl]
            (knossos.model Model)))

(def default-meta-host "172.28.1.1")
(def default-meta-port 45500)

(defrecord MultiRegister []
  Model
  (step [this op]
    (reduce (fn [state [f k v]]
              ; Apply this particular op
              (case f
                :r (if (or (nil? v)
                           (= v (get state k)))
                     state
                     (reduced
                       (model/inconsistent
                         (str (pr-str (get state k)) "≠" (pr-str v)))))
                :w (assoc state k v)))
            this
            (:value op))))

(defn multi-register
  "A register supporting read and write transactions over registers identified
  by keys. Takes a map of initial keys to values. Supports a single :f for ops,
  :txn, whose value is a transaction: a sequence of [f k v] tuples, where :f is
  :read or :write, k is a key, and v is a value. Nil reads are always legal."
  [values]
  (map->MultiRegister values))

; Three keys, five possible values per key.
(def key-range (vec (range 10)))
(defn get-random-key [] [(rand-nth key-range)])
(defn rand-val [] (rand-int 5))

(defn r
  "Read a random subset of keys."
  [_ _]
  (->> (get-random-key)
       (mapv (fn [k] [:r k nil]))
       (array-map :type :invoke, :f :read, :value)))

(defn w [_ _]
  "Write a random subset of keys."
  (->> (get-random-key)
       (mapv (fn [k] [:w k (rand-val)]))
       (array-map :type :invoke, :f :write, :value)))

(defrecord Client [conn]
  client/Client

  (open! [this test node] this)

  (setup! [this test node]
    (assoc this :conn (nclient/init default-meta-host default-meta-port))) ; :conn is a object of StorageClient

  (invoke! [this test op]
    (let [[txn] (:value op)]
      (let [[f key v] txn]
          (try
              (case (:f op)
                :read (let [res (nclient/nget conn key)]
                          (assoc op :type (if (= res nil) :fail :ok),
                           :value (mapv (fn [res] [f key res]) [res]))) ; get the value of a specific key from storage
                :write (let [res (nclient/nput conn key v)] ; put a key : value pair to storage
                          (assoc op :type (if (false? res) :fail :ok))))
                (catch java.lang.NullPointerException e
                  (assoc op :type :fail, :error :nullpointer_exception)))))) ; basically this will happen when there is no that key

  (teardown! [this test])

  (close! [_ test]))

(defn test
  [opts]
  (ncore/basic-test
    (merge
      {:name      "multi-register-test"
       :client    (Client. nil)
       :checker   (checker/compose
                     {:perf     (checker/perf)
                      :timeline (timeline/html)
                      :linear   (checker/linearizable {:model (multi-register {})
                                                       :algorithm :linear})})
       :generator (ncore/generator [r w] (:time-limit opts))}
      opts)))