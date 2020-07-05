(ns algoradio.download
  (:require [clojure.walk :as walk]
            [algoradio.state :refer [app-state]]
            [algoradio.common :refer [distinct-by]]
            [cljs.user :refer [spy]]))

(def download-link-id "download-link")

(defn set-as-freesound-query! [app-state {:keys [name data]}]
  (swap! app-state update-in [:freesounds name] concat data))

(defn set-as-freesound-queries! [data-edn app-state]
  (doseq [d data-edn] (set-as-freesound-query! d app-state)))

(defn toggle-uploader! []
  (::show-uploader? (swap! app-state update ::show-uploader? not)))

(defn parse-loaded-file! [event]
  (let [selections (-> event
                       .-target
                       .-result
                       js/JSON.parse
                       js->clj
                       walk/keywordize-keys
                       (update :name keyword))]
    (set-as-freesound-query! app-state selections)
    (toggle-uploader!)))

(comment (toggle-uploader!))

(defn main [app-state]
  [:div
   [:a {:id download-link-id :style {:display "none"}}]
   [:div {:class (str "download__uploader"
                      (when (get @app-state ::show-uploader?) " show"))}
    [:div {:class "download__main-container"}
     [:span {:on-click toggle-uploader!
             :class "download__uploader"} "X"]
     [:p "Click to upload a selection file"]
     [:input {:type "file"
              :on-change (fn [event]
                           (let [file (-> event .-target .-files (aget 0))
                                 reader (js/FileReader.)]

                             (js/console.log reader)
                             (set! (.. reader -onload) parse-loaded-file!)
                             (js/console.log reader)
                             (.readAsText reader file)))}]]]])

(defn download-json! [data name*]
  (if data
    (let [el (js/document.getElementById download-link-id)
          contents (js/JSON.stringify (clj->js {:name name*
                                                :data (distinct-by :url data)}))]
      ;; data:text/plain;charset=utf-8,' + encodeURIComponent(text)
      (.setAttribute el "href" (str "data:application/json;charset=utf-8," contents))
      (.setAttribute el "download" (str (name name*) ".json"))
      (.click el))))


(comment (download-json! {1 2} :holi-boli))


(let [obj (clj->js {})]
  (set! (.. obj -onload) identity)
  (js/console.log obj))
