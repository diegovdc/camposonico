(ns algoradio.archive
  (:require ["howler" :refer [Howl]]
            [cljs.user :refer [spy]]))

(def sounds
  #{#_ #_ #_#{:url "https://ia801407.us.archive.org/31/items/fuego-2020-02-22/fuego-2020-02-22.mp3"}
    {:url "https://ia802809.us.archive.org/25/items/espirales_1-2020-02-22_toma_1/espirales_1-2020-02-22_toma_2.mp3"}
    {:url "https://ia801407.us.archive.org/31/items/fuego-2020-02-22/fuego-2020-02-2223432.mp3"}
    {:url "https://ia801709.us.archive.org/35/items/drum_overlapping_sounds_db_audio/36_38d0_l100.mid.mp3"}})

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
  [{:keys [audio url duration] :as sound}]
  (.on audio "play" #(js/console.log "Started playing:" url ", duration: " duration))
  sound)

(defn +on-end
  "Adds a callback to the `:audio` Howl instance"
  [{:keys [audio] :as sound} cb]
  (.on audio "end" #(cb))
  sound)

(defn preload-metadata!
  "Preloads the audio metadata and returns a Promise witha preloaded Howl object
  when ready"
  [{:keys [url] :as sound}]
  (let [audio (Howl. (clj->js {:src [url]
                               :html5 true
                               :preload true}))]
    (-> (js/Promise.
         (fn [res rej]
           (.on audio "load" #(res (assoc sound :audio audio)))
           (.on audio "loaderror" #(rej "Could not load audio!")))))))

(defn prepare-sound
  [min-wait-ms max-wait-ms on-end already-played sounds]
  (let [data (select-sound already-played sounds)]
    (update data :sound
            (fn [sound]
              (.then (preload-metadata! sound)
                     #(-> %
                          +duration
                          (+schedule min-wait-ms max-wait-ms)
                          +on-play
                          (+on-end on-end)))))))

(declare init!)

(defn play!
  ([{sound-promise :sound} retry-args]
   (-> sound-promise
       (.then #(delay* % (spy "Delaying for (ms):" (% :schedule))))
       (.then #(do (.play (:audio %)) %))
       (.then spy)
       (.catch (fn [e]
                 (js/console.log e)
                 (js/console.log "Retrying in 5 seconds")
                 (js/setTimeout #(apply init! retry-args) 5000))))))

(defn init! [min-wait-time max-wait-time on-end app-state sounds]
  (when (@app-state ::should-play?)
    (let [s (prepare-sound
             min-wait-time max-wait-time
             on-end
             (@app-state ::already-played)
             sounds)]
      (swap! app-state assoc ::already-played (s :already-played))
      (play! s [min-wait-time max-wait-time app-state sounds]))))

(comment
  (init! 100
         500
         #(spy "archive sound ended")
         algoradio.state/app-state
         sounds))
