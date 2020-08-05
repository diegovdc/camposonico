(ns algoradio.api
  (:require ["axios" :as axios]
            [algoradio.config :as config]))

;;;; utils
(defn get-error [err] (try (-> err .-response .-data)
                           (catch js/TypeError e
                             (do (js/console.error "Unknown Error" e)
                                 "There was an unknown error"))))

#_(get-error (clj->js {:response {:data "ups"}}))

(defn get-data [res] (-> res .-data))

(defn post-history [data]
  (axios/post (str config/api "/history")
              (clj->js (update data :history #(js/JSON.stringify (clj->js %))))))

(defn get-history [id]
  (axios/get (str config/api "/history/" id)))

(comment (-> (get-history "1-author-title")
             (.then get-data)
             (.then println)
             (.catch get-error)))
