(ns algoradio.player
  (:require [algoradio.state :refer [app-state]]
            [cljs.user :refer [spy]]
            [algoradio.config :as config]
            [algoradio.freesound :as freesound]
            [algoradio.history :as history]
            ["howler" :refer [Howl]]
            [algoradio.archive :as archive]
            [algoradio.archive.sounds :as archive*]
            [clojure.walk :as walk]
            [clojure.set :as set]
            [algoradio.collab.core :as collab]))

;; TODO collab mode has made this file pretty complex (i.e. preventing feedback)... too much coupling with the collab namespaces, ideally all this should be simplified

(declare play-sound! update-density!)
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

(defn notify-finished! [audio-data type]
  (when (and @config/auto-play? (play? @app-state type)
             (= (:originator-id audio-data)
                (::collab/ws-id @collab/state)))
    (play-sound! type))
  (when-not (= (:originator-id audio-data)
               (::collab/ws-id @collab/state))
    (update-density! dec type))
  (update-now-playing!)
  (freesound/get-audios! app-state type))

(declare stop!)

(defn play! [{:keys [src type] :as sound}
             {:keys [vol start dur send-to-collab? originator-id]
              :or {vol config/default-volume
                   send-to-collab? true}
              :as opts}]
  (let [originator-id* (or originator-id (::collab/ws-id @collab/state))
        audio (Howl. (clj->js {:src [src]
                               :html5 true
                               :volume 0}))
        _ (js/console.log "Will load sound for playback" type src originator-id)
        id (.play audio)
        audio-data {:id id
                    :audio audio
                    :src src
                    :sound sound
                    :type type
                    :opts opts
                    :originator-id originator-id*}
        _ (.on audio "load"
               (fn [data]
                 (js/console.log "Sound loaded" id (audio-data :originator-id))
                 (js/console.debug "Sound loaded" audio)
                 (if-not (play? @app-state type)
                   (do
                     (js/console.debug "shouldn't play audio")
                     (js/setTimeout #(.stop audio) 100))
                   (do
                     (js/console.debug "starting playback")
                     (.fade audio 0 vol 5000)
                     (let [duration (.duration audio)
                           >5? (> duration 5)]
                       ;; TODO improve callback scheduling based on audio duration
                       (js/setTimeout #(when >5? (.fade audio vol 0 5000))
                                      (* 1000 (- duration (if >5? 5 0))))
                       (js/setTimeout #(notify-finished! audio-data type)
                                      (* 1000 (+ 0.5 duration))))))))
        ]
    (when start (.seek audio start))
    (when dur (js/setTimeout #(stop! type audio id) (* 1000 dur)))
    (.on audio "play"
         (fn []
           (swap! app-state update ::now-playing
                  #(-> %
                       update-now-playing
                       (conj audio-data)))
           (history/add-play! app-state
                              (assoc sound :opts opts)
                              id)
           (when send-to-collab?
             (collab/send-play-event!
              id
              (assoc sound :opts opts)
              originator-id*)
             (swap! collab/state assoc-in [::collab/player-map id]
                    {:id id :audio audio}))))
    {:id id :audio audio}))

(defn play-sound!
  ([type] (play-sound! type nil))
  ([type {:keys [index] :as opts}]
   (let [idx-fn (if index
                  #(nth % index (count %))
                  #(rand-nth %))
         sound (-> @app-state :freesounds (get type) idx-fn (set/rename-keys {:mp3 :src}))]
     (play! (assoc sound :type type) opts))))

(defn update-density! [op type]
  "Op should be `inc` or `dec`"
  #_(js/console.log "update density")
  (swap! app-state update-in [::fields-density type]
         #(if % (max 0 (op %)) 1)))

(defn stop!
  ([type audio id] (stop! type audio id true))
  ([type audio id send-to-collab?]
   (when audio
     (js/console.log "Fading out" type id)
     (.fade audio (.-_volume audio) 0 5000)
     (update-density! dec type)
     (js/setTimeout (fn [] (.stop audio) (update-now-playing!)) 5000)
     (history/add-stop! app-state id type)
     (when send-to-collab?
       (collab/send-stop-event! id type)))))

#_(-> @app-state :freesounds (get "ocean"))

(defn user-play-sound!
  ([type] (user-play-sound! type nil))
  ([type opts]
   (when (-> @app-state :freesounds (get type))
     (update-density! inc type)
     (play-sound! type opts))))

(defn rand-stop! [type]
  (let [audios (get-playing-audio-by-type @app-state type)
        {:keys [audio id]} (when-not (empty? audios) (rand-nth audios))]
    (stop! type audio id)))

(defn set-volume! [id audio vol]
  (.volume audio vol)
  (swap! app-state update ::volumes assoc id vol)
  (history/add-volume-change! app-state id vol))

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
     ;; TODO fix this so that `:id` is a key on a `hash-map`...
     ;; TODO fix `set-volume!` so that the audio state is handled in a single place
     ;; TODO `::volumes` is used in source_info so fix that too
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
