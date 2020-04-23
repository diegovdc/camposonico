(ns algoradio.state
  (:require [reagent.core :as reagent]
            [algoradio.archive :as archive]
            [algoradio.editor :as editor]))

(def editor-text
  "// ctrl+enter sobre la linea para ejecutarla\nload('ocean')\n\nplay('ocean')")

(defonce app-state
  (reagent/atom {:text "Hello world!"
                 :freesounds {}
                 ::archive/already-played #{}
                 ::editor/text editor-text}))
