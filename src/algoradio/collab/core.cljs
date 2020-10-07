(ns algoradio.collab.core
  (:require [algoradio.websockets :refer [make-conn send-message!]]
            [algoradio.config :as config]))

(defonce conn (make-conn (str config/ws-uri "/collab")) )

(defonce state (atom {::ws-id nil
                      ::player-map {}}))

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

(defn send-play-event! [audio-id event-data originator-id]
  (println "Sending play event" event-data)

  (send-message! conn
                 :collab-event
                 (str (assoc event-data
                             :audio-id audio-id
                             :originator-id originator-id))
                 {:id (@state ::ws-id)
                  :editor-id 1
                  :type :play}))

(defn send-stop-event! [audio-id audio-type]
  (println "Sending stop event"  audio-type)
  (send-message! conn
                 :collab-event
                 (str {:audio-id audio-id
                       :audio-type audio-type})
                 {:id (@state ::ws-id)
                  :editor-id 1
                  :type :stop}))
