(ns algoradio.hydra
  (:require [algoradio.js-loader :refer [load-script]]))

(defonce h (atom nil))

(defn create! [canvas-id]
  (load-script
   {:src "https://unpkg.com/hydra-synth@1.3.6/dist/hydra-synth.js"
    :on-load
    (fn [_]
      (let [canvas (js/document.getElementById canvas-id)
            scale js/window.devicePixelRatio
            size (js/Math.floor (* scale 500))]
        (set! (.. canvas -height) size)
        (set! (.. canvas -width) size)
        (new js/Hydra
             (clj->js
              {:canvas canvas
               :autoLoop true
               :makeGlobal true
               :numSources 4
               :numOutputs 4
               :detectAudio true
               :extendTransforms []
               :precision "mediump"
               }))))}))

(defn init! []
  (reset! h (create! "hydra-canvas")))

(comment
  (init!)

  (-> (js/shape 3)
      (.scrollX 0.1 0.1 )
      (.out))

  (-> (js/osc 100)
      (.color 2 0.1 1)
      (.mult (js/osc 1 0.01 0))
      (.repeat 5 2)
      (.mult (js/osc 2))
      (.modulateRotate (js/noise 2 0.5) 0.5)
      (.diff (-> (js/src js/o0)
                 (.color  0.7 #(rand-nth [0.1 1.2 1]) #(rand-nth [0.1 2])))
             0.7)
      (.out))

  (js/console.log (js/document.getElementById "hydra-canvas"))
  )
