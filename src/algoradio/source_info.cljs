(ns algoradio.source-info
  (:require [algoradio.state :refer [app-state]]
            [algoradio.colors :refer [get-color]]
            [algoradio.config :as config]
            [algoradio.history :as history]
            [cljs.user :refer [spy]]
            [algoradio.player :as player]
            [clojure.string :as str]))

(declare as-background!? set-pause! describe! close!
         in-selections-list? add-to-selections! remove-from-selections!
         stopping-list)

(defn main [app-state]
  (let [{:keys [sound id audio type]} (get @app-state ::info)
        {:keys [name description tags duration username author url]} sound
        as-background? (get @app-state ::as-background? true)
        position (get @app-state ::position "bottom")
        bg-opacity (if-not as-background? 1
                           (get @app-state ::background-opacity 0.5))
        color (get-color id bg-opacity)
        volume (get-in @app-state [::player/volumes id])]
    (when (and sound (.playing audio))
      (js/console.debug "Viewing audio with id:" id)
      [:div {:class (str "source-info "
                         (when as-background? " as-background "))
             :style {:background-color color}
             :on-double-click #(as-background!? true)}
       [:div {:class (str "source-info__container " position)
              :style {:background-color color}}
        (when (not as-background?)
          [:div
           [:span {:class "source-info__send-to-back"
                   :on-click #(do (as-background!? true)
                                  (set-pause! false))} "_"]])
        (when name
          [:p [:span [:b "nombre: "] name]])
        (when description
          [:p [:span [:b "descripción: "] description]])
        (when (not (empty? tags))
          [:p [:span [:b "tags: "] (str/join ", " tags)]])
        (when duration
          [:p [:span [:b "duración: "] (js/parseInt duration) "s"]])
        (when (or username author)
          [:p [:span [:b "autor: "] (or username author)]])
        (when url
          (-> url (str/split #",")
              (->> (map (fn [url*]
                          [:p {:key url*} [:span
                                           [:a {:class "link"
                                                :href url*
                                                :key url*
                                                :target "_blank"}
                                            url*]]])))))
        (when-not as-background?
          [:div {:class "df ac"
                 ;; :style {:min-height "22px"}
                 }
           (if-not (in-selections-list? app-state sound)
             [:p [:span [:b {:class "source-info__add-to-list"
                             :on-click #(add-to-selections! app-state sound)}
                         "+ (add to selections list)"]]]
             [:p [:span [:b {:class "source-info__add-to-list"
                             :on-click #(remove-from-selections! app-state sound)}
                         "- (remove from selections list)"]]])
           [:p {:class "ml-10"}
            (if-not (@stopping-list id)
              [:b {:class "curp"
                   :style {:fontSize "17px"}
                   :on-click #(do (player/stop! type audio id)
                                  (as-background!? true)
                                  (set-pause! false)
                                  (swap! stopping-list conj id))}
               "■"])]])
        (when (@stopping-list id) [:p [:span "Stopping..."]])
        (when-not as-background?
          [:input {:key id
                   :class "range-input" :type "range"
                   :min 0 :max 1 :step 0.01
                   :value volume
                   :on-change
                   (fn [ev]
                     (if (.playing audio)
                       (let [vol (-> ev .-target .-value)]
                         (player/set-volume! id audio vol))))}])]])))

#_(-> @app-state ::info :audio .-_sounds js/console.log)

(defn as-background!?
  ([bool] (as-background!? bool 0.5))
  ([bool opacity]
   (when-not bool (swap! app-state assoc ::position "bottom"))
   (swap! app-state assoc
          ::as-background? bool
          ::background-opacity opacity)))

(defn set-position! [position]
  (let [available-positions #{"abajo" "izquierda" "derecha" "centro"
                              "arriba" "top" "bottom" "left"
                              "right" "center" "full" "complete"}]
    (when (available-positions position)
      (swap! app-state assoc ::position position))))

(defn rand-info!
  ([] (rand-info! 7000))
  ([timeout]
   (let [id (@app-state ::rand-interval)]
     (when id (js/clearInterval id)))
   (swap! app-state assoc ::rand-interval
          (js/setInterval
           #(let [now-playing (get @app-state ::player/now-playing)
                  pause-change? (get @app-state ::paused? false)]

              (when (and (not pause-change?) (seq now-playing))
                (-> now-playing rand-nth  spy describe!)))
           timeout))))

(defn describe!
  ([sound] (describe! sound (get @app-state ::as-background? true)))
  ([sound as-background?]
   (swap! app-state
          #(-> %
               (assoc ::info sound)
               (assoc ::as-background? as-background?)))))

(defn close! [_]
  (swap! app-state assoc ::info nil))

(defn set-pause!
  "Pause random change"
  [bool]
  (swap! app-state assoc ::paused? bool))

(defn in-selections-list? [app-state sound]
  (->> @app-state ::selection-list
       (map :url)
       (#((set %) (sound :url)))
       nil? not))

(defn add-to-selections! [app-state sound]
  (swap! app-state update ::selection-list conj sound))

(defn remove-from-selections! [app-state sound]
  (swap! app-state update ::selection-list
         (fn [selections]
           (remove #(= (% :url) (sound :url))
                   selections))))

(def stopping-list (atom #{}))
