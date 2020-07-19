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

(defn replay-event!
  [app-state {:keys [event_type audio_id type] :as event}]
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
                 (player/stop! type audio id)))
    "editor_change" (do (js/console.debug "Replay editor change" (event :change))
                        (-> (editor/get-cm! app-state) .-doc
                            (.replaceRange
                             (-> event :change :text
                                 (#(if (= ["" ""] %) ["\n"] %))
                                 (nth 0) clj->js)
                             (-> event :change :from clj->js)
                             (-> event :change :to clj->js))))
    "editor_eval" (let [code (-> event :mark :code)
                        code-to-eval (remove-function-calls excluded-fns code)]
                    (js/console.debug "Replay editor eval"(event :mark))
                    (editor/mark-text! (event :mark)
                                       (editor/get-cm! app-state))
                    (js/eval code-to-eval)
                    (js/console.log code))))

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
