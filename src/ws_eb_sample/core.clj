(ns ws-eb-sample.core
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.handler :as handler]
            [org.httpkit.server :as ws])
  (:gen-class))

(def clients (atom {}))

(defn handle-websocket [req]
  (ws/with-channel req con
    (println "Connection from" con)
    (swap! clients assoc con true)

    (ws/on-receive con #(ws/send! con (str "You said: " %)))

    (ws/on-close con (fn [status]
                       (println con "disconnected with status" (name status))
                       (swap! clients dissoc con)))))

(defroutes routes
  (GET "/ws" [] handle-websocket))

(def application (handler/site routes))

(defn -main [& _]
  (let [port (-> (System/getenv "SERVER_PORT")
                 (or "8080")
                 (Integer/parseInt))]
    (ws/run-server application {:port port, :join? false})
    (println "Listening for connections on port" port)))
