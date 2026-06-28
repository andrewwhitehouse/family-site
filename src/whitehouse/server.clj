(ns whitehouse.server
  "Jetty entry point."
  (:require [ring.adapter.jetty :as jetty]
            [whitehouse.handler :as handler])
  (:gen-class))

(defn- port []
  (Integer/parseInt (or (System/getenv "PORT") "3000")))

(defn -main [& _]
  (let [p (port)]
    (println (str "Whitehouse family site listening on http://localhost:" p))
    (jetty/run-jetty handler/app {:port p :join? true})))
