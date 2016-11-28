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
            [om.next :as om])
  (:gen-class))

(defmulti readf (fn [_ k _] k))

(defmethod readf :customers/-id [env k params]
  {:value 0#_(db/get-customers-by-id (:db env))})

(defn generate-response [data & [status]]
  (let [out (ByteArrayOutputStream. 4096)
        writer (t/writer out :json)
        _ (t/write writer data)]
    {:status (or status 200)
     :headers {"Content-Type" "application/transit+msgpack, application/transit+json;q=0.9"}
     :body (.toString out)}))

(comment
  (defn om-next-query-resource [parser env body]
    (generate-response (parser env body))))

(def debug (atom nil))

(defn run-query [query db]
  (d/q `[~':find (~'pull ~'?e ~query)
         ~':where [~'?e ~':poem/name]]
    db))

(defroutes routes
  (POST "/props"
    {body :body}
    (let [query (:poems (first (t/read (t/reader body :json))))
          result {:poems (first (into [] (run-query query (db/db))))}]
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


(comment
  (deref debug)
  (db/db)
  (t/read (t/reader @debug :json)))
