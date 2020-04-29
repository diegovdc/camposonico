(ns algoradio.instructions)

(def intro
  "//  /`  ,_      _ ,._. _
//  \\,(|||||)()_\\()|||(_()
//         |
//  |)   |.     /||      ,|-,_ . _
//  |\\(|(||()  /-||(|()|`||_||||(_(|
//               _|

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
//
// B) Cuando la búsqueda haya terminado, se iniciará la reproducción de
//    una de las pistas encontradas y, aparecerá lo siguiente del lado derecho:
//    El nombre del resultado junto con dos números
//     1. El primero corresponde a los pistas que suenan actualmente
//        Éstas pistas se eligen al azar.
//        No hay límite a la cantidad de sonidos que puedes activar, y
//        y si quieres silenciarlos todos, usa 0.
//         (Tip: puedes usar las flechas del teclado)
//     2. El segundo corresponde al número de pistas disponibles.
//        Al principio se cargarán sólo 15.
//        Si hay más pistas disponibles (dependiendo de la búsqueda):
//         Entonces, al terminar cada una de estas pistas se cargaran otras 15
//         y así sucesivamente.
//
// C) Debajo aparecerá un pequeño cuadro por cada capa activa
//    Al dar click en este cuadro aparecerán lo datos que le corresponden.
//    Para esconder la información se puede dar click en los botones:
//    1. \"X\" - para cerrar
//    2. \"Mandar al fondo\" - para mantener la información visible
//         sin bloquear el resto de la interfaz
//
// D) Al dar click en \"Agregar música\":
//    Se activará la reproducción del archivo de música y sonido experimental


// Método 2 (interfaz de código -JavaScript-):
// A) (Para aprender) ejecuta en órden las siguientes lineas de código

// ctrl+enter sobre la linea para ejecutarla
load(\"ocean\")

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


//Para cambiar la posición del editor
setInfoPosition(\"centro\") //opciones:\"centro\", \"izquierda\", \"derecha\" \"abajo\" \"arriba\"


// se puede usar el sintetizador de visuales hydra, para más información https://github.com/ojack/hydra
initHydra()
osc(1,1,1).out()

clearComments() // y sigue explorando sonidos...













")
