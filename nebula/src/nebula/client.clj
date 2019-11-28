(ns nebula.client
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [cli :as cli]
                    [control :as c]
                    [db :as db]
                    [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.centos :as centos])
  (:import [com.vesoft.nebula.storage.client StorageClientImpl]
           [com.vesoft.nebula.meta.client MetaClientImpl]
           [com.vesoft.nebula.graph.client GraphClientImpl]))

(def default-port 44500)
(def default-space 1)
(def default-meta-host "172.28.1.1")
(def default-meta-port 45500)

(defn init
  [metaHost metaPort]
  (let [metaClient (MetaClientImpl. metaHost metaPort)]
       (def storageClient (StorageClientImpl. metaClient))
       (.switchSpace storageClient default-space)
       storageClient))

(defn connect
  ([server-uri]
   (connect server-uri {}))
  ([server-uri opts]
   (merge {:port              default-port
           :endpoint          server-uri
           :space             default-space}
          opts)))

(defn put
  ([storageClient k v]
    (let [part (+ (mod (.hash storageClient k) 5) 1)]
      (.put storageClient part k v))))

(defn get
  ([storageClient k]
    (let [part (+ (mod (.hash storageClient k) 5) 1)]
      (.get storageClient part k))))

