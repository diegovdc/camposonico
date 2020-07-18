(ns algoradio.replayer
  (:require
   [clojure.core.async :as a]
   [algoradio.player :as player]))

(def replay-map (atom {}))

(js/console.log @replay-map)
(defn replay-event!
  [{:keys [event_type audio_id type] :as event}]
  (condp = (name event_type)
    "play" (let [data (player/play! event (event :opts))]
             (js/console.debug "Replay playing" data)
             (player/update-density! inc type)
             (swap! replay-map assoc audio_id data))
    "volume" (if-let [{:keys [id audio] :as data} (@replay-map audio_id)]
               (do (js/console.debug "Replay stoping" data)
                   (player/set-volume! id audio (event :volume_level))))
    "stop" (if-let [{:keys [id audio] :as data} (@replay-map audio_id)]
             (do (js/console.debug "Replay stoping" data)
                 (player/stop! type audio id)))))

(comment
  (def hist (algoradio.history/get-history! algoradio.state/app-state))
  (js/console.log hist)
  (a/go-loop [h hist]
    (when (seq h)
      (let [ev (first h)]
        (a/<! (a/timeout (ev :interval)))
        (replay-event! ev)
        (js/console.log ev))
      (recur (next h)))))
