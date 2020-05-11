(ns algoradio.convocatoria)

(def titles
  {:es "Convocatoria para música y sonidos"
   :en "Open Call for Music and Sounds"})
(defn s [text] [:span {:class "link"} text])
(def paragraphs
  {:es
   ["Musicos experimentales y creadores sonoros: Se les invita a crear y donar música o sonidos que tengan la capacidad de coexistir con otros ambientes (naturales, humanos, etc.)."
    [:span "Camposónico (radio algorítmica) " "es una aplicación web para la escucha e intervención de paisajes sonoros y música experimental. Se tienen dos archivos sonoros diferentes. El primero, los paisajes sonoros de "
     (s "freesound.org") ". El segundo es una archivo curado de música y sonidos experimental, que tiene como repositorio a " (s "archive.org") "."]
    [:span "La invitación es para nutrir este segundo archivo. Se pueden enviar cualquier número de grabaciones."]
    "¿Cómo participar?"
    "1) Crea una pieza de música o un sonido aislado con la intención de que pueda combinarse con uno o varios tipos de paisajes sonoros (a modo de collage)."
    [:span "2) Sube la grabación a tu propia cuenta en " (s "archive.org") ", en la categoría de música."]
    [:span "3) Envía el link a de la grabación a " (s "radioalgoritmica@gmail.com") ". Si tienes preguntas puedes usar este mismo correo."]

    [:span "Por favor visita " (s "camposonico.net") " para darte una idea de la experiencia de escicha. Este proyecto no es ni jamás será desarrollado con fines de lucro."]]
   :en
   ["Experimental musicians and sound explorers: Donate sounds and music created with the aim of coexisting with other environments (natural, human, etc.)."

    "Camposónico (algorithmic radio) is a web app for listening and intervening soundscapes and experimental music. We have two different sound archives. The first one is the field recordings archive from freesound.org. The second is a curated archive of experimental music that takes its music from archive.org."

    [:span"All sound creators are invited to donate any number of tracks to the Camposónico radio music and sound archive " [:span {:class "link"} "(https://camposonico.net)."]]

    "How to participate:"
    "1) Create a piece of music or just a sound with the mindset that it will be played back along any sort of field recordings (in a collage sort of way) and should able blend with them."
    [:span "2) Upload it to your own account on " [:span {:class "link"} "archive.org"] " (in the music category)."]
    [:span "3) Send the link to "
     [:span {:class "link"} "radioalgoritmica@gmail.com"]
     " (feel free to send any questions as well.)"]

    [:span "Please visit " [:span {:class "link"} "camposonico.net "] "to get a feel of what the listening experience is like. This project is not and will never be for profit."]]})

(defn main [lang]
  [:div {:class "convo__container"}
   [:h1 {:class "convo__title"} (titles lang)]
   (map
    (fn [par]
      [:p {:key par :class "convo__p"} par])
    (paragraphs lang))])
