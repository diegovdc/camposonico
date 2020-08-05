(ns algoradio.inputs)

(defn input [type id label on-change]
  [:label {:class "form-label" :for id}
   [:p label]
   [:input {:class "form-input"
            :type :text
            :id id
            :on-change (fn [ev] (on-change ev id))}]])

(defn submit
  [label on-click]
  [:div {:class "form-submit"}
   [:button {:type :submit
             :on-click on-click} label]])
