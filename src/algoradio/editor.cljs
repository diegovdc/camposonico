(ns algoradio.editor
  (:require [clojure.string :as str]
            [algoradio.history :as history]
            ["yjs" :as Y]
            [algoradio.config :as config]
            ["y-websocket" :refer [WebsocketProvider]]
            ["y-codemirror" :refer [CodemirrorBinding]]
            ["react-codemirror2" :as react-codemirror]
            ["codemirror/mode/javascript/javascript"]
            ["codemirror/addon/display/fullscreen"]))

(defn init [app-state editor session-name]
  (let [y-doc (Y/Doc.)
        provider (WebsocketProvider. "wss://demos.yjs.dev"
                                     (str "camposonico-live-session-" session-name)
                                     y-doc)
        y-text (.getText y-doc "codemirror")
        binding (CodemirrorBinding. y-text editor (.-awareness provider))]
    (.on (-> binding .-cm #_js/console.log) "change"
         (fn [_ change]
           (history/add-editor-change! app-state
                                       (get @app-state ::key)
                                       change)))
    ))


(declare get-cm! eval-block! eval-line-or-selection!)

(defn main [app-state is-live? user-data send-eval-event!]
  (let [session-name (:session-name user-data)]
    [:div {:key (get @app-state ::key)
           :id "editor-container"
           :class "editor"
           :on-key-down
           (fn [e]
             (let [editor-id (get @app-state :algoradio.editor/key)
                   ctrl? (.-ctrlKey e)
                   enter? (= 13 #_enter (.-keyCode e))
                   shift? (.-shiftKey e)]

               ;; TODO finish implementing input prevention
               (when (and is-live?
                          (false? (-> @app-state
                                      :algoradio.collab/login-data :valid-password?)))
                     (.preventDefault e))
               (cond
                 (and ctrl? enter?) (do (.preventDefault e)
                                        (eval-block! (get-cm! app-state)
                                                     app-state
                                                     editor-id
                                                     send-eval-event!))

                 (and shift? enter?) (do (.preventDefault e)
                                         (eval-line-or-selection!
                                          (get-cm! app-state)
                                          app-state
                                          editor-id
                                          send-eval-event!)))))}

     [:> (if is-live? react-codemirror/UnControlled react-codemirror/Controlled)
      {:editor-did-mount  (fn [ref]
                            (when is-live? (init app-state ref session-name))
                            (when-not (@app-state ::instance)
                              (swap! app-state assoc ::instance ref)))
       :options {:theme "oceanic-next"
                 :fullScreen true
                 :scrollbarStyle "null"}
       :autoSave false
       :value (-> @app-state (get ::text))
       :on-before-change (when-not is-live?
                           (fn [editor data value]
                             (swap! app-state assoc ::text value)))
       :on-change (fn [text change value]
                    (history/add-editor-change! app-state
                                                (get @app-state ::key)
                                                change))}]]))
(defn get-cm! [app-state]
  (-> @app-state ::instance))

(defn get-text [cm]
  (-> cm .-doc .getValue))

(defn get-cursor [cm] (.getCursor cm))

(defn get-line [cm]
  (let [cursor (.getCursor cm)]
    (.getLine cm (.-line cursor))))

(defn find-first-new-line
  "Direction should be `:up` or `:down`"
  [text-vec direction start-line]
  (let [next-line ({:up dec :down inc} direction)
        at-end? (fn [line] (case direction
                             :up (= line 0)
                             :down (= line (dec (count text-vec)))))]
    (loop [line start-line]
      (cond
        (empty? (str/trim (nth text-vec line ""))) line
        (at-end? line) line
        (-> text-vec (nth (next-line line) "") str/trim empty?) line
        :else (recur (next-line line))))))

(defn get-block [cm]
  (let [text  (str/split (get-text cm) #"\n")
        line (-> cm .getCursor .-line)
        start (find-first-new-line text :up line)
        end (find-first-new-line text :down line)]
    {:start start
     :end end
     :code (str/join "\n" (subvec text start (inc end)))}))

(defn mark-text! [{:keys [start end]} cm]
  (let [mark (.markText cm
                        (clj->js {:line start :ch 0})
                        (clj->js {:line (inc end) :ch 0})
                        (clj->js {:className "editor__flashed-text"}))]
    (js/setTimeout #(.clear mark) 1000)
    mark))

(defn get-line-for-eval [cm line-num]
  (let [line (.getLine cm line-num)]
    {:start line-num
     :end line-num
     :code line}))

(defn get-selection-for-eval [cm from to]
  {:start from
   :end to
   :code (.getSelection cm)})

(defn eval-line-or-selection! [cm app-state editor-id send-eval-event!]
  (let [from (.-line (.getCursor cm "from"))
        to (.-line (.getCursor cm "to"))
        selection? (not= from to)
        {code :code :as mark} (if selection?
                                (get-selection-for-eval cm from to)
                                (get-line-for-eval cm from))]
    (mark-text! mark cm)
    (js/eval code)
    (js/console.log code)
    (history/add-editor-eval! app-state editor-id mark)
    (send-eval-event! mark)
    ))

(defn eval-block! [cm app-state editor-id send-eval-event!]
  (let [block (get-block cm)]
    (mark-text! block cm)
    (js/eval (->> block :code))
    (js/console.log (->> block :code))
    (history/add-editor-eval! app-state editor-id block)
    (send-eval-event! block)))

(defn remove-comment-lines! [app-state]
  (let [text (-> (get @app-state ::text "")
                 (str/split "\n")
                 (->> (remove #(re-find #"^//" %))
                      (drop-while empty?)
                      (str/join "\n")))]
    (swap! app-state #(-> %
                          (update ::key inc)
                          (assoc ::text text)
                          (dissoc ::instance)))
    nil))

(defn clear-all! [app-state]
  (swap! app-state #(-> %
                        (update ::key inc)
                        (assoc ::text "")
                        (dissoc ::instance)))
  nil)
