(ns algoradio.websockets
  (:require [cljs.core.async :as a]
            [cljs.core.async.impl.protocols :refer [closed?]]
            [cljs.reader :refer [read-string]]
            [haslett.client :as ws]))


(comment
  ;; NOTE {:sink out-chan :source in-chan}
  )

(defn send-message!
  ([conn type msg] (send-message! conn type msg {}))
  ([conn type msg opts]
   (a/go (a/>! ((a/<! conn) :sink) (assoc msg :type type :opts opts)))))

(defn on-message [router msg]
  (try (let [msg (read-string msg)]
         (js/console.debug "algoradio.websockets/on-message" msg)
         (router msg))
       (catch :default e (js/console.error "Could not read message" e msg))))

(defn make-receiver [conn router]
  (a/go-loop []
    (if (and (not (closed? ((a/<! conn) :source))) (ws/connected? (a/<! conn)))
      (let [msg (a/<! ((a/<! conn) :source))]
        (when msg (on-message router msg))
        (recur)))
    (js/console.info "Channel has been closed")))

(defn make-conn [path]
  (js/console.debug "[ws] Making connection")
  (ws/connect path))
