(ns algoradio.chat
  (:require [algoradio.alert :refer [create-alert!]]
            [algoradio.state :refer [app-state]]
            [algoradio.collab.core :as collab]
            [algoradio.websockets
             :refer
             [make-conn make-receiver on-message send-message!]]
            [cljs.core.async :as a]
            [reagent.core :as r]
            [algoradio.config :as config])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defonce clock (r/atom 0))

(defonce start-clock! (memoize (fn [clock]
                                 (js/console.log "Starting clock")
                                 (go-loop []
                                   (a/<! (a/timeout 1000))
                                   (swap! clock + 1000)
                                   (recur)))))
(defn submit-message [ev]
  (.preventDefault ev)
  (when (or (not (.-keyCode ev)) (= 13 (.-keyCode ev)))
    (collab/send-chat-message! (@collab/state ::chat-message))
    (swap! collab/state merge {::chat-message nil ::textarea-height 23})))

(defn get-latest-messages [show-latest-messages? now messages]
  (js/console.log show-latest-messages?)
  (if show-latest-messages?
    (take-while #(-> % :received-timestamp (> (- now 10000))) messages)
    messages))

(defn chat []
  (let [show-chat? (@collab/state ::show-chat?)
        show-latest-messages? (@collab/state ::show-latest-messages?)]
    [:div
     (when show-chat?
       [:div {:id "chat-messages-container"
              :class "chat__messages-container"}
        (->> (@collab/state ::messages)
             (get-latest-messages show-latest-messages? @clock)
             (map (fn [{:keys [timestamp username message]}]
                    [:div {:key (str username "_" timestamp)
                           :class "chat__message-container"}
                     [:p {:class "chat__message-text"}
                      [:span {:class "chat__username"}
                       (or username (apply str "anonymous-"
                                           (->> (@collab/state ::collab/ws-id)
                                                (take-last 3))))]
                      (str ": " message)]]))
             reverse)])
     [:div {:class "chat__form-container"}
      [:div {:class "chat__icons"}
       [:i {:class (str "fas chat_toggle-button "
                        (str "fa-comment" (when show-chat? "-slash")))
            :title (if show-chat? "Hide chat" "Show chat")
            :on-click (fn [] (swap! collab/state
                                   #(-> %
                                        (update ::show-chat? not)
                                        (assoc ::show-latest-messages? false))))}]
       (when show-chat?
         [:i {:class (str "fas chat__history "
                          (str "fa-history" (when-not show-latest-messages? " show ")))
              :title (if show-latest-messages?
                       "Show full chat history"
                       "Only show latest messages")
              :on-click #(swap! collab/state update ::show-latest-messages? not)}])]
      (when show-chat?
        [:form {:class "chat__form"
                :on-submit submit-message
                :on-key-up submit-message}
         [:textarea {:id "chat-message"
                     :class "chat__textarea"
                     :value (@collab/state ::chat-message)
                     :style {:height (@collab/state ::textarea-height)
                             :padding-top 1
                             :padding-bottom 1}
                     :on-change (fn [ev]
                                  (when-not (-> ev .-target .-value last (= "\n"))
                                    (swap! collab/state assoc ::chat-message (-> ev .-target .-value))
                                    (swap! collab/state assoc
                                           ::textarea-height
                                           (-> ev .-target .-scrollHeight))))}]
         [:button {:class (str "chat__submit-button "
                               (when (< 0 (count (@collab/state ::chat-message)))
                                 "not-empty"))
                   :id "chat-submit-button"
                   :on-click submit-message}
          [:span "Send"]]])]]))

(defn valid-username? [username] (and (string? username) (> (count username ) 0)))

(comment
  ;;  Currently unused, might be used later
  (defn username-field []
    [:div {:class "chat_login"}
     [:label {:class "chat__username-input-label"} "Choose a username to start chatting"]
     [:form {:class "chat__form"
             :on-submit
             (fn [ev]
               (.preventDefault ev)
               (let [username (@state ::username-field)]
                 (when (valid-username? username)
                   (println username)
                   (send-message! conn :set-username username {:user-id (@state ::ws-id)}))))}

      [:input {:on-change (fn [ev]
                            (swap! state assoc ::username-field (-> ev .-target .-value)))}]
      [:button {:class "chat__submit-button" :type "submit"} "Send"]]]))

(defn main []
  (reset! clock (js/Date.now))
  (start-clock! clock)
  (r/create-class
   {:component-did-update
    (fn [_ _]
      (let [msg-container (js/document.getElementById "chat-messages-container")]
        (when msg-container
          (.scrollTo msg-container 0 (.-scrollHeight msg-container)))))
    :reagent-render
    (fn []
      [:div {:class "chat"}
       [:div {:class "chat__container"}
        (chat)]])}))

(comment
  (-> @collab/state )
  (send-message! (@collab/state ::collab/conn) :set-username "Diego" {:user-id (@state ::ws-id)})
  (send-message! (@collab/state ::collab/conn) :chat-message "Holis" {:user-id (-> @collab/state ::collab/ws-id) :date (js/Date.now)})
  (a/go (a/>! ((a/<! conn) :source) {:msg "Hello World"})))
