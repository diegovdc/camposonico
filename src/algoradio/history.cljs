(ns algoradio.history
  (:require [algoradio.api :as api]
            [algoradio.alert :as alert]
            [algoradio.inputs :as inputs]))

(defn add-play! [app-state sound audio-id]
  (swap! app-state update ::history conj
         (assoc sound
                :event_type :play
                :audio_id audio-id
                :timestamp (js/Date.now))))

(defn add-stop! [app-state audio-id type]
  (swap! app-state update ::history conj
         {:event_type :stop
          :audio_id audio-id
          :type type
          :timestamp (js/Date.now)}))

(defn add-volume-change! [app-state audio-id volume-level]
  (swap! app-state update ::history conj
         {:event_type :volume
          :audio_id audio-id
          :volume_level volume-level
          :timestamp (js/Date.now)}))

(defn add-editor-change! [app-state editor-id change]
  (swap! app-state update ::history conj
         {:event_type :editor_change
          :editor_id editor-id
          :change change
          :timestamp (js/Date.now)}))

(defn add-editor-eval! [app-state editor-id mark]
  (swap! app-state update ::history conj
         {:event_type :editor_eval
          :editor_id editor-id
          :mark mark
          :timestamp (js/Date.now)}))

(defn prepare-history
  "Order history by timestamp, ascending,
  and add `:interval` between each event"
  [history]
  (let [history* (reverse history)
        initial-time (:timestamp (first history*))]
    (:history
     (reduce (fn [{:keys [history prev-time]} event]
               (let [current-time (event :timestamp)
                     interval (- current-time prev-time)]
                 {:history (conj history (assoc event :interval interval))
                  :prev-time current-time}))
             {:history [] :prev-time initial-time}
             history*))))

(defn get-history! [app-state]
  (#'prepare-history (get @app-state ::history)))

;;;;;;;;;;;;;;;
;; save history
;;;;;;;;;;;;;;;
(def history-metadata (atom {}))

(defn on-history-input-change [ev id]
  (swap! history-metadata assoc id (-> ev .-target .-value)))

(defn show-save-template! [app-state]
  (swap! app-state assoc ::save? true))

(defn hide-save-template! [app-state]
  (swap! app-state assoc ::save? false))

(defn save-history! [app-state metadata]
  (-> (api/post-history (assoc metadata :history (get-history! app-state)))
      (.then (fn [_]
               (alert/create-alert! app-state :success "Your performance has been saved")
               (reset! history-metadata {})
               (hide-save-template! app-state)))
      (.catch #(alert/create-alert! app-state :error (api/get-error %)))))

(defn on-history-submit [app-state event]
  (.preventDefault event)
  (save-history! app-state @history-metadata))

(defn save-template [app-state]
  (when (@app-state ::save?)
    [:div {:class "history-save"}
     [:div {:class "history-save__container"}
      [:h2 {:class "history-save__title"} "Save your performance"]
      [:p {:class "mb-10"} "This operation will save your performance to Camposónico's database. All fields are optional."]
      [:form {:name "history" :class "history-save__form"}
       (inputs/input :text :title "Title" on-history-input-change)
       (inputs/input :text :author "Author" on-history-input-change)
       (inputs/input :text :tags "Tags (comma separated)" on-history-input-change)
       (inputs/submit "Save" (partial on-history-submit app-state))]]]))


(comment (show-save-template! algoradio.state/app-state))
