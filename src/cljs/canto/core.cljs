(ns canto.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [clojure.walk :as walk]
            [cognitect.transit :as t]))

(enable-console-print!)

(def dbg (atom nil))

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
    (if-let [name (:poem/name (om/props this))]
      (dom/div nil name
        (apply dom/div nil (map canto (:poem/cantos (om/props this)))))
      (dom/div nil ""))))

(def poem-view (om/factory Poem))

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
      (apply dom/ul nil
        (map (fn [poem]
               (let [name (:poem/name poem)]
                 (dom/li #js {:onClick #(om/transact! this `[(poem/select! {:poem-ref [:poems/by-name ~name]})
                                                             :selected/poem])}
                         name)))
             list)))))

(def poem-list (om/factory PoemList))

(defui CantoApp
  static om/IQuery
  (query [this]
    [{:poems/list (om/get-query PoemList)}
     {:selected/poem (om/get-query Poem)}])

  Object
  (render [this]
    (let [{:keys [selected/poem]} (om/props this)
          list (select-keys (om/props this) [:poems/list])
          _ (cljs.pprint/pprint poem)]
      (dom/div #js {:style #js {:display "flex" :flex 1}}
        (poem-list list)
        (poem-view poem)))))

(def app-state (om/tree->db CantoApp {} true))

(defmulti mutate om/dispatch)

(defmethod mutate 'poem/select!
  [{:keys [state]} k {:keys [poem-ref]}]
  #_(reset! dbg [state poem-ref])
  {:action #(swap! state assoc :selected/poem poem-ref)})

(defmulti read om/dispatch)

(defmethod read :poems/list
  [{:keys [state]} k params]
  (let [st @state]
    (if-let [val (get st k)]
      {:value (mapv #(get-in st %) val)}
      {:poems/list true})))

(defmethod read :selected/poem
  [{:keys [state ast query]} k params]
  (let [st @state]
    (if-let [poem-ref (get st k)]
      (let [poem (get-in st poem-ref)
            query_ [`({:selected/poem ~query} {:poem/name ~(:poem/name poem)})]]
        (if (nil? (:poem/cantos poem))
          {:selected/poem (om/query->ast query_)}
          {:value (om/db->tree query poem st)})))))

(defmethod read :default [env k params]
  (let [st @(:state env)]
    (if-let [v (get st k)]
      {:value v}
      {:remote true})))

(defn send [m cb]
  (let [remote-key (first (keys m))
        query (case remote-key
                :selected/poem (first (remote-key m))
                (remote-key m))]
    (doto (new js/XMLHttpRequest)
          (.open "POST" "/props")
          (.setRequestHeader "Content-Type" "application/transit+json")
          (.setRequestHeader "Accept" "application/transit+json")
          (.addEventListener "load"
            (fn [evt]
              (let [response (t/read (om/reader)
                                     (.. evt -currentTarget -responseText))]
                (reset! dbg response)
                (cb (case remote-key
                      :poems/list response
                      :selected/poem response)))))
          (.send (t/write (om/writer) query)))))

(def reconciler (om/reconciler {:state app-state
                                :send send
                                :normalize true
                                :parser (om/parser {:read read :mutate mutate})
                                :remotes [:selected/poem :poems/list]}))

(om/add-root! reconciler CantoApp (gdom/getElement "app"))

(comment
  (cljs.pprint/pprint (deref (om/app-state reconciler)))
  (cljs.pprint/pprint (deref dbg))
  (cljs.pprint/pprint (om/tree->db CantoApp @dbg true))
  (cljs.pprint/pprint (-> reconciler :state deref :root)))
