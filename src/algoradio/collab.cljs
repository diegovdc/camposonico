(ns algoradio.collab
  (:require [algoradio.editor :as editor]
            [algoradio.replayer.core :refer [play-editor-change! play-editor-eval!]]
            [algoradio.state :refer [app-state]]
            [algoradio.websockets :refer [make-receiver send-message!]]
            [cljs.core.async :as a]
            [algoradio.collab.core :as collab]
            [clojure.edn :as edn]
            [clojure.walk :as walk]
            [algoradio.replayer.core :as replayer]
            [algoradio.player :as player]))

(comment
  (a/go (println ((a/<! conn) :source)))
  (editor/get-cm! app-state))

(defn router [msg]
  (condp = (:type msg)
    :ping (send-message! collab/conn :pong nil)
    :id (swap! collab/state assoc ::collab/ws-id (:msg msg))
    :collab-event-broadcast
    (do
      (js/console.log "collab" msg)
      (condp = (-> msg :msg :type)
        :editor-change  (->> msg :msg :data
                             js/JSON.parse
                             js->clj
                             walk/keywordize-keys
                             (assoc {} :change)
                             (replayer/play-editor-change! app-state)
                             #_(js/console.log))
        :editor-eval (->> msg :msg :data edn/read-string (replayer/play-editor-eval! app-state))
        :play (->> msg :msg :data edn/read-string
                   ((fn [event]
                      (println "=======================" event)
                      (swap! collab/state assoc-in [::collab/player-map (event :audio-id)]
                             (replayer/play-play!
                              (event :audio-id)
                              (-> event
                                  (assoc-in [:opts :send-to-collab?] false)
                                  (assoc-in [:opts :originator-id] (event :originator-id)))
                              (event :type))))))
     :stop (->> msg :msg :data edn/read-string
                   ((fn [event]
                      (println "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" (event :audio-id) (event :originator-id))
                      (let [{:keys [audio id]} (-> @collab/state
                                                   ::collab/player-map
                                                   (get (event :audio-id)))]
                        (player/stop! (event :audio-type) audio id false)))))
        nil))
    (js/console.error "Unknown message type:" msg)))

(swap! collab/state assoc-in [::collab/player-map "aaaaaaa"] {})
(defonce init-receiver (memoize (fn [] (make-receiver collab/conn #'router))))
