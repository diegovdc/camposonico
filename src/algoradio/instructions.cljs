(ns algoradio.instructions)

(def intro
  "// /`  ,_       (`  ,_
// \\,(|||||)()  _)()||()|`()
//        |
// |)   |.     /||      ,|-,_ . _
// |\\(|(||()  /-||(|()|`||_||||(_(|
//                _|

// Instrucciones:
// Método 1 (interfáz gráfica):
// A) En la caja de búsqueda (arriba a la derecha)
//    escribe el nombre del paisaje sonoro que quisieras escuchar.
//    Las búsquedas en inglés suelen arrojar más resultados :(
//    Cuando la busqueda haya terminado, agrega o remueve capas a placer
// B) Al dar click en \"Agregar música\":
//    Se activará la reproducción del archivo de música y sonido experimental


// Método 2 (interfáz de código):
// A) (Para aprender) ejecuta en órden las siguientes lineas de código

// ctrl+enter sobre la linea para ejecutarla
load(\"ocean\")

play(\"ocean\")

load(\"song bird\")

//para evaluar varias lineas al mismo tiempo
//no las separes
play(\"ocean\")
play(\"song bird\")

stop(\"ocean\")


// cambiar la información de los sonidos cada 3 segundos
showInfo(3000)

// mandar la información al fondo
infoAsBackground(true)


//Para cambiar la posición del editor
setInfoPosition(\"completa\") //opciones: \"completa\", \"izquierda\", \"derecha\" \"abajo\" \"arriba\"

// quitar la mayoria de las instrucciones
clearComments() // y sigue explorando sonidos...
")
