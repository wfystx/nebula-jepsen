(ns nebula.nemesis
  (:require [clojure.tools.logging :refer :all]
            [jepsen.nemesis :as nemesis]
            [nebula.core :as nebula]))


(defn get-random-node
  [coll]
  (nth coll (rand-int 5)))

(def kill-node
  "Kills random node"
  (nemesis/node-start-stopper
    get-random-node
    (fn start [test node] (nebula/stop-nebula! node test))
    (fn stop [test node] (nebula/start-nebula! node test))))

(def partition-random-node
  (nemesis/partition-random-node))

(def noop nemesis/noop)