(ns algoradio.about
  (:require [cljs.user :refer [spy]]
            [algoradio.state :refer [app-state]]
            [clojure.string :as str]))

(defn print-title [title year]
  [:h5 {:class "about__works-work-title"}
   (str title
        (when year (str " (" year ")")))])

(defn print-works [works]
  (->> works
       (sort-by :title)
       (map (fn [{:keys [title year description tags place mp3 url]}]
              [:div {:key title
                     :class "about__works-work"}
               (print-title title year)
               [:p {:class "about__works-work-link"}
                [:a {:href mp3} "mp3"]]
               [:p {:class "about__works-work-description"}
                description]
               (when (not (empty? tags))
                 [:p {:class "about__works-work-tags"}
                  [:b "Tags: "] (str/join ", " tags)])
               [:p {:class "about__works-work-place"} place]]))))

(defn print-author-links [works]
  [:div {:class "about__works-author-links"}
   (->> works
        (map :url)
        (remove nil?)
        set
        (map (fn [url] [:a {:href url :key url} url])))])


(defn archive-data [archive]
  [:div {:class "about__works"}
   [:p [:b "Obras y artistas"]]
   [:div {:class "about__works-container"}
    (->> archive
         (group-by :author)
         (sort-by first)
         (map (fn [[author works]]
                [:div {:key author}
                 [:h4 {:class "about__works-work-author"} author]
                 (print-author-links works)
                 (print-works works)])))]])

(defn code-block [fn-name fn-description code]
  [:div {:class "about__code-block"}
   [:p {:class "about__p about__code-block-fn"}
    [:span {:class "about__code-block-fn-name"} fn-name]
    fn-description]
   [:pre [:code {:class "language-javascript"} code]]])

(defn toggle-show-about [_]
  (swap! app-state update ::show-about? not))

(defn main [archive]
  [:div {:class "about"}
   [:button {:class "about__close"
             :on-click toggle-show-about} "X"]
   [:h2 {:class "about__title"} "¿Qué es Camposónico?"]
   [:p {:class "about__p"} [:i "Camposónico (radio algorítmica) "] "es una aplicación para escuchar e intervenir paisajes sonoros y música experimental, haciendo posible una interacción con el sonido que va desde el simple autoplay hasta la experimentación creativa mediante el uso de código."]
   [:p {:class "about__p"} "Sus objetivos son los siguientes:"]
   [:ol {:class "about__ol"}
    [:li "Facilitar la exploración del archivo de paisajes sonoros de " [:a {:href "https://freesound.org" :class "link"} "Freesound.org"] "."]
    [:li "Invitar a un juego de la escucha donde distintos espacios y tiempos puedan convivir y converger en un mismo acontecimiento."]
    [:li "Fomentar la creación y desarrollo de un archivo de música experimental expresamente creada para ser escuchada junto con los paisajes sonoros."]
    [:li "Invitar al escucha a jugar con ambos archivos y explorarlos creativamente mediante el código."]]
   [:h2 {:class "about__title"} "¿Cómo usar Camposónico?"]
   [:p {:class "about__p"} "La manera más sencilla de usarlo es ingresando alguna término en la barra de búsqueda que se encuentra en la esquina superior derecha de la ventana. Es posible ingresar cualquier número de búsquedas, y controlar cuántas grabaciones se reproducirán al mismo tiempo en un momento dado. La elección de los audios particulares es aleatoria."]
   [:p {:class "about__p"} "Camposónico también ofrece una interfaz de código, en el lenguaje JavaScript, que permite una interacción más creativa, efectiva y activa con los materiales sonoros. Esta interfaz aun se encuentra en desarrollo, por lo que los interesado puede colaborar o sugerir funcionalidades en" [:a {:href "https://github.com/diegovdc/algoradio/issues"} " el repositorio que aloja el código de la aplicación "]]

   [:h3 {:class "about__subtitle"} "Funciones básicas"]
   (code-block "load" "Carga un tipo de paisaje sonoro. La búsqueda en freesound.org se hace por \"tags\"." "load(\"veracruz\")")
   (code-block "play" "Agrega una capa de este paisaje sonoro" "play(\"veracruz\")")
   (code-block "stop" "Quita una capa de este paisaje sonoro" "stop(\"veracruz\")")
   (code-block "showInfo" "Muestra la información de los sonidos y la música que suenan actualmente. El parámetro numérico determina la velocidad con la que se pasa de una track al siguiente (en milisegundos)" "showInfo(7000)")
   (code-block "setInfoPosition" "Determina la posición en la pantalla donde aparecerá el texto" "setInfoPosition(\"center\") // otras opciones \"left\", \"right\" \"top\" \"bottom\"")
   (code-block
    "randNth" "Elige un elemento de un array al azar cada 5 segundos"
    "paisajes = [\"ocean\", \"bright\", \"sand\", \"forest\", \"faraway\", \"nightingale\"]

paisajes.forEach(p => load(p))

paisajes.forEach(p => {play(p); play(p)}) // inicializar dos paisajes de cada tipo

posiciones = [\"centro\", \"abajo\", \"arriba\", \"izquierda\", \"derecha\"]


// Cada 20 segundos seleccionar un paisaje al azar para activar
// y uno para desactivar de la lista `paisajes`,
// también, cambiar la posición de la información
interval = setInterval(
    () => {
        play(randNth(paisajes))
        stop(randNth(paisajes))
        setInfoPosition(randNth(posiciones))
    },
    20000
)

clearInterval(interval) // detener los cambios")

   [:h2 {:class "about__title"} "El archivo de música y sonido"]
   (archive-data archive)
   ])
