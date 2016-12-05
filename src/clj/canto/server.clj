(ns canto.server
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.adapter.jetty :refer [run-jetty]]
            [canto.db :as db]
            [cognitect.transit :as t]
            [datomic.api :as d]
            [om.next.server :as om])
  (:gen-class))

(def dbg (atom nil))

(deref dbg)

(defn run-query [query entity-key db]
  (mapv first
    (d/q `[~':find (~'pull ~'?e ~query)
           ~':where [~'?e ~entity-key]]
      db)))

(comment (map first (run-query [:poem/name {:poem/cantos [:canto/text]}] :poem/name (db/db))))

(defmulti readf (fn [_ k _] k))

(defmethod readf :poems/list
  [{:keys [db query]} k params]
  {:value (run-query query :poem/name db)})

(defmethod readf :selected/poem
  [{:keys [db query]} k {:keys [poem/name]}]
  {:value (ffirst (d/q `[~':find (~'pull ~'?e ~query)
                         ~':where [~'?e ~':poem/name ~name]]
                    db))})

(defn generate-response [data & [status]]
  (let [out (ByteArrayOutputStream. 4096)
        writer (t/writer out :json)
        _ (t/write writer data)]
    {:status (or status 200)
     :headers {"Content-Type" "application/transit+msgpack, application/transit+json;q=0.9"}
     :body (.toString out)}))

(defroutes routes
  (POST "/props"
    {body :body}
    (let [parser (om/parser {:read readf})
          env {:db (db/db)}
          query (t/read (t/reader body :json))
          _ (reset! dbg query)
          result (parser env query)]
      (generate-response result)))
  (GET "/" _
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (io/input-stream (io/resource "public/index.html"))})
  (resources "/"))

(def http-handler
  (-> routes
      (wrap-defaults api-defaults)
      wrap-gzip))

(defn -main [& [port]]
  (let [port (Integer. (or port 10555))]
    (run-jetty http-handler {:port port :join? false})))
