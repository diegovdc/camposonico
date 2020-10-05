(ns algoradio.collab.init
  (:require [algoradio.websockets :refer [make-conn send-message!]]))

(defonce state (atom {::ws-id nil}))

(defonce conn (make-conn "ws://localhost:3456/collab") )

(defn send-typing-event! [event-data]
  (println "Sending type event" )
  (send-message! conn
                 :collab-event
                 (js/JSON.stringify event-data)
                 {:type :editor-change
                  :id (@state ::ws-id)
                  :editor-id 1}))

(defn send-eval-event! [event-data]
  (println "Sending eval event" event-data)
  (send-message! conn
                 :collab-event
                 (str {:mark event-data})
                 {:id (@state ::ws-id)
                  :editor-id 1
                  :type :editor-eval}))

(defn send-play-event! [audio-id event]
  (println audio-id event))
