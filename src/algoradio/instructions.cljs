(ns algoradio.instructions)

(defn intro [version]
  (str "//  /`  ,_      _ ,._. _
//  \\,(|||||)()_\\()|||(_()
//         |
//  |)   |.     /||      ,|-,_ . _
//  |\\(|(||()  /-||(|()|`||_||||(_(|
//               _|
// versión: " version "

// ¿Qué es Camposónico?
// Es una \"Radio algorítmica\" con dos objetivos:
// 1. Facilitar la búsqueda y escucha de paisajes sonoros
// 2. Posibilitar la creación de  nuevas experiencias musicales
//     mediante la superposición y manipulación de paisajes sonoros
//     y grabaciones de música experimental.

// Instrucciones:
// Método 1 (interfaz gráfica):
// A) En la caja de búsqueda (arriba a la derecha)
//    escribe el nombre del paisaje sonoro que quisieras escuchar.
//    (Las búsquedas en inglés suelen arrojar más resultados)

// B) Al dar click en \"Agregar música\":
//    Se activará la reproducción del archivo de música y sonido experimental


// Método 2 (interfaz de código -JavaScript-):
// A) (Para aprender) ejecuta en órden las siguientes lineas de código

// ctrl+enter sobre la linea para ejecutarla
load(\"ocean\")

//espera unos segundos para que aparezca \"ocean\" del lado derecho
play(\"ocean\") // dale ctrl+enter varias veces :)

load(\"song bird\")

//para evaluar varias lineas al mismo tiempo
//no las separes
play(\"ocean\")
play(\"song bird\")

stop(\"ocean\")


// cambiar la información de los sonidos cada 3 segundos
showInfo(3000)

// mandar la información al fondo y continuar usando la interfaz
infoAsBackground(true)


//Para cambiar la posición de la información en el editor
setInfoPosition(\"centro\") //opciones:\"centro\", \"izquierda\", \"derecha\" \"abajo\" \"arriba\"


clearComments() // y sigue explorando sonidos...













"))
