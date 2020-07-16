(ns algoradio.about
  (:require [cljs.user :refer [spy]]
            [algoradio.state :refer [app-state]]
            [clojure.string :as str]))

(defn toggle-show-functions []
  (swap! app-state update ::show-functions not))

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
                [:a {:href mp3 :target "_blank"} "mp3"]]
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
        (map (fn [url]
               (-> url (str/split #",")
                   (->> (map (fn [url*]
                               [:a {:href url*
                                    :key url*
                                    :target "_blank"} url*])))))))])


(defn archive-data [archive]
  [:div {:class "about__works"}
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
   [:button {:class "about__close" :on-click toggle-show-about} "X"]
   [:h2 {:class "about__title"} "¿Qué es Camposónico?"]
   [:p {:class "about__p"}
    [:a {:class "link" :href "https://github.com/diegovdc/camposonico/" :target "_blank"}
     [:i "Camposónico (radio algorítmica)"] "es una aplicación"] " para escuchar e intervenir paisajes sonoros y música experimental, haciendo posible una interacción con el sonido que va desde el simple autoplay hasta la experimentación creativa mediante el uso de código."]
   [:p {:class "about__p"} "Sus objetivos son los siguientes:"]
   [:ol {:class "about__ol"}
    [:li "Facilitar la exploración del archivo de paisajes sonoros de "
     [:a {:href "https://freesound.org"
          :class "link"
          :target "_blank"} "Freesound.org"] "."]
    [:li "Invitar a un juego de la escucha donde distintos espacios y tiempos puedan convivir y converger en un mismo acontecimiento."]
    [:li "Fomentar la creación y desarrollo de un "
     [:a {:class "link" :href "#about__archive" :target "_blank"}
      "archivo de música experimental"] " expresamente creada para ser escuchada junto con los paisajes sonoros."]
    [:li "Invitar al escucha a jugar con ambos archivos y explorarlos creativamente mediante el código."]]
   [:h2 {:class "about__title"} "¿Cómo usar Camposónico?"]
   [:h3 {:class "about__subtitle"} "Método 1: Interfaz gráfica"]
   [:p {:class "about__p"}
    "La manera más sencilla de usarlo es ingresando alguna término en la barra de búsqueda que se encuentra en la esquina superior derecha de la ventana. Es posible ingresar cualquier número de búsquedas, y controlar cuántas grabaciones se reproducirán al mismo tiempo en un momento dado. La elección de los audios particulares es aleatoria."]
   [:p {:class "about__p"}
    "Cuando se cargan los resultados de una búsqueda, estos aparecerán debajo de la barra de búsqueda. Aparecerán también un \"input\" o \"control\" numérico y un número más su derecha, algo como \"1/15\". El input permite controlar la cantidad de capas de el tipo de paisaje seleccionado, cero calla todas las capas,y cualquier numero mayor a 0 determinará que se activen tantas capas como se esepcifica" [:b "*"]". El otro número indica la cantidad de capas distintas que están disponibles. Sí hay más capas disponibles éstas se irán descargando paulatinamente."]
   [:p {:class "about__p"}
    "Cada que una capa se activa aparecerá un cuadrito de color. Al dar click en este cuadrito se mostrará la información correspondiente a dicha capa (autor, descripción, link, etc.)."]
   [:p {:class "about__p"}
    [:b "*Nota: "] "Es posible que el número que controla la cantidad de capas no corresponda con el número de capas que suenan actualmente (y que se muestra en la interfaz con los cuadritos de colores). Esto se debe a ciertas limitaciones en el servicio de Freesound.org. Esto generalmente sucede cuando se activan o desactivan muchas capas rápidamente."]
   [:h3 {:class "about__subtitle"} "Método 2: Interfaz de código"]

   [:p {:class "about__p"}
    "Camposónico también ofrece una interfaz de código, en el lenguaje JavaScript, que permite una interacción más creativa, efectiva y activa con los materiales sonoros. Esta interfaz aun se encuentra en desarrollo, por lo que quien tenga interés puede colaborar o sugerir funcionalidades en"
    [:a {:class "link"
         :href "https://github.com/diegovdc/camposonico/issues"
         :target "_blank"}
     " el repositorio que aloja el código de la aplicación."]]
   [:button {:class "about__button" :on-click #(toggle-show-functions)}
    (if-not (@app-state ::show-functions)
      "Mostrar funciones"
      "Ocultar funciones")]
   (when (@app-state ::show-functions)
     [:div
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
      (code-block
       "initHydra"
       [:span "Activa el sintetizador de visuales hydra-synth. "
        [:a {:class "link"
             :href "https://github.com/ojack/hydra"
             :target "_blank"}
         "Más información"]]
       "initHydra()\nosc(1,1,1).out()")])
   [:div {:id "about__archive"}
    [:h2 {:class "about__title"} "El archivo de música experimental"]
    [:p {:class "about__p"}
     "Camposónico cuenta con un archivo de música experimental concebida para ser escuchada con y sin los paisajes sonoros. Este archivo es abierto y se encuentra en constante crecimiento."]
    [:p {:class "about__p"}
     "Eventualmente se proveeran más funciones en al interfaz de código la cuáles permitirán realizar otros tipos de escucha de esta música."]
    [:p {:class "about__p"}
     "Para activar la reproducción del archivo basta con activar la casilla \"Agregar música\" que se encuentra debajo de la barra de búsqueda."]
    [:p {:class "about__p"}
     "A continuación se muestra la lista completa de piezas actualmente en el archivo."]

    (archive-data archive)]])
