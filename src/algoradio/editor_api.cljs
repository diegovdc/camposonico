(ns algoradio.editor-api
  (:require [algoradio.hydra :as hydra]
            [algoradio.source-info :as sources]
            [algoradio.editor :as editor]
            [algoradio.player :as player]
            [algoradio.search :as search]
            [algoradio.freesound :as freesound]
            [algoradio.download :as download]))

(declare load-audios!)

(defn setup! [app-state]
  (set! (.. js/window -initHydra) hydra/init!)
  (set! (.. js/window -randNth) rand-nth)
  (set! (.. js/window -load) (partial load-audios! app-state))
  (set! (.. js/window -showInfo) sources/rand-info!)
  (set! (.. js/window -infoAsBackground) sources/as-background!?)
  (set! (.. js/window -setInfoPosition) sources/set-position!)
  (set! (.. js/window -clearComments) #(editor/remove-comment-lines! app-state))
  (set! (.. js/window -stop) player/rand-stop!)
  (set! (.. js/window -play) player/user-play-sound!)
  (set! (.. js/window -traerAudios) search/get-audios!)
  (set! (.. js/window -setBaseQuery) freesound/reset-base-query!)
  (set! (.. js/window -uploadSelections) download/toggle-uploader!)
  (set! (.. js/window -downloadSelections) (fn [name]
                                             (download/download-json!
                                              (get @app-state
                                                   ::sources/selection-list)
                                              name))))

#_(get @algoradio.state/app-state ::sources/selection-list)

(defn load-audios!
  ([app-state query]
   (-> (freesound/get-audios! app-state query)
       (.then #(swap! app-state assoc ::search "")))))
