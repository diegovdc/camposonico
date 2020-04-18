(ns algoradio.state
  (:require [reagent.core :as reagent]))

(defonce app-state
  (reagent/atom {:text "Hello world!"
                 :freesounds {}}))
