(ns algoradio.player
  (:require [algoradio.state :refer [app-state]]
            [algoradio.freesound :as freesound]
            ["howler" :refer [Howl]]))

(declare play-sound!)
(defn notify-finished! [src type]
  (js/console.log "ended")
  (-> (freesound/get-audios! app-state type)
      (.then #(play-sound! type))))

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

         _ (.on audio "load" (fn []
                               (js/console.log "sound loaded")
                               (.fade audio 0 1 5000)
                               (let [duration (.duration audio)]
                                 (when (> duration 5)
                                   (js/setTimeout
                                    #(do
                                       ;; TODO what happens if user pauses this field's stream
                                       (notify-finished! src type)
                                       (.fade audio 1 0 5000))
                                    (* 1000 (- duration 5)))))))
         id (.play audio)]
     (.on audio "play" #(swap! app-state update ::now-playing conj
                              {:id id
                               :audio audio
                               :src src
                               :type type})))))

(defn get-playing-audio-by-type [app-state type]
  (->> (app-state ::now-playing)
       (filter (fn [s] (and (= type (:type s))
                           (.playing (:audio s)))))))

(defn rand-stop! [type]
  (js/console.log "stoping sound" type)
  (let [audios (-> (get-playing-audio-by-type
                    @app-state
                    type))
        {:keys [src audio]} (when-not (empty? audios) (rand-nth audios))]
    (when audio
      (.stop audio)
      (swap! app-state update ::now-playing
             (fn [np] (remove #(= src (% :src)) np))))))

(comment
  (reset! app-state)
  (play-sound! "mountain"))
