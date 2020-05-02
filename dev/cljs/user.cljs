(ns cljs.user)

(defn hello [] "holax")

(defn make-spy [printer]
  (fn [& args]
    (let [off (some #(= % :off) args)
          tap (some #(= % :tap) args) ;; does not print the input value
          quiet (some #(= % :quiet) args) ;; executes the functions but does not log anything
          println* (some #(= % :pln) args)
          printer* (if println* println printer)
          val (last args)]
      (if (or off)
        val ;; returns val when muted and/or not on debug

        (let [msgs (butlast (filter (comp not #{:tap :off :quiet :pln}) args))]
          ((if quiet identity printer*)
           (concat
            (->> msgs (mapv (fn [msg]
                              (cond
                                  (keyword? msg) (or (msg val) msg)
                                  (fn? msg) (msg val)
                                  :else msg))))
            (if tap [] [val])))))
      val)))

(defn spy
  "Example: (spy :some-symbol some-func return-value) => return-value ;; logs everything to the console"
  [& args]
  (apply
   (make-spy #(doseq [x %] (js/console.debug x)))
   args))

(defn spy->
  "Spy for a thread first macro"
  [& args]
  (apply spy (reverse args)))

(def data (atom {}))

(defn capture
  ([key val] ((capture key) val))
  ([key]
   (fn [val]
     (swap! data #(assoc % key val))
     :captured)))

(defn capture-all
  ([key val] ((capture-all key) val))
  ([key]
   (fn [val]
     (swap! data #(update % key conj val))
     :captured)))

(defn tap
  [& args]
  (apply spy (concat [:tap] args)))

(defn tap->
  [& args]
  (apply spy-> (concat [:tap] args)))

(defn qtap
  [& args]
  (apply spy (concat [:tap :quiet] args)))

(defn qtap->
  [& args]
  (apply spy-> (concat [:tap :quiet] args)))

(defn tc
  "Tap capture"
  ([key] (partial tc key))
  ([key x]
   (tap (capture key) x)))

(defn tca
  "Tap capture-all"
  ([key] (partial tc key))
  ([key x]
   (tap (capture-all key) x)))

(defn clear-data
  ([] (reset! data nil))
  ([key] (swap! data update key nil)))

(comment
  (do
    (defmacro spy2
      [& body]
      `(let [x# ~body
             ;; tag#
             d# ~ {:line (:line (meta &form)) :ns *ns*}]
         (println d#)
         (spy '~body d# x#)
         x#))


    (->>  "hola" (spy2 :tag "my func"))))
