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
                  #db/id [:db.part/user -203]]}

   {:db/id #db/id [:db.part/user -504]
    :canto/text "sheer white shadows"
    :canto/position 0}

   {:db/id #db/id [:db.part/user -505]
    :canto/text "the lake is stirring"
    :canto/position 1}

   {:db/id #db/id [:db.part/user -506]
    :canto/text "a bird dives for food, exhale for 7"
    :canto/position 2}

   {:db/id #db/id [:db.part/user -401]
    :poem/name "coke's in the eyes of the be.holder"
    :poem/cantos [#db/id [:db.part/user -504]
                  #db/id [:db.part/user -505]
                  #db/id [:db.part/user -506]]}])

(defn reset-db []
  (d/delete-database db/uri)
  (d/create-database db/uri)
  (let [conn (db/get-conn)]
    @(d/transact conn schema)
    @(d/transact conn initial-data)))

(comment
  (deref (d/transact (db/get-conn) [{:db/id #db/id [:db.part/user -100]
                                     :poem/name "black crows die"}]))

  (reset-db))
