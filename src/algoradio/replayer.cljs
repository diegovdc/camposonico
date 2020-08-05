(ns algoradio.replayers
  (:require
   [clojure.core.async :as a]
   [algoradio.editor :as editor]
   [algoradio.player :as player]
   [cljs.user :refer [spy]]
   [clojure.string :as str]))

(def replay-map (atom {}))

(js/console.log @replay-map)

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
    "editor_eval_mark" (do (js/console.debug "Replay editor change" (event :change))
                           (editor/mark-text! (event :mark)
                                              (editor/get-cm! app-state)))))

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
  (a/go-loop [h hist]
    (println "in")
    (when (seq h)
      (println "seq")
      (let [ev (first h)]
        (a/<! (a/timeout (ev :interval)))
        (replay-event! algoradio.state/app-state ev)
        (js/console.log ev))
      (recur (next h)))))


(defn- make-regex-to-remove-code
  "Receives a list of function names to exclude and returns a regex pattern
  that can do that job"
  [excluded-function-names]
  (let [excluded-group (str/join "|" excluded-function-names)]
    (println excluded-group)
    (re-pattern (str "(^|\\s)(" excluded-group ")(\\s*)\\(((.|\n)*?)\\)"))))
(reduce str (str/split "hola" #""))
(do
  (defn find-first-index-of-balanced-parens
    [code start-index]
    (let [code* (-> code (str/split #"")
                    (subvec (inc start-index)) ;; `inc` because the split leaves an unwanted exmpty string at the begining thus we need to adjust the starting point
                    (->> (map-indexed vector))
                    )]
      (reduce (fn [acc [i char]]
                (let [{:keys [opening closing] :as acc*}
                      (condp = char
                        "(" (update acc :opening inc)
                        ")" (update acc :closing inc)
                        acc)]
                  (if (and (= opening closing)
                           (> opening 0))
                    (reduced (+ start-index i))
                    (assoc acc* :index i))))
              {:opening 0 :closing 0 :index -1}
              code*)))

  (find-first-index-of-balanced-parens "play(hola()) bola" 0))

(do
  (defn remove-full-function-from-code
    "The `regexed-function` (a string) might not be properly closed because of a
  lack of balanced parens, the regex is not powerful enough :(,
  so we need to find the closing parens"
    [code regexed-function]
    (let [start-index (str/index-of code regexed-function)
          last-index (find-first-index-of-balanced-parens code start-index)
          full-function (subs code start-index (inc last-index))]
      (spy full-function (str/replace code full-function ""))
      )
    )
  (remove-full-function-from-code "play(hola(((())))) bola" "play(hola()"))

(do
  (defn remove-code
    "Receives a list of function names to exclude"
    [excluded-function-names code]
    (let [matches (->> code
                       (re-seq (make-regex-to-remove-code
                                excluded-function-names))
                       (map first))]
      (reduce remove-full-function-from-code code matches)))
  (remove-code ["play" "stop"] "play  (\"holi boli\") hola load(\"\") stop()"))


(re-seq #"(^|\s)(play|stop)\(((.|\n)*?)\)" "play(\"holi boli\") hola load(\"\")")
(re-seq (re-pattern (str "(^|\\s)(" "play|stop" ")\\(((.|\n)*?)\\)")) "play(\"holi boli\") hola load(\"\")")
