(ns algoradio.search
  (:require [algoradio.freesound :as freesound]
            [algoradio.player :as player]))

(declare do-search! get-audios! loading?)

(defn main [app-state]
  [:div
   (if-not (loading? @app-state)
     [:div {:class "search__input-container"}
      [:input {:class "search__input"
               :type "text"
               :placeholder "e.g. river, birds, amazon, desert..."
               :value (get @app-state ::search "")
               :on-change (fn [e]
                            (swap! app-state assoc ::search
                                   (-> e .-target .-value)))
               :on-key-up (partial do-search! app-state)}]
      [:button {:class "search__button"
                :on-click (fn []
                            (let [search (@app-state ::search)]
                              (when search (get-audios! app-state search))))}
       "Buscar"]]
     [:p {:class "search__loading"} "Loading..."])
   [:div {:class "search__source-container"}
    [:div {:class "search__source"}
     [:small "Fuente de los sonidos: "
      [:a {:href "https://freesound.org" :target "_blank"} "freesound.org"]]]
    ]])

(defn do-search! [app-state e]
  (when (= 13 #_enter (.-keyCode e))
    (let [search (@app-state ::search)]
      (when search (get-audios! app-state search)))))

(defn get-audios!
  ([app-state query]
   (-> (freesound/get-audios! app-state query)
       (.then #(player/user-play-sound! query))
       (.then #(swap! app-state assoc ::search "")))))


(defn loading? [state]
  (-> state ::freesound/loading-queries empty? not))
