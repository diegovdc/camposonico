(ns algoradio.axios)

(defn response->data [response]
  (let [data (-> response
                 (js->clj :keywordize-keys true)
                 :data)]
    data))
