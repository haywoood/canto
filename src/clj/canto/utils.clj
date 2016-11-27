(ns canto.utils
  (:require [canto.schema :refer [schema]]
            [datomic.api :as d]
            [canto.db :as db]))

(def initial-data
   ; Stanza
  [{:db/id #db/id [:db.part/user -100]
    :stanza/name "black crows die"}
   ; Blocks
   {:db/id #db/id [:db.part/user -101]
    :block/text "if you could try and believe, even for 1 sec"
    :block/stanza #db/id [:db.part/user -100]}
   {:db/id #db/id [:db.part/user -102]
    :block/text "you would see, the coastal maine sea, awash in silence"
    :block/stanza #db/id [:db.part/user -100]}
   {:db/id #db/id [:db.part/user -103]
    :block/text "your body would ache for the luke warm breeze, the light belts out"
    :block/stanza #db/id [:db.part/user -100]}
   ; Sort entries
   {:db/id (d/tempid :db.part/user)
    :sort/value 0
    :sort/stanza #db/id [:db.part/user -100]
    :sort/block #db/id [:db.part/user -101]}
   {:db/id (d/tempid :db.part/user)
    :sort/value 1
    :sort/stanza #db/id [:db.part/user -100]
    :sort/block #db/id [:db.part/user -102]}
   {:db/id (d/tempid :db.part/user)
    :sort/value 2
    :sort/stanza #db/id [:db.part/user -100]
    :sort/block #db/id [:db.part/user -103]}])

(defn reset-db []
  (d/delete-database db/uri)
  (d/create-database db/uri)
  (let [conn (db/get-conn)]
    @(d/transact conn schema)
    @(d/transact conn initial-data)))
