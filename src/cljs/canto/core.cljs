(ns canto.core
  (:require [clojure.test.check.generators]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [clojure.spec :as s]
            [clojure.spec.test :as st]
            [clojure.pprint :refer [pprint]]
            [canto.reconciler :refer [reconciler]]))

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
    (dom/div #js {:className "Canto"} (:canto/text (om/props this)))))

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
      (dom/div #js {:className "Poem"}
        (dom/div #js {:className "Poem-wrap"}
          (dom/div #js {:className "Poem-name"} name)
          (apply dom/div nil (map canto (:poem/cantos (om/props this)))))))))

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
      (dom/div #js { :className "PoemList"}
        (apply dom/div nil
          (map (fn [poem]
                 (let [name (:poem/name poem)]
                   (dom/div #js {:className "PoemList-poemName"
                                 :onClick #(om/transact! this `[(poem/select! {:poem-ref [:poems/by-name ~name]})
                                                                :selected/poem])}
                           name)))
               list))))))

(def poem-list (om/factory PoemList))

(comment
  (let [e @dbg]
    (aget e "key")))

(defui Toolbar
  static om/IQuery
  (query [this]
         [[:show-new-poem-input '_]])
  Object
  (render [this]
          (let [show-new-poem-input (:show-new-poem-input (om/props this))]
            (dom/div #js {:className "Toolbar-wrap"}
                    (dom/div #js {:onClick #(om/transact! reconciler '[(poem/new)])}
                             (if show-new-poem-input "cancel" "add new poem"))
                    (if show-new-poem-input
                      (dom/input #js {:placeholder "poem title"
                                      :onKeyDown (fn [e]
                                                  (let [key (.-keyCode e)
                                                        value (-> e .-target .-value)]
                                                    (if (= key 13)
                                                      (om/transact! this [`(poem/create {:name ~value})]))))}))))))

(def toolbar-view (om/factory Toolbar))

(defui CantoApp
  static om/IQuery
  (query [this]
         [{:toolbar (om/get-query Toolbar)}
          {:poems/list (om/get-query PoemList)}
          {:selected/poem (om/get-query Poem)}])

  Object
  (render [this]
          (let [{:keys [selected/poem toolbar]} (om/props this)
                _ (println (om/props this))
                list (select-keys (om/props this) [:poems/list])]
            (dom/div #js {:className "CantoContainer"}
                     (toolbar-view toolbar)
                     (poem-view poem)
                     (poem-list list)))))


(om/add-root! reconciler CantoApp (gdom/getElement "app"))
