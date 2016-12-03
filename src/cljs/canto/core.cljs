(ns canto.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [clojure.walk :as walk]
            [cognitect.transit :as t]))

(enable-console-print!)

(defui Canto
  static om/Ident
  (ident [this props]
    [:cantos/by-text (:canto/text props)])

  static om/IQuery
  (query [this]
    [:canto/text :canto/position])

  Object
  (render [this]
    (dom/div nil (:canto/text (om/props this)))))

(def canto (om/factory Canto))

(defui Poem
  static om/Ident
  (ident [this props]
    [:poems/by-name (:poem/name props)])

  static om/IQuery
  (query [this]
    [:poem/name {:poem/cantos (om/get-query Canto)}])

  Object
  (render [this]
    (dom/div nil (:poem/name (om/props this))
      (apply dom/div nil (map canto (:poem/cantos (om/props this)))))))

(def poem (om/factory Poem))

(defui PoemList
  static om/Ident
  (ident [this props]
    [:poems/by-name (:poem/name props)])
  static om/IQuery
  (query [this]
    [:poem/name])

  Object
  (render [this]
    (let [list (:poems/list (om/props this))]
      (apply dom/ul nil (map #(dom/li nil (:poem/name %)) list)))))

(def poem-list (om/factory PoemList))

(defui CantoApp
  static om/IQuery
  (query [this]
    [{:poems/list (om/get-query PoemList)}])

  Object
  (render [this]
    (let [poems-list (select-keys (om/props this) [:poems/list])]
      (cljs.pprint/pprint (om/props this))
      (dom/div nil
        (poem-list poems-list)))))

(def app-state (om/tree->db CantoApp nil true))

(defmulti read om/dispatch)

(defmethod read :poems/list
  [{:keys [state]} k params]
  (let [st @state]
    (if-let [val (get st k)]
      {:value (mapv #(get-in st %) val)}
      {:remote true})))

(defmethod read :default [env k params]
  (let [st @(:state env)]
    (if-let [v (get st k)]
      {:value v}
      {:remote true})))

(def dbg (atom nil))
(deref dbg)
(defn send [m cb]
  (reset! dbg m)
  (doto (new js/XMLHttpRequest)
        (.open "POST" "/props")
        (.setRequestHeader "Content-Type" "application/transit+json")
        (.setRequestHeader "Accept" "application/transit+json")
        (.addEventListener "load"
          (fn [evt]
            (let [response (t/read (om/reader)
                                   (.. evt -currentTarget -responseText))]
              (reset! dbg response)
              (cb (om/tree->db PoemList response true)))))
        (.send (t/write (om/writer) (:remote m)))))

(def reconciler (om/reconciler {:state app-state
                                :send send
                                :parser (om/parser {:read read})
                                :remotes [:remote]}))

(om/add-root! reconciler CantoApp (gdom/getElement "app"))

(deref (om/app-state reconciler))
