(ns algoradio.fs
  (:require [clojure.walk :as walk]
            [algoradio.state :refer [app-state]]
            [algoradio.api :as api]
            [algoradio.alert :as alert]
            [algoradio.common :refer [distinct-by
                                      parse-query-string
                                      set-as-freesound-queries!
                                      set-as-freesound-query!]]
            [algoradio.replayer.core :as replayer]
            ["axios" :as axios]))

(def download-link-id "download-link")

(defn toggle-uploader! [type]
  (::show-uploader? (swap! app-state assoc ::show-uploader? type)))

(defn parse-loaded-file! [on-parse event]
  (let [parsed-file (-> event
                        .-target
                        .-result
                        js/JSON.parse
                        js->clj
                        walk/keywordize-keys)]
    (on-parse parsed-file)
    (toggle-uploader! nil)))

(defn load-and-parse-file! [on-parse]
  (fn [event]
    (let [file (-> event .-target .-files (aget 0))
          reader (js/FileReader.)]
      (set! (.. reader -onload)
            (partial parse-loaded-file!
                     (fn [data] on-parse)))
      (js/console.debug "FileReader" reader)
      (.readAsText reader file))))

(comment (toggle-uploader!))

(def on-parse-fns
  {:selections (partial set-as-freesound-query! app-state)
   :history (partial replayer/replay! app-state)})

(defn main [app-state]
  (let [uploader-type (get @app-state ::show-uploader?)]
    [:div
     [:a {:id download-link-id :style {:display "none"}}]
     [:div {:class (str "fs__uploader"
                        (when uploader-type " show"))}
      [:div {:class "fs__main-container"}
       [:span {:on-click toggle-uploader!
               :class "fs__uploader-close"} "X"]
       [:p (str"Click to upload a " (when uploader-type (name uploader-type))  " file")]
       [:input {:type "file"
                :on-change (load-and-parse-file!
                            (on-parse-fns uploader-type))}]]]]))

(defn download-json! [name* contents]
  (let [el (js/document.getElementById download-link-id)]
    ;; data:text/plain;charset=utf-8,' + encodeURIComponent(text)
    (.setAttribute el "href" (str "data:text/plain;charset=utf-8," contents))
    (.setAttribute el "download" (str name* ".json"))
    (.click el)))

(defn download-selections! [name* data]
  (if data
    (download-json!
     name*
     (js/JSON.stringify (clj->js {:name name*
                                  :data (distinct-by :url data)})))))
(defn download-history! [name* data]
  (if data
    (download-json!
     name*
     (js/JSON.stringify (clj->js data)))))

(defn replay-from-url! [url]
  (-> (axios/get url)
      (.then #(-> %
                  js->clj
                  walk/keywordize-keys
                  :data
                  ((on-parse-fns :history))))
      (.catch (js/console.error))))

(defn replay-from-database!
  "`id` can be an a string in the format `id-author-title`,
   only the `id` at the front matters"
  [id]
  (-> (api/get-history id)
      (.then #(-> % api/get-data
                  js->clj
                  (get "history")
                  js/JSON.parse
                  js->clj
                  walk/keywordize-keys
                  ((on-parse-fns :history))))
      (.catch #(alert/create-alert! app-state :error "Could not find set for replay."))))

(defn replay-from-query-string! [query-string]
  (let [{url :replay id :set} (parse-query-string query-string)]
    (cond url (replay-from-url! (js/decodeURIComponent url))
          id (replay-from-database! id))))

(comment (replay-from-database! 1))

(comment (download-json! {1 2} :holi-boli))
