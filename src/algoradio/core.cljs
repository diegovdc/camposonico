(ns algoradio.core
  (:require
   [algoradio.about :as about]
   [algoradio.add-music :as add-music]
   [algoradio.archive :as archive]
   [algoradio.archive.sounds :as archive*]
   [algoradio.editor :as editor]
   [algoradio.editor-api :as editor-api]
   [algoradio.fields :as fields]
   [algoradio.icons :as icons]
   [algoradio.search :as search]
   [algoradio.source-info :as sources]
   [algoradio.state :refer [app-state]]
   [cljs.user :refer [spy]]
   [reagent.core :as reagent]))

(defn intro []
  [:div {:class "intro"}
   [:h1 "Campo Sonoro/Radio algorítmica"]
   [:p {:class "intro__p"}
    "Escribe el nombre de algún tipo de paisaje o \"tag\" relacionado (las búsquedas en inglés suelen arrojar más resultados)."]])

(defn campo-sonoro []
  (reagent/create-class
   {:component-did-mount (fn []
                           (editor-api/setup! app-state)
                           (sources/rand-info!))
    :reagent-render
    (fn []
      (js/console.debug "render")
      [:div
       {:on-key-up (fn [e]
                     (when (= 27 (.-keyCode e))
                       (sources/close! nil)))}
       #_(intro)
       [:div {:class "container main"}
        [:canvas {:id "hydra-canvas" :class "hydra-canvas"}]
        (editor/main app-state)
        [:div {:class "search"} (search/main app-state) (add-music/main app-state)]
        [:div {:class "fields"} (fields/main @app-state)]
        (sources/main app-state)
        [:button {:class "info-icon__container"
                  :on-click about/toggle-show-about} icons/info]
        (when (@app-state ::about/show-about?) (about/main archive*/sounds))]])}))

(defn start []
  (reagent/render-component [campo-sonoro]
                            (. js/document (getElementById "app"))))
(comment
  (reset! app-state)
  (-> @app-state :freesounds cljs.user/spy))

(defn ^:export init []
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
