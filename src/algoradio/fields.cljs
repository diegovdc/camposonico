(ns algoradio.fields
  (:require [algoradio.source-info :as sources]
            [algoradio.player :as player]
            [algoradio.colors :as colors]))

(declare on-field-change!)

(defn main
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
                          :style {:background-color (colors/get-color id)}
                          :on-click #(do (sources/describe! sound)
                                         (sources/as-background!? false)
                                         (sources/set-pause! true))}])
                 (get now-playing* name))]])
        (keys freesounds)))]))

(defn get-int-value [event]
  (-> event  .-target .-value  js/parseInt))

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
   (get-int-value e)))
