(ns canto.utils
  (:require [canto.schema :refer [schema]]
            [datomic.api :as d]
            [canto.db :as db]))

(def initial-data
   ; Cantos
  [{:db/id #db/id [:db.part/user -201]
    :canto/text "if you could try and believe, even for 1 sec"
    :canto/position 0}

   {:db/id #db/id [:db.part/user -202]
    :canto/text "you would see, the coastal maine sea, awash in silence"
    :canto/position 1}

   {:db/id #db/id [:db.part/user -203]
    :canto/text "your body would ache for the luke warm breeze, the light belts out"
    :canto/position 2}

   {:db/id #db/id [:db.part/user -100]
    :poem/name "black crows die"
    :poem/cantos [#db/id [:db.part/user -201]
                  #db/id [:db.part/user -202]
                  #db/id [:db.part/user -203]]}])

(defn reset-db []
  (d/delete-database db/uri)
  (d/create-database db/uri)
  (let [conn (db/get-conn)]
    @(d/transact conn schema)
    @(d/transact conn initial-data)))
