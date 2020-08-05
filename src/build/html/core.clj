(ns build.html.core
  (:require [build.html.index :as index]
            [clojure.edn :as edn]
            [hiccup.core :refer [html]]))

(defn get-manifest! []
  (edn/read-string (slurp "public/js/compiled/manifest.edn")))


(defn build-index! []
  (->> (get-manifest!)
       (filter #(= (:name %) :main))
       first
       :output-name
       index/template
       html
       (spit "public/index.html")))

(defn -main []
  (build-index!))
