(ns build
  "Build the deployable uberjar.  Usage:  clj -T:build uber"
  (:require [clojure.tools.build.api :as b]))

(def lib 'whitehouse/family-site)
(def main 'whitehouse.server)
(def class-dir "target/classes")
(def uber-file "target/app.jar")

(defn- basis [] (b/create-basis {:project "deps.edn"}))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis      (basis)
                  :ns-compile [main]
                  :class-dir  class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     (basis)
           :main      main})
  (println "Built" uber-file))
