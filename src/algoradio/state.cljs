(ns algoradio.state
  (:require [reagent.core :as reagent]
            [algoradio.archive :as archive]
            [algoradio.editor :as editor]
            [algoradio.freesound :as freesound]))

(defonce app-state
  (reagent/atom {:algoradio.freesound/base-query "field+recordings"
                 :freesounds {}
                 :freesounds-pages {}
                 ::archive/already-played #{}
                 ;; ::editor/text nil
                 ::editor/key 1}))

(defn get-sounds []
  (get @app-state :freesounds))
