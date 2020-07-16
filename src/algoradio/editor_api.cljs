(ns algoradio.editor-api
  (:require [algoradio.hydra :as hydra]
            [algoradio.source-info :as sources]
            [algoradio.editor :as editor]
            [algoradio.player :as player]
            [algoradio.search :as search]
            [algoradio.freesound :as freesound]
            [algoradio.download :as download]
            [algoradio.config :as config]
            [algoradio.state :as state]
            [algoradio.archive :as archive]
            [clojure.walk :as walk]))

(declare load-audios!)

(defn setup! [app-state]
  (set! (.. js/window -initHydra) hydra/init!)
  (set! (.. js/window -randNth) rand-nth)
  (set! (.. js/window -load) (partial load-audios! app-state))
  (set! (.. js/window -loadCamposonico) #(archive/load-as-freesounds! app-state))
  (set! (.. js/window -showInfo) sources/rand-info!)
  (set! (.. js/window -infoAsBackground) sources/as-background!?)
  (set! (.. js/window -setInfoPosition) sources/set-position!)
  (set! (.. js/window -clear) #(editor/clear-all! app-state))
  (set! (.. js/window -clearComments) #(editor/remove-comment-lines! app-state))
  (set! (.. js/window -stop) player/rand-stop!)
  (set! (.. js/window -play) (fn [type opts]
                               (player/user-play-sound!
                                type (-> opts js->clj walk/keywordize-keys))))
  (set! (.. js/window -traerAudios) search/get-audios!)
  (set! (.. js/window -setBaseQuery) (partial freesound/reset-base-query! app-state))
  (set! (.. js/window -uploadSelections) download/toggle-uploader!)
  (set! (.. js/window -autoPlay) (fn [bool] (reset! config/auto-play? (boolean bool))))
  (set! (.. js/window -downloadSelections)
        (fn [name]
          (download/download-json!
           (get @app-state ::sources/selection-list) name)))
  (set! (.. js/window -getSounds) (fn [] (clj->js (state/get-sounds))))
  (set! (.. js/window -getCSArchiveSounds) (fn [] (clj->js (archive/get-archive-sounds))))
  ;; TODO Add timestamps
  ;; TODO Add volume
  ;; FIXME reverse history
  (set! (.. js/window -getHistory) (fn [] (clj->js (player/get-history)))))

#_(get @algoradio.state/app-state ::sources/selection-list)

(defn load-audios!
  ([app-state query]
   (-> (freesound/get-audios! app-state query)
       (.then #(swap! app-state assoc ::search "")))))

(comment (setup! @algoradio.state/app-state))
