(ns algoradio.player
  (:require [algoradio.state :refer [app-state]]
            ["howler" :refer [Howl]]))

(declare play-sound!)
(defn notify-finished! [src type]
  (js/console.log "ended")
  (play-sound! type))

(defn play-sound!
  ([type] (play-sound! nil type))
  ([index type]
   (let [idx-fn (if index
                  #(nth % index (count %))
                  #(rand-nth %))
         src (-> @app-state :freesounds (get type) idx-fn :mp3)
         audio (Howl. (clj->js {:src [src]
                                :html5 true
                                :volume 0
                                :autoplay true}))]

     (.on audio "load" (fn []
                         (.fade audio 0 1 5000)
                         (let [duration (.duration audio)]
                           (when (> duration 5)
                             (js/setTimeout #(do
                                               (notify-finished! src type)
                                               (.fade audio 1 0 5000))
                                            (* 1000 (- duration 5))))))))))
(comment
  (play-sound! "river"))
