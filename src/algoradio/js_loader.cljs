(ns algoradio.js-loader)

(defn load-script [{:keys [src on-load]
                    :or {on-load #(js/console.log (str "Script: " src " loaded"))}}]
  (let [script (js/document.createElement "script")]
    (set! (.. script -onload) on-load)
    (set! (.. script -src) src)
    (js/console.log "LOADING script")
    (js/document.head.appendChild script)))

(comment
  (load-script {:src "https://cdnjs.cloudflare.com/ajax/libs/tone/14.7.62/Tone.js"
                :on-load js/console.log})
  (load-script {:src "https://unpkg.com/hydra-synth@1.3.0/dist/hydra-synth.js"
                :on-load js/console.log}))
