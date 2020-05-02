(ns algoradio.player
  (:require [algoradio.state :refer [app-state]]
            [cljs.user :refer [spy]]
            [algoradio.freesound :as freesound]
            ["howler" :refer [Howl]]
            [algoradio.archive :as archive]
            [algoradio.archive.sounds :as archive*]))

(declare play-sound!)
(defn get-playing-audio-by-type [app-state type]
  (->> (app-state ::now-playing)
       (filter (fn [s] (and (= type (:type s))
                           (.playing (:audio s)))))))

(defn play? [app-state type]
  (let [density (get-in app-state [::fields-density type] 0)
        total (count (get-playing-audio-by-type app-state type))]
    (spy "should play?" (< (max 0 total) density))))

(defn update-now-playing [now-playing]
  (filter (fn [{:keys [audio]}]
            (let [loading? (= "loading" (.state audio))
                  playing? (.playing audio)]
              (or loading? playing?)))
          now-playing))

(defn update-now-playing! []
  (swap! app-state update ::now-playing update-now-playing))

(defn notify-finished! [src type]
  (js/console.log "ended")
  (if (play? @app-state type)
    (play-sound! type))
  (update-now-playing!)
  (freesound/get-audios! app-state type))

(defn play-sound!
  ([type] (play-sound! nil type))
  ([index type]
   (let [idx-fn (if index
                  #(nth % index (count %))
                  #(rand-nth %))
         {src :mp3 :as sound} (-> @app-state :freesounds (get type) idx-fn)
         audio (Howl. (clj->js {:src [src]
                                :html5 true
                                :volume 0}))
         _ (js/console.log "will load sound playing" type src)
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
         id (.play audio)]
     (.on audio "play"
          (fn [] (swap! app-state update ::now-playing
                       #(-> %
                            update-now-playing
                            (conj {:id id
                                   :audio audio
                                   :src src
                                   :sound sound
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
      (update-now-playing!))))

(declare init-archive!)
(defn notify-finished-archive! []
  (spy "archive track is finished")
  (when (spy "play next archive track?"
             (@app-state ::archive/should-play?))
    (update-now-playing!)
    (init-archive! 0 0)))

(defn init-archive!
  "Starts a track from the archive"
  [min-wait max-wait]
  (archive/init!
   min-wait max-wait
   (fn [{:keys [audio] :as sound}]
     (swap! app-state update ::now-playing
            #(-> %
                 update-now-playing
                 (conj {:sound (dissoc sound :schedule :audio)
                        :audio audio
                        :src (sound :mp3)
                        :type :archive
                        :id (-> audio .-_sounds first .-_id)}))))
   notify-finished-archive!
   app-state
   archive*/sounds))

(defn stop-archive!
  []
  (archive/stop! (@app-state ::now-playing))
  (update-now-playing!))
(comment
  (-> algoradio.state/app-state deref ::now-playing )
  (reset! app-state)
  (play-sound! "mountain"))
(comment
  (-> @app-state ::fields-density)
  (update-density! dec "river")
  (count (get-playing-audio-by-type @app-state "river")))
