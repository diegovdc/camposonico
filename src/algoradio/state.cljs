(ns algoradio.state
  (:require [reagent.core :as reagent]
            [algoradio.archive :as archive]
            [algoradio.editor :as editor]
            [algoradio.instructions :refer [intro]]))

(defonce app-state
  (reagent/atom {:text "Hello world!"
                 :freesounds {}
                 :freesounds-pages {}
                 ::archive/already-played #{}
                 ::editor/text intro
                 ::editor/key 1 }))
