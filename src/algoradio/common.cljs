(ns algoradio.common
  (:require [clojure.walk :as walk]
            [cljs.user :refer [spy]]
            [clojure.string :as str]))

(defonce color-offset (rand-int 100))

(defn distinct-by [f coll]
  (->> coll
       (group-by f)
       (map (fn [[_ v]] (first v)))))

(defn set-as-freesound-query! [app-state {:keys [name data]}]
  (swap! app-state update-in [:freesounds name] concat data))

(defn set-as-freesound-queries! [app-state data]
  (doseq [list data] (set-as-freesound-query! app-state list)))

(defn parse-query-string [query-string]
  (some-> query-string
          (str/replace-first "?" "")
          (str/split "&")
          (->> (remove empty?)
               (map #(str/split % "="))
               (into {}))
          walk/keywordize-keys))
