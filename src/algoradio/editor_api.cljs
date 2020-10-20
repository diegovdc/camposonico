(ns algoradio.editor-api
  (:require [algoradio.js-loader :refer [load-script]]
            [algoradio.hydra :as hydra]
            [algoradio.source-info :as sources]
            [algoradio.editor :as editor]
            [algoradio.history :as history]
            [algoradio.player :as player]
            [algoradio.search :as search]
            [algoradio.freesound :as freesound]
            [algoradio.fs :as fs]
            [algoradio.config :as config]
            [algoradio.state :as state]
            [algoradio.archive :as archive]
            [clojure.walk :as walk]
            [clojure.set :as set]))

(declare load-audios!)

(defn setup! [app-state]
  (set! (.. js/window -loadScript)
        (fn [opts]
          (js/console.log opts)
          (load-script
           (-> opts js->clj
               walk/keywordize-keys
               (set/rename-keys {:onLoad :on-load})))))
  (set! (.. js/window -initHydra) hydra/init!)
  (set! (.. js/window -randNth) rand-nth)
  (set! (.. js/window -load) (partial load-audios! app-state))
  (set! (.. js/window -loadCamposonico) #(archive/load-as-freesounds! app-state))
  (set! (.. js/window -showInfo) sources/rand-info!)
  (set! (.. js/window -infoAsBackground) sources/as-background!?)
  (set! (.. js/window -setInfoPosition) sources/set-position!)
  (set! (.. js/window -clearAll) #(editor/clear-all! app-state))
  (set! (.. js/window -clearComments) #(editor/remove-comment-lines! app-state))
  (set! (.. js/window -stop) player/rand-stop!)
  (set! (.. js/window -play) (fn [type opts]
                               (player/user-play-sound!
                                type (-> opts js->clj walk/keywordize-keys))))
  (set! (.. js/window -setBaseQuery) (partial freesound/reset-base-query! app-state))
  (set! (.. js/window -uploadSelections) #(fs/toggle-uploader! :selections))
  (set! (.. js/window -replayFromFile) #(fs/toggle-uploader! :history))
  (set! (.. js/window -replayFromUrl) (fn [url] (fs/replay-from-url! url)))
  (set! (.. js/window -autoPlay) (fn [bool] (reset! config/auto-play? (boolean bool))))
  (set! (.. js/window -downloadSelections)
        (fn [name]
          (fs/download-selections!
           name (get @app-state ::sources/selection-list))))
  (set! (.. js/window -downloadHistory)
        (fn [name*]
          (fs/download-history!
           (or name* (str "camposonico-history-" (.toISOString (js/Date.))))
           (history/get-history! app-state))))
  (set! (.. js/window -getSounds) (fn [] (clj->js (state/get-sounds))))
  (set! (.. js/window -getCSArchiveSounds) (fn [] (clj->js (archive/get-archive-sounds))))
  (set! (.. js/window -getHistory) (fn [] (clj->js (history/get-history! app-state)))))

#_(get @algoradio.state/app-state ::sources/selection-list)

(defn load-audios!
  ([app-state query]
   (-> (freesound/get-audios! app-state query)
       (.then #(swap! app-state assoc ::search "")))))

(comment (setup! @algoradio.state/app-state))
