(ns algoradio.editor-api
  (:require ["tone" :as Tone]
            [algoradio.hydra :as hydra]
            [algoradio.source-info :as sources]
            [algoradio.editor :as editor]
            [algoradio.player :as player]
            [algoradio.search :as search]
            [algoradio.freesound :as freesound]))

(declare load-audios!)

(defn setup! [app-state]
  (set! (.. js/window -initHydra) hydra/init!)
  (set! (.. js/window -Tone) Tone)
  (set! (.. js/window -randNth) rand-nth)
  (set! (.. js/window -load) (partial load-audios! app-state))
  (set! (.. js/window -showInfo) sources/rand-info!)
  (set! (.. js/window -infoAsBackground) sources/as-background!?)
  (set! (.. js/window -setInfoPosition) sources/set-position!)
  (set! (.. js/window -clearComments) #(editor/remove-comment-lines! app-state))
  (set! (.. js/window -stop) player/rand-stop!)
  (set! (.. js/window -play) player/user-play-sound!)
  (set! (.. js/window -traerAudios) search/get-audios!))

(defn load-audios!
  ([app-state query]
   (-> (freesound/get-audios! app-state query)
       (.then #(swap! app-state assoc ::search "")))))
