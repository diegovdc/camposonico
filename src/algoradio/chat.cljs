(ns algoradio.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [clojure.string :as str]
            [cljs.reader :refer [read-string]]
            [cljs.core.async.impl.protocols :refer [closed?]]
            [reagent.core :as r]
            [algoradio.state :refer [app-state]]
            [algoradio.alert :refer [create-alert!]]
            [haslett.client :as ws]
            [haslett.format :as fmt]))

(defonce out-chan (a/chan))
(defonce in-chan (a/chan))

(defonce conn (ws/connect "ws://localhost:3456/ws"
                          {:sink out-chan
                           :source in-chan}))

(defn send-message!
  ([conn type msg] (send-message! conn type msg {}))
  ([conn type msg opts]
   (a/go (a/>! out-chan {:type type :msg msg :opts opts}))))

(defonce ws-id (atom nil))

(defonce state (r/atom {::messages ()
                        ::textarea-height 23
                        ::show-latest-messages? true}))

(defonce clock (r/atom 0))

(defonce start-clock! (memoize (fn [clock]
                                (js/console.log "Starting clock")
                                (go-loop []
                                  (a/<! (a/timeout 1000))
                                  (swap! clock + 1000)
                                  (recur)))))

(defn on-new-message [msg]
  (swap! state update ::messages conj msg)
  (js/console.log (@state ::messages)))

(defn router [msg]
  (condp = (:type msg)
    :ping (send-message! conn :pong nil)
    :id (reset! ws-id (:msg msg))
    :chat (on-new-message msg)
    :username-has-been-set? (if (-> msg :msg :username)
                              (swap! state assoc ::username-data (:msg msg))
                              (do (js/console.error (-> msg :msg :error))
                                  (create-alert!
                                   app-state
                                   :error "Username has already been chosen")))
    (js/console.error "Unknown message type:" msg)))


(defn on-message [msg]
  (try (let [msg (read-string msg)] (router msg))
       (catch :default e (js/console.error "Could not read message" e msg)))
  (js/console.debug "Got message" (:type (read-string msg))))

(defonce receiver
  (a/go-loop []
    (if (and (not (closed? in-chan)) (ws/connected? (a/<! conn)))
      (let [msg (a/<! in-chan)]
        (when msg (on-message msg))
        (recur)))
    (js/console.info "Channel has been closed")))

(defn submit-message [ev]
  (.preventDefault ev)
  (when (or (not (.-keyCode ev)) (= 13 (.-keyCode ev)))
    (send-message! conn
                   :chat (@state ::chat-message)
                   {:user-id @ws-id :date (js/Date.now)})
    (swap! state merge {::chat-message nil ::textarea-height 21})))

(defn get-latest-messages [show-latest-messages? now messages]
  (js/console.log show-latest-messages?)
  (if show-latest-messages?
    (take-while #(-> % :opts :date (> (- now 10000))) messages)
    messages))

(defn chat []
  (let [show-chat? (@state ::show-chat?)
        show-latest-messages? (@state ::show-latest-messages?)]
    [:div
     (when show-chat?
       [:div {:id "chat-messages-container"
              :class "chat__messages-container"}
        (->> (@state ::messages)
             (get-latest-messages show-latest-messages? @clock)
             (map (fn [{:keys [msg]
                       {:keys [date username]} :opts}]
                    [:div {:key (str username "_" date)
                           :class "chat__message-container"}
                     [:p {:class "chat__message-text"}
                      [:span {:class "chat__username"}
                       (or username (apply str "anonymous-"
                                           (->> @ws-id (take-last 3))))]
                      (str ": " msg)]]))
             reverse)])
     [:div {:class "chat__form-container"}
      [:div {:class "chat__icons"}
       [:i {:class (str "fas chat_toggle-button "
                        (str "fa-comment" (when show-chat? "-slash")))
            :title (if show-chat? "Hide chat" "Show chat")
            :on-click (fn [] (swap! state
                                   #(-> %
                                        (update ::show-chat? not)
                                        (assoc ::show-latest-messages? false))))}]
       (when show-chat?
         [:i {:class (str "fas chat__history "
                          (str "fa-history" (when-not show-latest-messages? " show ")))
              :title (if show-latest-messages?
                       "Show full chat history"
                       "Only show latest messages")
              :on-click #(swap! state update ::show-latest-messages? not)}])]
      (when show-chat?
        [:form {:class "chat__form"
                :on-submit submit-message
                :on-key-up submit-message}
         [:textarea {:id "chat-message"
                     :class "chat__textarea"
                     :value (@state ::chat-message)
                     :style {:height (@state ::textarea-height)
                             :padding-top 1
                             :padding-bottom 1}
                     :on-change (fn [ev]
                                  (when-not (-> ev .-target .-value last (= "\n"))
                                    (swap! state assoc ::chat-message (-> ev .-target .-value))
                                    (swap! state assoc
                                           ::textarea-height
                                           (-> ev .-target .-scrollHeight))))}]
         [:button {:class (str "chat__submit-button "
                               (when (< 0 (count (@state ::chat-message)))
                                 "not-empty"))
                   :id "chat-submit-button"
                   :on-click submit-message}
          [:span "Send"]]])]]))

(defn valid-username? [username] (and (string? username) (> (count username ) 0)))

(defn username-field []
  [:div {:class "chat_login"}
   [:form {:on-submit
           (fn [ev]
             (.preventDefault ev)
             (let [username (@state ::username-field)]
               (when (valid-username? username)
                 (println username)
                 (send-message! conn :set-username username {:user-id @ws-id}))))}
    [:label "Choose a username to start chatting"
     [:input {:on-change (fn [ev]
                           (swap! state assoc ::username-field (-> ev .-target .-value)))}]]
    [:button {:type "submit"} "Send"]]])

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
        (if (-> @state ::username-data :username-has-been-set?)
          (chat)
          (username-field))]])}))

(comment
  (send-message! conn :set-username "Diego" {:user-id @ws-id})
  (send-message! conn :chat "Holis" {:user-id @ws-id :date (js/Date.now)})
  (a/go (a/>! in-chan {:msg "Hello World"})))
