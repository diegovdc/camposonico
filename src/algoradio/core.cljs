(ns algoradio.core
  (:require
   [algoradio.state :refer [app-state]]
   [cljs.user :refer [spy]]
   [algoradio.archive :as archive]
   [reagent.core :as reagent]
   [algoradio.freesound :as freesound]
   [algoradio.player :as player]
   [algoradio.editor :as editor]
   ["react-codemirror" :as react-codemirror]
   ["codemirror/mode/javascript/javascript"]))

(defn get-audios!
  [query]
  (-> (freesound/get-audios! app-state query)
      (.then #(player/user-play-sound! query))
      (.then #(swap! app-state assoc ::search ""))))

(defn intro []
  [:p {:class "intro__p"}
   "Escribe el nombre de algún tipo de paisaje o \"tag\" relacionado (las búsquedas en inglés suelen arrojar más resultados)."])

(defn search []
  [:div
   [:input {:class "sidebar__input"
            :type "text"
            :placeholder "e.g. river, birds, amazon, felix blume..."
            :value (get @app-state ::search "")
            :on-change (fn [e]
                         (swap! app-state assoc ::search
                                (-> e .-target .-value)))}]
   [:div {:class "sidebar__button-container"}
    [:div {:class "sidebar__source"}
     [:small "Fuente de los sonidos: "
      [:a {:href "https://freesound.org"} "freesound.org"]]]
    [:button {:class "sidebar__button"
              :on-click (fn []
                          (let [search (@app-state ::search)]
                            (when search (get-audios! search))))}
     "Buscar"]]])

(defn agregar-musica []
  [:div {:class "sidebar__checkbox-container"}
   [:label
    [:input {:class "sidebar__checkbox"
             :type "checkbox"
             :checked (get @app-state ::archive/should-play? false)
             :on-change (fn [_]
                          (swap! app-state update
                                 ::archive/should-play? not)
                          (when (@app-state ::archive/should-play?)
                            (player/init-archive! 0 0)))}]
    [:span {:class "sidebar__checkbox-label"} "Agregar música"]]])
(do
  (defn on-field-change!
    [name old-value new-value]
    (let [diff (- new-value old-value)
          add? (> diff 0)]
      (doseq [i (range 0 (js/Math.abs diff))]
        (js/console.log i add?)
        (if add?
          (player/user-play-sound! name)
          (player/rand-stop! name)))))
  (on-field-change! "ocean" 2 4))

(defn get-int-value [event]
  (-> event  .-target .-value js/parseInt))


(defn fields
  [app-state]
  (let [freesounds (get app-state :freesounds)
        now-playing (get app-state ::player/fields-density)]
    [:div {:class "field-list"}
     (when freesounds
       (map (fn [name] [:div {:class "fields__field" :key name}
                       [:b name]
                       [:div [:input
                              {:class "fields__input"
                               :type "number"
                               :value (now-playing name)
                               :on-change
                               (fn [e]
                                 (on-field-change!
                                  name
                                  (now-playing name)
                                  (get-int-value e)))
                               #_ #_:on-keyup (partial on-field-change (get  now-playing name 0))}]
                        [:span {:class "fields__separator"}"/"]
                        (-> freesounds (get name 0) count)]
                       #_[:button {:on-click #(player/rand-stop! name)} "-"]
                       #_[:button {:on-click #(player/user-play-sound! name)} "+"]])
            (keys freesounds)))]))

(defn editor []
  [:div {:id "editor-container"
         :class "editor"
         :on-key-down
         (fn [e]
           (when (and (.-ctrlKey e)
                      (= 13 #_enter (.-keyCode e)))
             (editor/eval! (editor/get-cm app-state))))}
   [:> react-codemirror
    {:ref #(swap! app-state assoc ::editor/instance %)
     :options {:theme "oceanic-next"}
     :autoSave false
     :value (get @app-state ::editor/text "")
     :on-change #(swap! app-state assoc ::editor/text %)}]])

(defn campo-sonoro []
  (reagent/create-class
   {:component-did-mount (fn [] (js/console.log "mounted"))
    :reagent-render
    (fn []
      [:div
       [:h1 "Campo Sonoro/Radio algorítmica"]
       (intro)
       [:div {:class "container main"}
        (editor)
        [:div {:class "sidebar"} (search) (agregar-musica) (fields @app-state)]]])}))



(set! (.. js/window -load) (partial freesound/get-audios! app-state))
(set! (.. js/window -stop) player/rand-stop!)
(set! (.. js/window -play) player/user-play-sound!)
(set! (.. js/window -traerAudios) get-audios!)


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
  (js/console.log "stop"))

(comment
  (defn say-hello [name] (str "Hello " name))
  (defn set-text! [text] (swap! app-state assoc :text text) nil)
  (set! (.. js/window -debounce) debounce)
  (-> @app-state ::archive/should-play? spy ))
