(ns algoradio.hydra
  (:require ["hydra-synth" :as Hydra]))

(defonce h (atom nil))

(defn create! [canvas-id]
  (new Hydra
       (clj->js
        {:canvas (js/document.getElementById canvas-id)
         :autoLoop true
         :makeGlobal true
         :numSources 4
         :numOutputs 4
         :extendTransforms []
         :precision "mediump"
         })))

(defn init! []
  (reset! h (create! "hydra-canvas")))

(comment
  (init!)

  (-> (js/shape 3)
      (.scrollX )
      (.out))

  (js/console.log (js/document.getElementById "hydra-canvas"))
  )
