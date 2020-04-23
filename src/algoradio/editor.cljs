(ns algoradio.editor
  (:require [clojure.string :as str]))

(do
  (defn get-cm [app-state]
    (-> @app-state ::instance .getCodeMirror))

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
       :code (subvec text start (inc end)) }))
  (defn mark-text! [{:keys [start end]} cm]
    (let [mark (.markText cm
                          (clj->js {:line start :ch 0})
                          (clj->js {:line (inc end) :ch 0})
                          (clj->js {:className "editor__flashed-text"}))]
      (js/setTimeout #(.clear mark) 1000)
      mark))
  (defn eval! [cm]
    (let [block (get-block cm)]
      (mark-text! block cm)
      (js/eval (->> block :code (str/join "\n")))
      (js/console.log (->> block :code (str/join "\n")))))
  #_(get-block cm))