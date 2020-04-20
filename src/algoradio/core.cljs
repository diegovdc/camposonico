(ns algoradio.core
  (:require
   [algoradio.state :refer [app-state]]
   [cljs.user :refer [spy]]
   [reagent.core :as reagent]
   [algoradio.freesound :as freesound]
   [algoradio.player :as player]))

(defn fields
  [app-state]
  (let [freesounds (get app-state :freesounds)
        now-playing (get app-state ::player/fields-density)]
    (when freesounds
      (map (fn [name] [:p {:key name}
                      [:b name] ", "
                      "sonando: " (get now-playing name 0)
                      "/" (-> freesounds (get name 0) count)
                      [:button {:on-click #(player/rand-stop! name)} "-"]
                      [:button {:on-click #(player/user-play-sound! name)} "+"]])
           (keys freesounds)))))


(defn campo-sonoro []
  [:div
   [:h1 "Campo Sonoro/Radio algorítmica"]
   [:h3 {:style {:margin-bottom "5px"}}
    "Escribe el nombre de algún tipo de paisaje o \"tag\" relacionado (en inglés funciona mejor)."]
   [:div {:style {:margin-bottom "10px"}}
    [:small "Fuente de los sonidos: "
     [:a {:href "https://freesound.org"} "freesound.org"]]]
   [:input {:type "text"
            :style {:width "250px"}
            :placeholder "e.g. river, birds, amazon, felix blume..."
            :value (get @app-state ::search "")
            :on-change (fn [e]
                         (swap! app-state assoc ::search
                                (-> e .-target .-value)))}]
   [:button {:on-click (fn []
                         (let [search (@app-state ::search)]
                           (when
                               search
                             (-> (freesound/get-audios! app-state search)
                                   (.then #(player/user-play-sound! search))
                                   (.then #(swap! app-state
                                                  assoc ::search ""))))))}
    "Buscar"]
   [:div {:class "field-list"}
    (fields @app-state)]])

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
  (set! (.. js/window -sayHello) say-hello)
  (set! (.. js/window -resetText) set-text!))
