(ns algoradio.add-music
  (:require [algoradio.archive :as archive]
            [algoradio.player :as player]
            [algoradio.colors :as colors]
            [algoradio.source-info :as sources]))

(defn main [app-state]
  (let [sound (archive/get-first-playing-sound
               (get @app-state ::player/now-playing []))]
    [:div {:class "search__checkbox-container"}
     [:label
      [:input {:class "search__checkbox"
               :type "checkbox"
               :checked (get @app-state ::archive/should-play? false)
               :on-change (fn [_]
                            (swap! app-state update
                                   ::archive/should-play? not)
                            (if (@app-state ::archive/should-play?)
                              (player/init-archive! 0 0)
                              (player/stop-archive!)))}]
      [:span {:class "search__checkbox-label"} "Agregar música"]]
     (when sound
       [:div {:key (sound :id)
              :class "fields__field-color"
              :style {:background-color (colors/get-color (sound :id))}
              :on-click #(do (sources/describe! sound)
                             (sources/as-background!? false)
                             (sources/set-pause! true))}])]))
