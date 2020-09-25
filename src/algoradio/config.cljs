(ns algoradio.config)

(def api (if goog.DEBUG
           "http://localhost:3456"
           "https://shrouded-beach-57468.herokuapp.com"))
(def default-volume 0.8)
(def auto-play? (atom true))
