(ns algoradio.fields
  (:require [algoradio.source-info :as sources]
            [algoradio.player :as player]
            [algoradio.colors :as colors]
            ["/js/index" :refer [isMobileOrTablet]]))

(declare on-field-change!)

(defn make-source-info-toggler []
  (let [prev-id (atom nil)] ;;
    (fn [app-state id sound]
      (let [to-background? (and (= @prev-id id)
                                (not (app-state ::sources/as-background?)))]
        (if to-background?
          (do (sources/as-background!? true)
              (sources/set-pause! false))
          (do (sources/as-background!? false)
              (sources/set-pause! true))))
      (sources/describe! sound)
      (reset! prev-id id))))

(def source-info-toggler (make-source-info-toggler))

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
            (when (isMobileOrTablet)
              [:span {:class "fields__mobile-buttons"}
               [:button {:on-click #(player/rand-stop! name)} "-"]
               [:button {:on-click #(player/user-play-sound! name)} "+"]])]
           [:div {:class "fields__field-color-container"}
            (map (fn [{id :id :as sound}]
                   [:div {:key id
                          :class "fields__field-color"
                          :style {:background-color (colors/get-color id 1)}
                          :on-click #(source-info-toggler app-state id sound)}])
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
