(ns canto.db
  (:require [datomic.api :as d]
            [clojure.pprint :as pp]))

(def uri "datomic:free://localhost:4334/cantos")

(defn get-conn [] (d/connect uri))
(defn db [] (d/db (get-conn)))

(defn get-data []
  (into []
    (d/q '[:find (pull ?e [:poem/name {:poem/cantos [:canto/text :canto/position]}])
           :where [?e :poem/name]]
      (db))))

(comment
  (def results {:poems (first (get-data))})

  (into []
    (d/q '[:find (pull ?e [:poem/_name])
           :where [?e :canto/text]]
      (db)))


  (d/pull (db) [:block/text])
  (pp/pprint results))
