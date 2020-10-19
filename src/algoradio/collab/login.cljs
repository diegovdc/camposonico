(ns algoradio.collab.login
  (:require [algoradio.collab.core :as collab]
            [algoradio.alert :as alert]
            [algoradio.common :as common]
            [algoradio.state :refer [app-state]]
            [algoradio.websockets :refer [send-message!]]
            [clojure.string :as str]))

(defn validate-create-session [data]
  (let [missing-fields (->> data (filter (comp nil? second)) (map first))
        messages {:session-name "please give a name to you session"
                  :password "please set a password"
                  :username "please choose a username"}]
    (when-not (empty? missing-fields)
      (str "One or more fields in the form are empty: "
           (str/join ", " ((apply juxt missing-fields) messages))))) )

(defn validate-join-session [data]
  (let [missing-fields (->> (dissoc data :password :session-action)
                            (filter (comp
                                     #(or (nil? %) (empty? %))
                                     second))
                            (map first))
        messages {:session-name "please select a session"
                  :username "please choose a username"}]
    (when-not (empty? missing-fields)
      (str "One or more fields in the form are empty: "
           (str/join ", " ((apply juxt missing-fields) messages))))) )

(defn send-start-session!
  [data]
  (let [validation-message (case (data :session-action)
                             :create (validate-create-session data)
                             :join (validate-join-session data))]
    (if-not validation-message
      (send-message! collab/conn :start-session
                     (assoc data :client-id (@collab/state ::collab/ws-id)))
      (alert/create-alert! app-state :error validation-message))))


(defn created-session-alert [{:keys [session-action session-path password]}]
  (let [password* (if (@app-state ::show-password?) password
                      (str/join (repeat (count password) "*")))
        finish-joining #(do (swap! app-state update ::hide-login assoc true)
                            (.focus (@app-state :algoradio.editor/instance)))]
    (if (= session-action :create)
      [:div
       [:h2 "Session has been created:"]
       [:p "Session link: " (collab/make-collab-url session-path)]
       [:p "Password: " password* " "
        [:span {:class "collab-login__button-container" :style {:display "inline"}}
         [:button {:style {:display "inline-flex"}
                   :on-click #(swap! app-state update ::show-password? not)}
          (if (@app-state ::show-password?) "Hide password" "Show password")]]]
       [:div {:class "collab-login__button-container"}
        [:button {:on-click finish-joining}
         "Click to continue to live editor"]]]
      (do (js/setTimeout finish-joining 500) "Joining!"))))

(defn render-create-session
  [create? set-session-name set-password set-username send-start-session-button]
  (when create?
    [:div
     [:h3 "Creating a session"]
     [:p {:class "blue"} "By filling the following form and you will be provided with a link to share so other people can join in to the fun"]
     [:p [:small "(Note: no data will persisted into any database)"]]
     [:label "Session name:"
      [:input {:on-change set-session-name} ]]
     [:label "Password "
      [:small "(People with the password will be able to collaborate with writting the code)"]
      [:input {:on-change set-password :type "password"}]]
     [:label "Your name:"
      [:input {:on-change set-username}]]
     (send-start-session-button "Create session")]))

(defn render-join-session
  [join? set-session-name set-password set-username send-start-session-button]
  (when join?
    [:div
     [:h3 "Joining a session"]
     [:label "Choose a session:"
      [:span {:class "collab-login__select-container"}
       [:select {:on-change set-session-name
                 :value (@app-state ::session-name)}
        (concat [[:option {:key -1 :value nil :default true} "Select a session"]]
                (map (fn [session-name] [:option {:key session-name} session-name])
                     (get @collab/state ::collab/available-sessions [])))]]]
     [:label
      "Password "
      [:small "(Optional: people with the password will be able to collaborate with writting the code)"]
      [:input {:on-change set-password :type "password"}]]
     [:label "Your name:" [:input {:on-change set-username} ]]
     (send-start-session-button "Join session")]))

(comment (@collab/state ::collab/available-sessions))

(defn login [app-state]
  (let [create? (= :create (@app-state ::session-action))
        join? (= :join (@app-state ::session-action))
        query-string (common/parse-query-string js/window.location.search)
        set-session-name  #(swap! app-state assoc
                                  ::session-name (-> % .-target .-value))
        set-password #(swap! app-state assoc
                             ::password (-> % .-target .-value))
        set-username #(swap! app-state assoc
                             ::username (-> % .-target .-value))
        send-start-session-button
        (fn [text]
          [:div {:class "collab-login__button-container"}
           [:button {:on-click #(send-start-session!
                                 {:session-action (@app-state ::session-action)
                                  :session-name (@app-state ::session-name)
                                  :password (@app-state ::password)
                                  :username (@app-state ::username)})}
            text]])]
    (when (and (not (@app-state ::login-initialized?))
               (:session query-string))
      (swap! app-state assoc
             ::session-name (:session query-string)
             ::session-action :join
             ::login-initialized? true))
    (cond
      (get @app-state ::hide-login false) nil
      (@app-state :algoradio.collab.init/login-data)
      [:div {:class "collab-login"}
       (created-session-alert (@app-state ::login-data))]
      :else
      [:div {:class "collab-login"}

       [:div {:class "collab-login__container"}
        [:h2 "Camposonico Live"]
        [:div
         [:div {:class "collab-login__button-container"}
          [:button {:class (when create? "selected")
                    :on-click #(swap! app-state assoc ::session-action :create)}
           "Create a session"]
          [:button {:class (when join? "selected")
                    :on-click #(swap! app-state assoc ::session-action :join)}
           "Join an existing session"]]]
        (render-create-session create?
                               set-session-name
                               set-password
                               set-username
                               send-start-session-button)
        (render-join-session join?
                             set-session-name
                             set-password
                             set-username
                             send-start-session-button)]])))
