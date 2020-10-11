(ns algoradio.collab
  (:require-macros  [cljs.core.async.macros :refer [go go-loop]])
  (:require ["axios" :as axios]
            [cljs.core.async.impl.protocols :refer [closed?]]
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
            [haslett.client :as ws]
            [clojure.string :as str]))

(defn on-collab-event [msg]
  (js/console.debug "collab" msg)
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

(defn play-loop-callback [event-data]
  (println "playloopcallback")
  (condp  = (type event-data)
    cljs.core/PersistentVector (doseq [ev event-data] (on-collab-event ev))
    cljs.core/PersistentHashMap (on-collab-event event-data)
    cljs.core/PersistentArrayMap (on-collab-event event-data)
    (js/console.error "Unknown type"
                      (clj->js (type event-data))
                      (clj->js event-data))))

(defn start-play-loop [event-buffer]
  (a/go-loop []
    (let [event-data (a/<! event-buffer)]
      (println "play loop")
      (#'play-loop-callback event-data))
    (recur)))

(def still-connected-buffer (a/chan))

(defn router [msg]
  (condp = (:type msg)
    :ping (send-message! collab/conn :pong nil)
    :still-connected? (do #_(println msg)
                          (go (a/>! still-connected-buffer msg)))
    :id (do (swap! collab/state assoc
                   ::collab/ws-id (-> msg :msg :client-id)
                   ::collab/available-sessions (-> msg :msg :available-sessions))
            #_(get-and-replay-history!))
    :start-session (do (println msg)
                       )
    :collab-event-broadcast (a/go (a/>! event-buffer msg))
    (js/console.error "Unknown message type:" (clj->js msg))))

(comment (-> @collab/state println))

(defn check-for-connection []
  (go-loop [attempts 0]
    (let [[val chan] (a/alts! [still-connected-buffer (a/timeout 2000)])]
      (cond
        (= chan still-connected-buffer) (do (js/console.debug "Still connected" val)
                                            (recur 0))
        (< attempts 3) (do (js/console.debug "Awaiting pong response")
                           (recur (inc attempts)))
        :else (do (js/console.error "WebSocket disconnected: should reconnect")
                  (recur (inc attempts)))
        ))))

(defonce checker (check-for-connection))

#_(a/go (println (let [[val chan] (a/alts! [a b] )]
                   (= a chan))))

#_(a/go (a/>! b :5))
(defn send-pong-every! [conn ms]
  (a/go-loop []
    (a/<! (a/timeout ms))
    (when (ws/connected? (a/<! conn))
      (send-message! collab/conn :pong {:client-id (@collab/state ::collab/ws-id)}))
    (recur)))

(defn validate-create-session [data]
  (let [missing-fields (->> data (filter (comp nil? second)) (map first))
        messages {:session-name "please give a name to you session"
                  :password "please set a password"
                  :username "please choose a username"}]
    (when-not (empty? missing-fields)
      (str "One or more fields in the form are empty: "
           (str/join ", " ((apply juxt missing-fields) messages))))) )

(defn validate-join-session [data]
  (let [missing-fields (->> (dissoc data :password)
                            (filter (comp
                                     #(or (nil? %) (empty? %))
                                     second))
                            (map first))
        messages {:session-name "please select a session"
                  :username "please choose a username"}]
    (when-not (empty? missing-fields)
      (str "One or more fields in the form are empty: "
           (str/join ", " ((apply juxt missing-fields) messages))))) )

#_(validate-join-session {:session-action (@app-state ::session-action)
                          :session-name 1
                          :password 2
                          :username 4})
(defn send-start-session!
  [data]
  (let [validation-message (case (data :session-action)
                             :create (validate-create-session data)
                             :join (validate-join-session data))]
    (if-not validation-message
      (send-message! collab/conn :start-session data
                     {:user-id (@collab/state ::collab/ws-id)})
      (println validation-message)))
  )



(defonce init-receiver
  (memoize (fn []
             (start-play-loop event-buffer)
             (make-receiver collab/conn #'router)
             (send-pong-every! collab/conn 1000))))

(init-receiver)

(defn login [app-state]
  (let [create? (= :create (@app-state ::session-action))
        join? (= :join (@app-state ::session-action))
        set-session-name  #(swap! app-state assoc
                                  ::session-name (-> % .-target .-value))
        set-password #(swap! app-state assoc
                             ::password (-> % .-target .-value))
        set-username #(swap! app-state assoc
                             ::username (-> % .-target .-value))
        send-start-session-button
        (fn [text]
          [:div {:class "collab-login__button-container"}
           [:button {:on-click #(send-start-session!
                                 {:session-action (@app-state ::session-action)
                                  :session-name (@app-state ::session-name)
                                  :password (@app-state ::password)
                                  :username (@app-state ::username)})}
            text]])]
    [:div {:class "collab-login"}
     [:div {:class "collab-login__container"}
      [:h2 "Camposonico Live"]
      [:div
       [:div {:class "collab-login__button-container"}
        [:button {:class (when create? "selected")
                  :on-click #(swap! app-state assoc ::session-action :create)}
         "Create a session"]
        [:button {:class (when join? "selected")
                  :on-click #(swap! app-state assoc ::session-action :join)}
         "Join an existing session"]]]
      (when create?
        [:div
         [:h3 "Creating a session"]
         [:p {:class "blue"} "By filling the following form and you will be provided with a link to share so other people can join in to the fun"]
         [:p [:small "(Note: no data will persisted into any database)"]]
         [:label "Session name:"
          [:input {:on-change set-session-name} ]]
         [:label "Password "
          [:small "(People with the password will be able to collaborate with writting the code)"]
          [:input {:on-change set-password :type "password"}]]
         [:label "Your name:"
          [:input {:on-change set-username}]]
         (send-start-session-button "Create session")])
      (when join?
        [:div
         [:h3 "Joining a session"]
         [:label "Choose a session:"
          [:span {:class "collab-login__select-container"}
           [:select {:on-change set-session-name}
            (map (fn [session-name] [:option {:key session-name} session-name])
                 (@collab/state ::collab/available-sessions))]]]
         [:label
          "Password "
          [:small "(Optional: people with the password will be able to collaborate with writting the code)"]
          [:input {:on-change set-password :type "password"}]]
         [:label "Your name:" [:input {:on-change set-username} ]]
         (send-start-session-button "Join session")])]]))
