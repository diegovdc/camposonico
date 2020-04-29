(ns algoradio.source-info
  (:require [algoradio.state :refer [app-state]]
            [algoradio.colors :refer [get-color]]
            [cljs.user :refer [spy]]
            [algoradio.player :as player]
            [clojure.string :as str]))

(declare as-background!? set-pause! describe! close!)

(defn main [app-state]
  (let [{:keys [sound id type src]} (get @app-state ::info)
        {:keys [description tags duration username author url]} sound
        as-background? (get @app-state ::as-background? false)
        position (get @app-state ::position "bottom")]
    (when sound
      [:div {:class (str "source-info "
                         (when as-background? " as-background "))
             :style {:background-color (get-color id)}}
       [:div {:class (str "source-info__container " position)
              :style {:background-color (get-color id)}}
        (when (not as-background?)
          [:div
           [:span {:class "source-info__close"
                   :on-click close!} "X"]
           [:span {:class "source-info__send-to-back"
                   :on-click #(do (as-background!? true)
                                  (set-pause! false))} "Mandar al fondo"]])
        (when description
          [:p [:span [:b "descripción: "] description]])
        (when (not (empty? tags))
          [:p [:span [:b "tags: "] (str/join ", " tags)]])
        (when duration
          [:p [:span [:b "duración: "] (js/parseInt duration) "s"]])
        (when (or username author)
          [:p [:span [:b "autor: "] (or username author)]])
        (when url
          [:a {:href url :class "link" :target "_blank"} [:span "["url"]"]])]])))

(defn as-background!?
  [bool]
  (when-not bool (swap! app-state assoc ::position "bottom"))
  (swap! app-state assoc ::as-background? bool))

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
  ([sound]
   (describe! sound (get @app-state
                                ::as-background?
                                false)))
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
