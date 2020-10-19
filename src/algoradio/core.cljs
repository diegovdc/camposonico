(ns algoradio.core
  (:require ["/js/index" :refer [isMobileOrTablet]]
            [algoradio.about :as about]
            [algoradio.add-music :as add-music]
            [algoradio.alert :as alert]
            [algoradio.archive :as archive]
            [algoradio.archive.sounds :as archive*]
            [algoradio.chat :as chat]
            [algoradio.collab :as collab]
            [algoradio.collab.core :as collab-core]
            [algoradio.common :as common]
            [algoradio.editor :as editor]
            [algoradio.editor-api :as editor-api]
            [algoradio.fields :as fields]
            [algoradio.fs :as fs]
            [algoradio.history :as history]
            [algoradio.icons :as icons]
            [algoradio.instructions :as instructions]
            [algoradio.search :as search]
            [algoradio.source-info :as sources]
            [algoradio.state :refer [app-state]]
            [cljs.user :refer [spy]]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [reagent.core :as reagent]))



(defn intro []
  [:div {:class "intro"}
   [:h1 "Camposónico"]
   [:p {:class "intro__p"}
    "En la barra de búsqueda escribe el nombre de algún tipo de paisaje o \"tag\" relacionado (las búsquedas en inglés suelen arrojar más resultados)."]])
#_(println js/camposonicoVersion)
(defn campo-sonoro []
  (let [is-live? (str/includes? js/window.location.pathname "/live")]
    (reagent/create-class
     {:component-did-mount
      (fn []
        (editor-api/setup! app-state)
        (when is-live? (collab/init-receiver))
        (sources/rand-info!)
        (fs/replay-from-query-string! js/location.search))
      :reagent-render
      (fn []
        [:div
         {:on-key-up (fn [e] (when (= 27 (.-keyCode e)) (sources/close! nil)))}
         (when (isMobileOrTablet) (intro))
         [:div {:class "container main"}
          [:canvas {:id "hydra-canvas" :class "hydra-canvas"}]
          (cond
            (isMobileOrTablet)
            nil
            (not is-live?)
            (do (js/console.debug "STARTING SOLO SESSION")
                (editor/main app-state
                             is-live?
                             nil
                             collab-core/send-eval-event!))
            (and is-live? (@app-state ::collab/login-data))
            (do (js/console.debug "STARTING LIVE SESSION")
                (editor/main app-state
                             is-live?
                             (@app-state ::collab/login-data)
                             collab-core/send-eval-event!))
            :default nil)
          [:div {:class (str "search " (when (isMobileOrTablet) "is-mobile"))}
           (search/main app-state)
           (add-music/main app-state)]
          [:div {:class (str "fields " (when (isMobileOrTablet) "is-mobile"))}
           (fields/main @app-state)]
          (sources/main app-state)
          [:button {:class "info-icon__container"
                    :on-click about/toggle-show-about} icons/info]
          (when (@app-state ::about/show-about?) (about/main archive*/sounds))
          (fs/main app-state)
          (alert/main app-state)
          (history/save-template app-state)
          ;; TODO login to chat as well
          (when is-live? [chat/main])
          (when is-live? (collab/login app-state))
          #_(convocatoria/main :es)]])})))

(defn start []
  (reagent/render-component [campo-sonoro]
                            (. js/document (getElementById "app"))))
(comment
  (reset! app-state))

(defn ^:export init [opts]
  (let [opts* (js->clj opts)
        version (get opts* "version" "1.0.0")
        intro-text (get opts* "introText" #_(instructions/intro version))]
    (swap! app-state assoc ::editor/text intro-text))
  (if-let [lists (-> opts js->clj (get "lists")
                     js->clj
                     walk/keywordize-keys)]
    (common/set-as-freesound-queries! app-state lists))
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (swap! app-state #(-> %
                        (update ::editor/key inc)
                        (dissoc ::editor/instance)))
  (js/console.log "stop"))
