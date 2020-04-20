(ns algoradio.player
  (:require [algoradio.state :refer [app-state]]
            [cljs.user :refer [spy]]
            [algoradio.freesound :as freesound]
            ["howler" :refer [Howl]]))

(declare play-sound!)
(defn get-playing-audio-by-type [app-state type]
  (->> (app-state ::now-playing)
       (filter (fn [s] (and (= type (:type s))
                           (.playing (:audio s)))))))

(defn play? [app-state type]
  (let [density (get-in app-state [::fields-density type] 0)
        total (count (get-playing-audio-by-type app-state type))]
    (spy "should play?" (< (max 0 total) density))))

(defn notify-finished! [src type]
  (js/console.log "ended")
  (if (play? @app-state type)
    (play-sound! type))
  (freesound/get-audios! app-state type))

(defn play-sound!
  ([type] (play-sound! nil type))
  ([index type]
   (let [idx-fn (if index
                  #(nth % index (count %))
                  #(rand-nth %))
         src (-> @app-state :freesounds (get type) idx-fn :mp3)
         audio (Howl. (clj->js {:src [src]
                                :html5 true
                                :volume 0}))

         _ (.on audio "load"
                (fn []
                  (js/console.log "sound loaded")
                  (if-not (play? @app-state type)
                    (do
                      (js/console.log "shouldn't play audio")
                      (js/setTimeout #(.stop audio) 100))
                    (do
                      (js/console.log "starting playback")
                      (.fade audio 0 1 5000)
                      (let [duration (.duration audio)
                            >5? (> duration 5)]
                        (js/console.log "dur" duration)
                        ;; TODO improve callback scheduling based on audio duration
                        (js/setTimeout #(when >5? (.fade audio 1 0 5000))
                                       (* 1000 (- duration (if >5? 5 0))))
                        (js/setTimeout #(notify-finished! src type)
                                       (* 1000 (+ 0.5 duration))))))))
         id (.play audio)
         _ (js/console.log "sound playing" src)]
     (.on audio "play"
          (fn [] (swap! app-state
                       #(-> %
                            (update
                             ::now-playing conj
                             {:id id
                              :audio audio
                              :src src
                              :type type}))))))))

(defn update-density! [op type]
  "Op should be `inc` or `dec`"
  (js/console.log "update density")
  (swap! app-state update-in [::fields-density type]
         #(if % (max 0 (op %)) 1)))

(defn user-play-sound!
  ([type] (user-play-sound! nil type))
  ([index type]
   (update-density! inc type)
   (play-sound! index type)))

(defn rand-stop! [type]
  (js/console.log "stoping sound" type)
  (let [audios (spy (-> (get-playing-audio-by-type
                         @app-state
                         type)))
        {:keys [id audio]} (when-not
                               (empty? audios)
                             (rand-nth audios))]
    (update-density! dec type)
    (when audio
      (.stop audio)
      (swap! app-state update ::now-playing
             (fn [np] (remove #(= id (% :src)) np))))))

(comment
  (reset! app-state)
  (play-sound! "mountain"))
(comment
  (-> @app-state ::fields-density)
  (update-density! dec "river")
  (count (get-playing-audio-by-type @app-state "river")))
