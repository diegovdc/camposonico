(ns algoradio.collab
  (:require ["axios" :as axios]
            [algoradio.collab.core :as collab]
            [algoradio.config :as config]
            [algoradio.player :as player]
            [algoradio.replayer.core :as replayer]
            [algoradio.state :refer [app-state]]
            [algoradio.websockets :refer [make-receiver send-message!]]
            [cljs.core]
            [cljs.core.async :as a]
            [clojure.edn :as edn]
            [clojure.walk :as walk]
            [haslett.client :as ws]))

(defn on-collab-event [msg]
  (js/console.debug "collab" msg)
  (condp = (-> msg :msg :type)
    :editor-change  (->> msg :msg :data
                         js/JSON.parse
                         js->clj
                         walk/keywordize-keys
                         (assoc {} :change)
                         (replayer/play-editor-change! app-state)
                         #_(js/console.log))
    :editor-eval (->> msg :msg :data edn/read-string
                      (replayer/play-editor-eval! app-state))
    :play (->> msg :msg :data edn/read-string
               ((fn [event]
                  (swap! collab/state assoc-in [::collab/player-map
                                                (event :audio-id)]
                         (replayer/play-play!
                          (event :audio-id)
                          (-> event
                              (assoc-in [:opts :send-to-collab?] false)
                              (assoc-in [:opts :originator-id]
                                        (event :originator-id)))
                          (event :type))))))
    :stop (->> msg :msg :data edn/read-string
               ((fn [event]
                  (let [{:keys [audio id]} (-> @collab/state
                                               ::collab/player-map
                                               (get (event :audio-id)))]
                    (player/stop! (event :audio-type) audio id false)))))
    (js/console.error "Unknown event type" (clj->js (msg :type)))))

(def event-buffer (a/chan))

(defn get-and-replay-history! []
  (-> (axios/get (str config/api "/collab-history/1"))
      (.then #(-> % js->clj walk/keywordize-keys :data))
      (.then (fn [events]
               (a/go
                 (->> events
                      ;; TODO what to do with play and editor eval events?
                      ;; Probably, allow `load` events, state events like variables, etc
                      (filter #(= (% :type) "editor-change"))
                      (mapv (fn [ev] {:msg (update ev :type keyword)}))
                      (a/>! event-buffer)))))))

(defn start-play-loop [event-buffer]
  (a/go-loop []
    (let [event-data (a/<! event-buffer)]
      (condp  = (type event-data)
        cljs.core/PersistentVector (doseq [ev event-data] (on-collab-event ev))
        cljs.core/PersistentHashMap (on-collab-event event-data)
        cljs.core/PersistentArrayMap (on-collab-event event-data)
        (js/console.error "Unknown type"
                          (clj->js (type event-data))
                          (clj->js event-data))))
    (recur)))

(defn router [msg]
  (condp = (:type msg)
    :ping (send-message! collab/conn :pong nil)
    :id (do (swap! collab/state assoc ::collab/ws-id (:msg msg))
            (get-and-replay-history!))
    :collab-event-broadcast (a/go (a/>! event-buffer msg))
    (js/console.error "Unknown message type:" (clj->js msg))))

(defn send-pong-every! [conn ms]
  (a/go-loop []
    (a/<! (a/timeout ms))
    (when (ws/connected? (a/<! conn))
      (send-message! collab/conn :pong nil))
    (recur)))


(defonce init-receiver
  (memoize (fn []
             (start-play-loop event-buffer)
             (make-receiver collab/conn #'router)
             (send-pong-every! collab/conn 10000))))
