(ns algoradio.chat
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [cljs.reader :refer [read-string]]
            [cljs.core.async.impl.protocols :refer [closed?]]
            [reagent.core :as r]
            [haslett.client :as ws]
            [haslett.format :as fmt]))

(defonce out-chan (a/chan))
(defonce in-chan (a/chan))

(defonce conn (ws/connect "ws://localhost:3456/ws"
                      {:sink out-chan
                       :source in-chan}))



#_(a/go (let [stream (a/<! conn)
              socket (stream :socket)]
          (js/console.log socket)
          #_(set! (.. socket -onclose) identity)))

(comment
  (a/close! in-chan))

(defn send-message!
  ([conn type msg] (send-message! conn type msg {}))
  ([conn type msg opts]
   (a/go (a/>! out-chan {:type type :msg msg :opts opts}))))

(defonce ws-id (atom nil))

(defonce state (r/atom {::messages ()
                        ::textarea-height 23}))

(defn on-new-message [msg]
  (swap! state update ::messages conj msg)
  (js/console.log (@state ::messages)))

(defn router [msg]
  (condp = (:type msg)
    :ping (send-message! conn :pong nil)
    :id (reset! ws-id (:msg msg))
    :chat (on-new-message msg)
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
  (js/console.log "a" (not (.-keyCode ev)) "b" (= 13 (.-keyCode ev)))
  (when (or (not (.-keyCode ev)) (= 13 (.-keyCode ev)))
    (send-message! conn
                   :chat (@state ::chat-message)
                   {:user-id @ws-id :date (js/Date.now)})
    (swap! state merge {::chat-message nil ::textarea-height 21})))

(defn main []
  (r/create-class
   {:component-did-update
    (fn [_ _]
      (let [msg-container (js/document.getElementById "chat-messages-container")]
        (.scrollTo msg-container 0 (.-scrollHeight msg-container))))
    :reagent-render
    (fn []
      [:div {:class "chat"}
       [:div {:class "chat__container"}
        [:div {:id "chat-messages-container"
               :class "chat__messages-container"}
         (->> (@state ::messages)
              (map (fn [{:keys [msg]
                        {:keys [date username]} :opts}]
                     [:div {:key (str username "_" date)
                            :class "chat__message-container"}
                      [:p {:class "chat__message-text"}
                       [:span {:class "chat__username"} (or username (apply str "anonymous-"
                                                                            (->> @ws-id (take-last 3))))]
                       (str ": " msg)]]))
              reverse)]
        [:div {:class "chat__icons"}
         [:i {:class (str "fas chat_toggle-button "
                          (str "fa-comment" (when (@state ::toggle-show-all) "-slash")))} ]
         [:i {:class (str "fas chat__history "
                          (str "fa-history" (when true " show ")))}]]
        [:form {:on-submit submit-message
                :on-key-up submit-message}
         [:textarea {:id "chat-message"
                     :class "chat__textarea"
                     :value (@state ::chat-message)
                     :style {:height (@state ::textarea-height)
                             :padding-top 1
                             :padding-bottom 1}
                     :on-change (fn [ev]

                                  (js/console.log "k" (-> ev .-nativeEvent .-inputType (= "insertLineBreak")))
                                  (when-not (-> ev .-nativeEvent .-inputType (= "insertLineBreak"))
                                    (swap! state assoc ::chat-message (-> ev .-target .-value) )
                                    (swap! state assoc
                                           ::textarea-height
                                           (-> ev .-target .-scrollHeight))))}]
         [:button {:class "chat__submit-button"
                   :id "chat-submit-button"
                   :on-click submit-message}
          [:span ">"]]]]])}))

(main)

(comment

  (send-message! conn :set-username "Diego" {:user-id @ws-id})
  (send-message! conn :chat "Holis" {:user-id @ws-id :date (js/Date.now)})

  (a/go (a/>! in-chan {:msg "Hello World"}))

  )
