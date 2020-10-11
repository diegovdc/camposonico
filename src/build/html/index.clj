(ns build.html.index
  (:require [clojure.data.json :as json]))

(defn template [version main-js]
  (let [opts (json/write-str {"version" version})]
    [:html
     [:head
      [:title "Camposónico: Radio algorítmica"]
      [:meta {:charset "UTF-8"}]
      [:meta
       {:content "width=device-width, initial-scale=1", :name "viewport"}]
      [:link
       {:type "text/css", :rel "stylesheet", :href "/css/mazorca.css"}]
      [:link
       {:type "text/css",
        :rel "stylesheet",
        :href
        "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.53.2/codemirror.min.css"}]
      [:link
       {:type "text/css",
        :rel "stylesheet",
        :href
        "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.53.2/theme/oceanic-next.min.css"}]
      [:link
       {:type "text/css",
        :rel "stylesheet",
        :href
        "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.50.2/addon/display/fullscreen.min.css"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-57x57.png",
        :sizes "57x57",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-114x114.png",
        :sizes "114x114",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-72x72.png",
        :sizes "72x72",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-144x144.png",
        :sizes "144x144",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-60x60.png",
        :sizes "60x60",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-120x120.png",
        :sizes "120x120",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-76x76.png",
        :sizes "76x76",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:href "/images/favicon/apple-touch-icon-152x152.png",
        :sizes "152x152",
        :rel "apple-touch-icon-precomposed"}]
      [:link
       {:sizes "196x196",
        :href "/images/favicon/favicon-196x196.png",
        :type "image/png",
        :rel "icon"}]
      [:link
       {:sizes "96x96",
        :href "/images/favicon/favicon-96x96.png",
        :type "image/png",
        :rel "icon"}]
      [:link
       {:sizes "32x32",
        :href "/images/favicon/favicon-32x32.png",
        :type "image/png",
        :rel "icon"}]
      [:link
       {:sizes "16x16",
        :href "/images/favicon/favicon-16x16.png",
        :type "image/png",
        :rel "icon"}]
      [:link
       {:sizes "128x128",
        :href "/images/favicon/favicon-128.png",
        :type "image/png",
        :rel "icon"}]
      [:meta {:content "Camposónico", :name "application-name"}]
      [:meta {:content "#FFFFFF", :name "msapplication-TileColor"}]
      [:meta
       {:content "/images/favicon/mstile-144x144.png",
        :name "msapplication-TileImage"}]
      [:meta
       {:content "/images/favicon/mstile-70x70.png",
        :name "msapplication-square70x70logo"}]
      [:meta
       {:content "/images/favicon/mstile-150x150.png",
        :name "msapplication-square150x150logo"}]
      [:meta
       {:content "/images/favicon/mstile-310x150.png",
        :name "msapplication-wide310x150logo"}]
      [:meta
       {:content "/images/favicon/mstile-310x310.png",
        :name "msapplication-square310x310logo"}]
      [:meta {:content "summary_large_image", :name "twitter:card"}]
      [:meta {:content "@diegovideco", :name "twitter:site"}]
      [:meta {:content "@diegovideco", :name "twitter:creator"}]
      [:meta {:content "Camposónico", :name "twitter:title"}]
      [:meta
       {:content
        "Radio algorítmica con paisajes sonoros y música experimental",
        :name "twitter:description"}]
      [:meta
       {:content "https://www.camposonico.net/images/site-image.jpg",
        :name "twitter:image"}]
      [:meta {:content "https://www.camposonico.net", :property "og:url"}]
      [:meta {:content "article", :property "og:type"}]
      [:meta {:content "Camposónico", :property "og:title"}]
      [:meta
       {:content
        "Radio algorítmica con paisajes sonoros y música experimental",
        :property "og:description"}]
      [:meta
       {:content "https://www.camposonico.net/images/site-image.jpg",
        :property "og:image"}]
      [:meta {:content "656", :property "og:image:height"}]
      [:meta {:content "1312", :property "og:image:width"}]]
     [:body
      [:div#app]
      [:script {:type "text/javascript", :src (str "/js/compiled/" main-js)}]
      [:script
       {:type "text/javascript", :src "/js/external-libs/index.js"}]
      [:script "algoradio.core.init("opts");"]
      [:script {:src "https://kit.fontawesome.com/d1ccef923e.js"
                :crossorigin "anonymous"}]]])
  )
