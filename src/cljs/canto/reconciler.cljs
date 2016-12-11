(ns canto.reconciler
  (:require [om.next :as om]
            [clojure.walk :as walk]
            [cognitect.transit :as t]))

(def dbg (atom nil))

(defmulti mutate om/dispatch)

(defmethod mutate 'poem/create
  [env k params]
  {:remote true})

(defmethod mutate 'poem/new
  [{:keys [state]} k params]
  {:action #(swap! state update :show-new-poem-input not)})

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

(defmethod read :toolbar [env k params]
  (let [st @(:state env)]
    {:value (om/db->tree (:query env) (get st k) st)}))

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

(def reconciler (om/reconciler {:state {:show-new-poem-input false}
                                :send send
                                :normalize true
                                :parser (om/parser {:read read :mutate mutate})
                                :remotes [:selected/poem :poems/list :remote]
                                :merge-tree (fn deep-merge [a b]
                                              (merge-with (fn [x y]
                                                            (if (map? y)
                                                              (deep-merge x y)
                                                              y))
                                                 a b))}))
