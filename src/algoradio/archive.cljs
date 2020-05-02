(ns algoradio.archive
  (:require ["howler" :refer [Howl]]
            [cljs.user :refer [spy spy->]]
            [algoradio.archive.sounds :refer [sounds]]))

(defn random-interval [min max]
  (+ min (rand-int (- max min))))

(defn delay*
  [x ms]
  (js/Promise.
   (fn [res rej]
     (js/setTimeout #(res x) ms))))

(defn select-sound
  "`already-played` is a set of the with the sounds that have already been played,
  it is a key on `algoradio.state`"
  [already-played sounds]
  (loop [s (first sounds)
         ss (rest sounds)]
    (let [played? (already-played s)]
      (cond
        played? (recur (first ss) (rest ss))
        (and s (not played?)) {:already-played (conj already-played s)
                               :sound s}
        :else (let [s* (first sounds)]
                (js/console.log "All archive sounds played, restarting...")
                {:already-played #{s*}
                 :sound s*})))))

(defn +schedule
  [sound min-wait-ms max-wait-ms]
  (assoc sound :schedule
         (random-interval min-wait-ms max-wait-ms)))

(defn +duration
  "Gets duration from the `:audio` Howl instance"
  [{:keys [audio] :as sound}]
  (assoc sound :duration (.duration audio)))

(defn +on-play
  "Adds a callback to the `:audio` Howl instance"
  [{:keys [audio url duration] :as sound} on-play]
  (.on audio "play" #(on-play sound))
  sound)

(defn +on-end
  "Adds a callback to the `:audio` Howl instance"
  [{:keys [audio] :as sound} cb]
  (.on audio "end" #(cb))
  sound)

(defn preload-metadata!
  "Preloads the audio metadata and returns a Promise witha preloaded Howl object
  when ready"
  [{:keys [mp3] :as sound}]
  (let [audio (Howl. (clj->js {:src [mp3]
                               :html5 true
                               :preload true}))]
    (-> (js/Promise.
         (fn [res rej]
           (.on audio "load" #(res (assoc sound :audio audio)))
           (.on audio "loaderror" #(rej "Could not load audio!")))))))

(defn prepare-sound
  [min-wait-ms max-wait-ms on-play on-end already-played sounds]
  (let [data (select-sound already-played sounds)]
    (update data :sound
            (fn [sound]
              (spy "before-load===============" sound)
              (.then (preload-metadata! sound)
                     #(-> %
                          (spy-> "loaded")
                          +duration
                          (+schedule min-wait-ms max-wait-ms)
                          (+on-play on-play)
                          (+on-end on-end)))))))

(declare init!)

(defn play!
  [{sound-promise :sound}]
  (spy "play" sound-promise)
  (-> sound-promise
      (.then #(spy "promise1" %))
      (.then #(delay* % (spy "Delaying for (ms):" (% :schedule))))
      (.then #(spy "promise2" %))
      (.then #(do (.play (:audio %)) %))
      (.then #(spy "promise3" %))))

(defn init! [min-wait-time max-wait-time on-play on-end app-state sounds]
  (spy "app state" app-state)
  (when (get @app-state ::should-play? false)
    (let [s (prepare-sound
             min-wait-time max-wait-time
             on-play
             on-end
             (@app-state ::already-played)
             sounds)]
      (spy "Will play " s)
      (-> (play! s)
          (.then (swap! app-state assoc ::already-played (s :already-played)))
          (.catch (fn [e]
                    (js/console.log e)
                    (js/console.log "Retrying in 5 seconds")
                    (js/setTimeout #(init! min-wait-time
                                           max-wait-time
                                           on-play
                                           on-end
                                           app-state
                                           sounds)
                                   5000)))))))
(defn get-first-playing-sound
  [now-playing]
  (->> now-playing (filter #(= :archive (:type %))) first))

(defn stop! [now-playing]
  (->> now-playing
       get-first-playing-sound
       :audio
       .stop))
(comment
  (stop! (-> @algoradio.state/app-state :algoradio.player/now-playing))
  (init! 100
         500
         #(spy "archive sound ended")
         algoradio.state/app-state
         sounds))
