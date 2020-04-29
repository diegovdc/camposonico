(ns algoradio.common)

(defonce color-offset (rand-int 100))
(defn get-color [sound-id]
  (nth colors (mod (+ color-offset sound-id)
                   (dec (count colors))) "#000"))
