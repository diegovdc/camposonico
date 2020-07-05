(ns algoradio.common)

(defonce color-offset (rand-int 100))

(defn distinct-by [f coll]
  (->> coll
       (group-by f)
       (map (fn [[_ v]] (first v)))))
