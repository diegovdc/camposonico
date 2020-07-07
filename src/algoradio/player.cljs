(ns algoradio.player
  (:require [algoradio.state :refer [app-state]]
            [cljs.user :refer [spy]]
            [algoradio.config :as config]
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
    (< (max 0 total) density)))

(defn update-now-playing [now-playing]
  (filter (fn [{:keys [audio]}]
            (let [loading? (= "loading" (.state audio))
                  playing? (.playing audio)]
              (or loading? playing?)))
          now-playing))

(defn update-now-playing! []
  (swap! app-state update ::now-playing update-now-playing))

(defn notify-finished! [src type]
  #_(js/console.log "ended")
  (if (play? @app-state type)
    (play-sound! type))
  (update-now-playing!)
  (freesound/get-audios! app-state type))

(declare stop!)
(defn play-sound!
  ([type] (play-sound! type nil))
  ([type {:keys [index vol start dur] :as opts :or {vol config/default-volume}}]
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
                  (js/console.log "New sound loaded" audio)
                  (if-not (play? @app-state type)
                    (do
                      #_(js/console.log "shouldn't play audio")
                      (js/setTimeout #(.stop audio) 100))
                    (do
                      #_(js/console.log "starting playback")
                      (.fade audio 0 vol 5000)
                      (let [duration (.duration audio)
                            >5? (> duration 5)]
                        #_(js/console.log "dur" duration)
                        ;; TODO improve callback scheduling based on audio duration
                        (js/setTimeout #(when >5? (.fade audio vol 0 5000))
                                       (* 1000 (- duration (if >5? 5 0))))
                        (js/setTimeout #(notify-finished! src type)
                                       (* 1000 (+ 0.5 duration))))))))
         id (.play audio)]
     (when start (.seek audio start))
     (when dur (js/setTimeout #(do
                                 (.fade audio (.-_volume audio) 0 5000)
                                 (js/setTimeout (fn [] (stop! type audio)) 5000))
                              (* 1000 dur)))
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
  #_(js/console.log "update density")
  (swap! app-state update-in [::fields-density type]
         #(if % (max 0 (op %)) 1)))

(defn stop! [type audio]
  (update-density! dec type)
  (when audio
    (.stop audio)
    (update-now-playing!)))

#_(-> @app-state :freesounds (get "ocean"))

(defn user-play-sound!
  ([type] (user-play-sound! type nil))
  ([type opts]
   (when (-> @app-state :freesounds (get type))
     (update-density! inc type)
     (play-sound! type opts))))

(defn rand-stop! [type]
  (js/console.log "Stoping sound" type)
  (let [audios (spy (-> (get-playing-audio-by-type
                         @app-state
                         type)))
        {:keys [id audio]} (when-not
                               (empty? audios)
                               (rand-nth audios))]
    (stop! type audio)))

(declare init-archive!)
(defn notify-finished-archive! []
  #_(spy "archive track is finished")
  (when (@app-state ::archive/should-play?)
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
