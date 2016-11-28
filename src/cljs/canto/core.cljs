(ns canto.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cognitect.transit :as t]))

(enable-console-print!)

(defui Canto
  static om/IQuery
  (query [this]
    [:canto/text :canto/position])

  Object
  (render [this]
    (dom/div nil (:canto/text (om/props this)))))

(def canto (om/factory Canto))

(defui Poem
  static om/IQuery
  (query [this]
    [:poem/name {:poem/cantos (om/get-query Canto)}])

  Object
  (render [this]
    (dom/div nil (:poem/name (om/props this))
      (apply dom/div nil (map canto (:poem/cantos (om/props this)))))))

(def poem (om/factory Poem))

(defui Poems
  static om/IQuery
  (query [this]
    [{:poems (om/get-query Poem)}])

  Object
  (render [this]
    (dom/div nil
      (apply dom/div nil (map poem (:poems (om/props this)))))))

(def app-state (om/tree->db Poems nil true))

(defmulti read om/dispatch)

(defmethod read :default [env k params]
  (let [st @(:state env)]
    (if-let [[_ v] (find st k)]
      {:value v}
      {:remote true})))

(defn send [m cb]
  (doto (new js/XMLHttpRequest)
        (.open "POST" "/props")
        (.setRequestHeader "Content-Type" "application/transit+json")
        (.setRequestHeader "Accept" "application/transit+json")
        (.addEventListener "load"
          (fn [evt]
            (let [response (t/read (om/reader)
                                   (.. evt -currentTarget -responseText))]
              (cb response))))
        (.send (t/write (om/writer) (:remote m)))))

(def reconciler (om/reconciler {:state app-state
                                :send send
                                :parser (om/parser {:read read})
                                :remotes [:remote]}))

(om/add-root! reconciler Poems (gdom/getElement "app"))
