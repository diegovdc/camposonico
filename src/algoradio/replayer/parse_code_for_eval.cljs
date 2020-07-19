(ns algoradio.replayer.parse-code-for-eval
  (:require [clojure.string :as str]))

(declare make-regex-to-remove-code remove-full-function-from-code)

(defn remove-function-calls
  "Receives a list of function names to exclude"
  [excluded-function-names code]
  (let [functions-to-remove (->> code
                                 (re-seq (make-regex-to-remove-code
                                          excluded-function-names))
                                 (map first))]
    (reduce remove-full-function-from-code code functions-to-remove)))

(comment (remove-function-calls
          ["play" "stop"]
          "play  (\"holi boli\", {index:rrand()})
          //hola
          load(\"\")

          stop()"))

(defn make-regex-to-remove-code
  "Receives a list of function names to exclude and returns a regex pattern
  that can do that job"
  [excluded-function-names]
  (let [excluded-group (str/join "|" excluded-function-names)]
    (re-pattern (str "(^|\\s)(" excluded-group ")(\\s*)\\(((.|\n)*?)\\)"))))

(defn find-first-balanced-closing-paren
  [code start-index]
  (let [code* (-> code (str/split #"")
                  (subvec (inc start-index)) ;; `inc` because the split leaves an unwanted exmpty string at the begining thus we need to adjust the starting point
                  (->> (map-indexed vector)))]
    (reduce (fn [acc [i char]]
              (let [{:keys [opening closing] :as acc*}
                    (condp = char
                      "(" (update acc :opening inc)
                      ")" (update acc :closing inc)
                      acc)]
                (if (and (= opening closing) (> opening 0))
                  (reduced (+ start-index i))
                  (assoc acc* :index i))))
            {:opening 0 :closing 0 :index -1}
            code*)))

(comment (find-first-index-of-balanced-parens "play(hola()) bola" 0))

(defn remove-full-function-from-code
  "The `regexed-function` (a string) might not be properly closed because of a
  lack of balanced parens, the regex is not powerful enough :(,
  so we need to find the closing parens"
  [code regexed-function]
  (let [start-index (str/index-of code regexed-function)
        last-index (find-first-balanced-closing-paren code start-index)
        full-function (subs code start-index (inc last-index))]
    (str/replace code full-function "")))

(comment (remove-full-function-from-code "play(hola(((())))) bola" "play(hola()"))

(comment
  (re-seq #"(^|\s)(play|stop)\(((.|\n)*?)\)" "play(\"holi boli\") hola load(\"\")")
  (re-seq (re-pattern (str "(^|\\s)(" "play|stop" ")\\(((.|\n)*?)\\)")) "play(\"holi boli\") hola load(\"\")"))
