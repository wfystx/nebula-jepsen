(defproject jepsen.nebula "0.1.0-SNAPSHOT"
  :description "A Jepsen test for nebula"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main nebula.runner
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jepsen "0.1.15-SNAPSHOT"]]
  :resource-paths ["lib/*" "lib/guava/*"]
  :plugins [[lein-expand-resource-paths "0.0.1"]])

