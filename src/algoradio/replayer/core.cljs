(ns algoradio.replayer.core
  (:require
   [clojure.core.async :as a]
   [algoradio.editor :as editor]
   [algoradio.player :as player]
   [algoradio.replayer.parse-code-for-eval :refer [remove-function-calls]]
   [cljs.user :refer [spy]]
   [clojure.string :as str]))

(def replay-map (atom {}))

(def excluded-fns ["play" "stop" "replayFromFile" "replayFromUrl"
                   "uploadSelections" "downloadSelections" "downloadHistory"])

(defn play-play! [audio_id event type]
  (let [data (player/play! event (event :opts))]
    (js/console.log "Replay playing" data)
    (player/update-density! inc type)
    (swap! replay-map assoc audio_id data)
    data))

(defn play-stop! [audio_id type]
  (if-let [{:keys [id audio] :as data} (@replay-map audio_id)]
    (do (js/console.debug "Replay stoping" data)
        (player/stop! type audio id))))

(defn play-editor-change! [app-state event]
  (do (js/console.debug "Replay editor change" (event :change))
      (-> (editor/get-cm! app-state) .-doc
          (.replaceRange
           (-> event :change :text
               (#(if (= ["" ""] %) ["\n"] %))
               (nth 0) clj->js)
           (-> event :change :from clj->js)
           (-> event :change :to clj->js)))))

(defn play-editor-eval! [app-state event]
  (let [code (-> event :mark :code)
        code-to-eval (remove-function-calls excluded-fns code)]
    (js/console.debug "Replay editor eval"(event :mark))
    (editor/mark-text! (event :mark)
                       (editor/get-cm! app-state))
    (js/eval code-to-eval)
    (js/console.log code)))

(defn replay-event!
  [app-state {:keys [event_type audio_id type] :as event}]
  (condp = (name event_type)
    "play" (play-play! audio_id event type)
    "volume" (if-let [{:keys [id audio] :as data} (@replay-map audio_id)]
               (do (js/console.debug "Replay stoping" data)
                   (player/set-volume! id audio (event :volume_level))))
    "stop" (play-stop! audio_id type )
    "editor_change" (play-editor-change! app-state event)
    "editor_eval" (play-editor-eval! app-state event)))

(defn replay! [app-state history]
  (editor/clear-all! app-state)
  (a/go-loop [h history]
    (when (seq h)
      (let [ev (first h)]
        (a/<! (a/timeout (ev :interval)))
        (replay-event! app-state ev)
        (js/console.log ev))
      (recur (next h)))))

(comment
  (def hist (algoradio.history/get-history! algoradio.state/app-state))
  (js/console.log hist)
  (replay! algoradio.state/app-state hist))
