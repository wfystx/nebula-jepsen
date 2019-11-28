(ns jepsen.nebula.tests
  "Provide utilities for writing tests using jepsen."
  (:require [jepsen.os :as os]
            [jepsen.db :as db]
            [jepsen.client :as client]
            [jepsen.nemesis :as nemesis]
            [jepsen.generator :as gen]
            [knossos.model :as model]
            [jepsen.checker :as checker]
            [jepsen.net :as net]))

(def nebula-test
  "Boring test stub.
  Typically used as a basis for writing more complex tests.
  "
  {:nodes     ["n1"]
   :name      "nebula-test"
   :os        os/noop
   :db        db/noop
   :net       net/iptables
   :client    client/noop
   :nemesis   nemesis/noop
   :generator gen/void
   :checker   (checker/unbridled-optimism)})