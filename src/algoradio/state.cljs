(ns algoradio.state
  (:require [reagent.core :as reagent]
            [algoradio.archive :as archive]))

(defonce app-state
  (reagent/atom {:text "Hello world!"
                 :freesounds {}
                 ::archive/already-played #{}}))
