(ns algoradio.alert)

(declare close-alert!)

(defn main [app-state]
  "`type` can be `:success` or `:error`"
  (let [{:keys [type msg] :or {type :error}} (@app-state ::alert)]
    (when msg
      [:div {:class "alert"}
       [:span {:class "alert__close"
               :on-click #(close-alert! app-state)}
        [:span {:class "alert__close-x"} "x"]]
       [:div
        {:class (str "alert__container alert__" (name type))}
        [:div msg]]])))

(defn create-alert! [app-state type msg]
  (swap! app-state assoc ::alert {:type type :msg msg}))

(defn close-alert! [app-state]
  (swap! app-state assoc ::alert nil))

(comment te assoc ::alert {:type :success :msg "ups"})
