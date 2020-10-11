(ns build.html.core
  (:require [build.html.index :as index]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [hiccup.core :refer [html]]
            [clojure.java.io :as io]))

(defn get-manifest! []
  (edn/read-string (slurp "public/js/compiled/manifest.edn")))

(do
  (defn get-version! []
    (-> (slurp "package.json")
        json/read-str
        (get "version")))
  (get-version!))

(defn build-index! [version]
  (->> (get-manifest!)
       (filter #(= (:name %) :main))
       first
       :output-name
       (index/template version)
       html
       (spit "public/index.html")))

(defn build-live! [version]
  (let [path "public/live/index.html"]
    (io/make-parents path)
    (->> (get-manifest!)
         (filter #(= (:name %) :main))
         first
         :output-name
         (index/template version)
         html
         (spit path))))

(defn -main []
  (let [version (get-version!)]
    (build-index! version)
    (build-live! version))
  (println "Html files have been built"))
