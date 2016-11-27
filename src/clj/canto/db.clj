(ns canto.db
  (:require [datomic.api :as d]))

(def uri "datomic:free://localhost:4334/stanzas")

(defn get-conn [] (d/connect uri))
(defn db [] (d/db (get-conn)))

(def results
  (into []
    (d/q '[:find ?stanza-name ?block-text ?sort-value
           :where
           [?e :block/text ?block-text]
           [?e :block/stanza ?y]
           [?y :stanza/name ?stanza-name]
           [?a :sort/block ?e]
           [?a :sort/stanza ?y]
           [?a :sort/value ?sort-value]]
      (db))))
