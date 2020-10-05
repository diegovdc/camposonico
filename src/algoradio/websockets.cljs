(ns algoradio.websockets
  (:require [cljs.core.async :as a]
            [cljs.core.async.impl.protocols :refer [closed?]]
            [cljs.reader :refer [read-string]]
            [haslett.client :as ws]))


(defn send-message!
  ([conn type msg] (send-message! conn type msg {}))
  ([conn type msg opts]
   (a/go (a/>! ((a/<! conn) :sink) {:type type :msg msg :opts opts}))))

(defn on-message [router msg]
  (try (let [msg (read-string msg)] (router msg))
       (catch :default e (js/console.error "Could not read message" e msg)))
  (js/console.debug "Got message" (:type (read-string msg))))

(defn make-receiver [conn router]
  (a/go-loop []
    (if (and (not (closed? ((a/<! conn) :source))) (ws/connected? (a/<! conn)))
      (let [msg (a/<! ((a/<! conn) :source))]
        (when msg (on-message router msg))
        (recur)))
    (js/console.info "Channel has been closed")))

(defn make-conn [path]
  (js/console.log "Connecting")
  (ws/connect path #_{:sink out-chan :source in-chan}))
