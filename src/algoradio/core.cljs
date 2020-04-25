(ns algoradio.core
  (:require
   [algoradio.state :refer [app-state]]
   [cljs.user :refer [spy]]
   [algoradio.archive :as archive]
   [reagent.core :as reagent]
   [algoradio.freesound :as freesound]
   [algoradio.player :as player]
   [algoradio.editor :as editor]
   [algoradio.colors :refer [colors]]
   ["react-codemirror" :as react-codemirror]
   ["codemirror/mode/javascript/javascript"]
   ["codemirror/addon/display/fullscreen"]
   [clojure.string :as str]
   [clojure.set :as set]))

(defn get-audios!
  ([query]
   (-> (freesound/get-audios! app-state query)
       (.then #(player/user-play-sound! query))
       (.then #(swap! app-state assoc ::search "")))))

(defn load!
  ([query]
   (-> (freesound/get-audios! app-state query)
       (.then #(swap! app-state assoc ::search "")))))

(defn intro []
  [:div {:class "intro"}
   [:h1 "Campo Sonoro/Radio algorítmica"]
   [:p {:class "intro__p"}
    "Escribe el nombre de algún tipo de paisaje o \"tag\" relacionado (las búsquedas en inglés suelen arrojar más resultados)."]])

(defn do-search! [e]
  (when (= 13 #_enter (.-keyCode e))
    (let [search (@app-state ::search)]
      (when search (get-audios! search)))))

(defn search []
  [:div
   [:div {:class "search__input-container"}
    [:input {:class "search__input"
             :type "text"
             :placeholder "e.g. river, birds, amazon, felix blume..."
             :value (get @app-state ::search "")
             :on-change (fn [e]
                          (swap! app-state assoc ::search
                                 (-> e .-target .-value)))
             :on-key-up do-search!}]
    [:button {:class "search__button"
              :on-click (fn []
                          (let [search (@app-state ::search)]
                            (when search (get-audios! search))))}
     "Buscar"]]
   [:div {:class "search__source-container"}
    [:div {:class "search__source"}
     [:small "Fuente de los sonidos: "
      [:a {:href "https://freesound.org" :target "_blank"} "freesound.org"]]]
    ]])

(defn agregar-musica []
  [:div {:class "search__checkbox-container"}
   [:label
    [:input {:class "search__checkbox"
             :type "checkbox"
             :checked (get @app-state ::archive/should-play? false)
             :on-change (fn [_]
                          (swap! app-state update
                                 ::archive/should-play? not)
                          (when (@app-state ::archive/should-play?)
                            (player/init-archive! 0 0)))}]
    [:span {:class "search__checkbox-label"} "Agregar música"]]])

(defn get-int-value [event]
  (-> event  .-target .-value  js/parseInt))
(do
  (defn on-field-change!*
    [name old-value new-value]
    (let [diff (- new-value old-value)
          add? (> diff 0)]
      (doseq [i (range 0 (js/Math.abs diff))]
        (js/console.log i add?)
        (if add?
          (player/user-play-sound! name)
          (player/rand-stop! name)))))

  (defn on-field-change! [now-playing name e]
    (on-field-change!*
     name
     (get now-playing name 0)
     (get-int-value e))))

(defonce color-offset (rand-int 100))
(defn get-color [sound-id]
  (nth colors (mod (+ color-offset sound-id)
                   (dec (count colors))) "#000"))

(defn describe-source!
  ([sound]
   (describe-source! sound (get @app-state
                                ::source-info-as-background?
                                false)))
  ([sound as-background?]
   (swap! app-state
          #(-> %
               (assoc ::source-info sound)
               (assoc ::source-info-as-background? as-background?)))))

(defn fields
  [app-state]
  (let [freesounds (get app-state :freesounds)
        now-playing (get app-state ::player/fields-density) ;; static user defined, aims for eventual consistency
        now-playing* (->> (get app-state ::player/now-playing)
                          (group-by :type))]
    [:div {:class "fields__field-container"}
     (when freesounds
       (map
        (fn [name]
          [:div {:key name}
           [:div {:class "fields__field"}
            [:b {:class "fields__field-name"} name]
            [:span [:input
                    {:class "fields__input"
                     :type "number"
                     :value (get now-playing name 0)
                     :on-change (partial on-field-change!
                                         now-playing name)}]
             [:span {:class "fields__separator"}"/"]
             (-> freesounds (get name 0) count)]
            #_[:button {:on-click #(player/rand-stop! name)} "-"]
            #_[:button {:on-click #(player/user-play-sound! name)} "+"]]
           [:div {:class "fields__field-color-container"}
            (map (fn [{id :id :as sound}]
                   [:div {:key (sound :id)
                          :class "fields__field-color"
                          :style {:background-color (get-color id)}
                          :on-click #(describe-source! sound)}])
                 (get now-playing* name))]])
        (keys freesounds)))]))

(defn close-source-info! [_]
  (swap! app-state assoc ::source-info nil))

(defn source-info []
  (let [{:keys [sound id type src]} (get @app-state ::source-info)
        as-background? (get @app-state ::source-info-as-background? false)
        position (get @app-state ::source-info-position "bottom")]
    (when (spy "sinfo" sound)
      [:div {:class (str "source-info "
                         (when as-background? " as-background ")
                         (if (#{"full" "completa"} position)
                           "center"
                           position))
             :style (when (#{"full" "completa"} position)
                      {:background-color (get-color id)})}
       [:div {:class "source-info__container"
              :style {:background-color (get-color id)}}
        [:span {:class "source-info__close"
                :on-click close-source-info!} "X"]
        [:p [:span [:b "descripción: "] (sound :description)]]
        [:p [:span [:b "tags: "] (str/join ", " (sound :tags))]]
        [:p [:span [:b "duración: "] (js/parseInt (sound :duration)) "s"]]
        [:p [:span [:b "autor: "] (sound :username)]]
        [:a {:href (sound :url) :class "link" :target "_blank"} [:span "[Link]"]]]])))

(defn editor []
  [:div {:key (get @app-state ::editor/key)
         :id "editor-container"
         :class "editor"
         :on-key-down
         (fn [e]
           (when (and (.-ctrlKey e)
                      (= 13 #_enter (.-keyCode e)))
             (editor/eval! (editor/get-cm app-state))))}
   [:> react-codemirror
    {
:ref  (fn [ref] (when-not (@app-state ::editor/instance)
                      (swap! app-state assoc ::editor/instance ref)))
     :options {:theme "oceanic-next"
               :fullScreen true}
     :autoSave false
     :value (get @app-state ::editor/text "")
     :on-change #(swap! app-state assoc ::editor/text %)}]])

(defn campo-sonoro []
  (reagent/create-class
   {:component-did-mount (fn [] (js/console.log "mounted"))
    :reagent-render
    (fn []
      (js/console.log "render")
      [:div
       {:on-key-up (fn [e]
                     (when (= 27 (.-keyCode e))
                       (close-source-info! nil)))}
       #_(intro)
       [:div {:class "container main"}
        (editor)
        [:div {:class "search"} (search) (agregar-musica)]
        [:div {:class "fields"} (fields @app-state)]
        (source-info)]])}))

(defn info-as-background!?
  [bool]
  (when-not bool (swap! app-state assoc ::source-info-position "bottom"))
  (swap! app-state assoc ::source-info-as-background? bool))

(defn set-info-position! [position]
  (let [available-positions #{"abajo" "izquierda" "derecha" "centro"
                              "arriba" "top" "bottom" "left"
                              "right" "center" "full" "completa"}]
    (when (available-positions position)
      (swap! app-state assoc ::source-info-position position))))

(defn rand-info!
  ([] (rand-info! 7000))
  ([timeout]
   (let [id (@app-state ::source-info-rand-interval)]
     (when id (js/clearInterval id)))
   (swap! app-state assoc ::source-info-rand-interval
          (js/setInterval
           #(let [now-playing (-> (get @app-state ::player/now-playing)
                                  )]
              (when (seq now-playing)
                (-> now-playing rand-nth  spy describe-source!))
              nil )
           timeout))))

(defn remove-comment-lines! []
  (let [text (-> (get @app-state ::editor/text "")
                 (str/split "\n")
                 (->> (remove #(re-find #"^//" %))
                      (drop-while empty?)
                      (str/join "\n"))
                 spy)]
    [text (-> (get @app-state ::editor/text "")
              (str/split "\n")
              (->> (remove #(re-find #"^//" %))
                   (drop-while empty?)
                   (str/join "\n"))
              spy)]
    (swap! app-state #(-> %
                          (assoc ::editor/text text)
                          (update ::editor/key inc)))
    nil))


(defn stop-rand-info! []
  (let [id (@app-state ::source-info-rand-interval)]
    (when id (js/clearInterval id))))

(set-info-position! "completa")

(set! (.. js/window -load) load!)
(set! (.. js/window -showInfo) rand-info!)
(set! (.. js/window -infoAsBackground) info-as-background!?)
(set! (.. js/window -setInfoPosition) set-info-position!)
(set! (.. js/window -clearComments) remove-comment-lines!)
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

(-> (editor/get-cm app-state) spy)
