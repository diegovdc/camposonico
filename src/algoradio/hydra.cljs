(ns algoradio.hydra)

(defonce h (atom nil))

(defn create! [canvas-id]
  (new js/Hydra
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
