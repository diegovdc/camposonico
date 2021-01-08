(ns algoradio.collab.init
  (:require ["axios" :as axios]
            [algoradio.alert :as alert]
            [algoradio.collab.core :as collab]
            [algoradio.config :as config]
            [algoradio.player :as player]
            [algoradio.replayer.core :as replayer]
            [algoradio.state :refer [app-state]]
            [algoradio.websockets :refer [make-receiver send-message!]]
            cljs.core
            [cljs.core.async :as a]
            [clojure.edn :as edn]
            [clojure.walk :as walk]
            [haslett.client :as ws])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defn on-collab-event [msg]
  (js/console.debug "collab event" msg (-> msg :event-type))
  (condp = (-> msg :event-type)
    :editor-change  (->> msg :data
                         js/JSON.parse
                         js->clj
                         walk/keywordize-keys
                         (assoc {} :change)
                         (replayer/play-editor-change! app-state)
                         #_(js/console.log))
    :editor-eval (->> msg :data edn/read-string
                      (replayer/play-editor-eval! app-state))
    :play (->> msg :data edn/read-string
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
    :stop (->> msg :data edn/read-string
               ((fn [event]
                  (let [{:keys [audio id]} (-> @collab/state
                                               ::collab/player-map
                                               (get (event :audio-id)))]
                    (player/stop! (event :audio-type) audio id false)))))
    (js/console.error "Unknown event type" (clj->js (msg :event-type)))))

(defonce event-buffer (a/chan))

(defn get-and-replay-history! []
  (-> (axios/get (str config/api "/collab-history/1"))
      (.then #(-> % js->clj walk/keywordize-keys :data))
      (.then (fn [events]
               (a/go
                 (let [data (try (->> events
                                      ;; TODO what to do with play and editor eval events?
                                      ;; Probably, allow `load` events, state events like variables, etc
                                      (filter #(= (% :type) "editor-change"))
                                      (mapv (fn [ev] {:msg (update ev :type keyword)})))
                                 (catch js/Error e (js/console.error e)))]
                   (when data (a/>! event-buffer data))))))))

(defn play-loop-callback [event-data]
  (condp  = (type event-data)
    cljs.core/PersistentVector (doseq [ev event-data] (on-collab-event ev))
    cljs.core/PersistentHashMap (on-collab-event event-data)
    cljs.core/PersistentArrayMap (on-collab-event event-data)
    (js/console.error "Unknown type"
                      (clj->js (type event-data))
                      (clj->js event-data))))

(defn start-play-loop [event-buffer]
  (a/go-loop []
    (try
      (let [event-data (a/<! event-buffer)]
        (js/console.debug "playloop event")
        (#'play-loop-callback event-data))
      (catch js/Error e (js/console.log e)))
    (recur)))

(def still-connected-buffer (a/chan))

(defn router [msg]
  (condp = (:type msg)
    :ping (send-message! (@collab/state ::collab/conn) :pong nil)
    :still-connected? (go (a/>! still-connected-buffer msg))
    :id (do (js/console.debug "COLLAB CONNECTED"(-> msg :msg :available-sessions))
            (swap! collab/state assoc
                   ::collab/ws-id (-> msg :msg :client-id)
                   ::collab/available-sessions (-> msg :msg :available-sessions)))
    :start-session (do (println msg (msg :success?))
                       (if (true? (msg :success?))
                         (do
                           (swap! app-state assoc ::login-data msg)
                           (js/window.history.pushState
                            nil
                            "Camposonico live session"
                            (collab/make-collab-url (:session-path msg))))
                         (alert/create-alert! app-state :error (msg :body))))
    :chat (when (and (msg :username) (> (count (msg :message)) 0))
            (swap! collab/state update :algoradio.chat/messages
                   conj (assoc msg :received-timestamp (js/Date.now))))
    :collab-event-broadcast (do (js/console.debug "Received event" msg)
                                (a/go (a/>! event-buffer msg)))
    (js/console.error "Unknown message type:" (clj->js msg))))


(comment (-> @collab/state))

(defn check-for-connection []
  (go-loop [attempts 0]
    (let [[val chan] (a/alts! [still-connected-buffer (a/timeout 10000)])]
      (cond
        (= chan still-connected-buffer) (do (js/console.debug "Still connected" val)
                                            (recur 0))
        (< attempts 3) (do (js/console.debug "Awaiting pong response")
                           (recur (inc attempts)))
        :else (do (js/console.error "WebSocket disconnected: should reconnect")
                  (recur (inc attempts)))))))

(defn send-pong-every! [conn ms]
  (a/go-loop []
    (try
      (do
        (a/<! (a/timeout ms))
        (when (ws/connected? (a/<! conn))
          (send-message! (@collab/state ::collab/conn)
                         :pong {:client-id (@collab/state ::collab/ws-id)})))
      (catch js/Error e (js/console.error e)))
    (recur)))


(defonce init-receiver
  (memoize (fn []
             (js/console.debug "Initing receiver")
             (a/go-loop [connected? false]
               (if-not connected?
                 (let [conn (collab/create-connection)]
                   (if (ws/connected? (a/<! conn))
                     (do
                       (swap! collab/state assoc ::collab/conn conn)
                       (recur true))
                     (do
                       (js/console.debug "[ws] Failed to connect...")
                       (a/<! (a/timeout 2000))
                       (js/console.debug "[ws] TRYING TO CONNECT AGAIN...")
                       (recur false)))))
               (do
                 (js/console.debug "[ws] WEBSOCKET CONNECTED!")
                 (check-for-connection)
                 (start-play-loop event-buffer)
                 (make-receiver (@collab/state ::collab/conn) #'router)
                 (send-pong-every! (@collab/state ::collab/conn) 10000))))))

(comment
  (init-receiver)
  (send-start-session!
   {:session-action (@app-state ::session-action)
    :session-name (@app-state ::session-name)
    :password (@app-state ::password)
    :username (@app-state ::username)}))
