(ns algoradio.collab.core
  (:require [algoradio.websockets :refer [make-conn send-message!] :as ws]
            [algoradio.config :as config]
            [reagent.core :as r]))

(defn create-connection []
  (js/console.debug "[ws] Creating connection......................")
  (make-conn (str config/ws-uri "/collab")))

(defonce state (r/atom {::ws-id nil ::player-map {}
                        ::conn nil
                        :algoradio.chat/textarea-height 23
                        :algoradio.chat/messages ()
                        :algoradio.chat/show-chat? true
                        :algoradio.chat/show-latest-messages? true}))

(defn make-collab-url [session-path]
  (str js/window.location.origin session-path))

(defn send-typing-event! [event-data]
  (js/console.debug "Sending type event" )
  (send-message! (::conn @state)
                 :collab-event
                 {:event-type :editor-change
                  :client-id (@state ::ws-id)
                  :editor-id 1
                  :event (js/JSON.stringify event-data)}))

(defn send-eval-event! [event-data]
  (js/console.debug "Sending eval event" event-data)
  (send-message! (::conn @state)
                 :collab-event
                 {:event-type :editor-eval
                  :client-id (@state ::ws-id)
                  :editor-id 1
                  :event (str {:mark event-data})}))

(defn send-play-event! [audio-id event-data originator-id]
  (js/console.debug "Sending play event" event-data)

  (send-message! (::conn @state)
                 :collab-event
                 {:client-id (@state ::ws-id)
                  :editor-id 1
                  :event-type :play
                  :event (str (assoc event-data
                                     :audio-id audio-id
                                     :originator-id originator-id))}))

(defn send-stop-event! [audio-id audio-type]
  (js/console.debug "Sending stop event"  audio-type)
  (send-message! (::conn @state)
                 :collab-event
                 {:event (str {:audio-id audio-id
                               :audio-type audio-type})
                  :client-id (@state ::ws-id)
                  :editor-id 1
                  :event-type :stop}))


(defn send-chat-message! [message]
  (js/console.debug "Sending chat message" message)
  (send-message! (::conn @state)
                 :chat-message
                 {:message message
                  :client-id (@state ::ws-id)}))

(comment (send-chat-message! "holissss"))
