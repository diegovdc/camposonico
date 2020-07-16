# Camposónico: Algorithmic radio

## What is Camposónico?
Camposónico (algorithmic radio) is an application for listening to and intervening soundscapes and experimental music, making possible an interaction with sound that ranges from simple autoplay to creative experimentation through the use of code.

Its objectives are the following:

1. Make browsing the soundscape archive of Freesound.org easy.
1. Invite a listening game where different spaces and times can coexist and converge in the same event.
1. Encourage the creation and development of an archive of experimental music expressly created to be listened to along with the soundscapes.
1. Invite the listener to play with both archives and explore them creatively using the code.

## How to use Camposónico?
### Method 1: Graphical interface
The easiest way to use it is by entering a term in the search bar in the upper right corner of the window. It is possible to enter any number of queries, and control how many recordings will be played at the same time at any given moment. The choice of audios is random.

When a search results are loaded, they will appear below the search bar. A numeric "input" or "control" and a number plus its right, something like `1/15` will also appear. The input allows you to control the number of layers of the selected landscape type, zero silences all the layers, and any number greater than `0` will determine that as many layers are activated as specified *. The other number indicates the number of different layers that are available. If there are more layers available, these will be downloaded gradually.

Each time a layer is activated, a colored square will appear. Clicking on this box will display the information for that layer (author, description, link, etc.).

* Note: The number controlling the number of layers may not correspond to the number of layers currently playing (and displayed in the interface with the colored squares). This is due to certain limitations on the [Freesound.org](https://freesound.org) service. This usually happens when many layers are turned on or off quickly.

### Method 2: Code interface
Camposónico also offers a code interface, in the JavaScript language, that allows a more creative, effective and active interaction with the sound materials. This interface is still under development, so anyone with an interest can collaborate or suggest functionalities in the repository that houses the application code.

## API

#### `load`: Loads a type of soundscape. The search on freesound.org is done by "tags".

```js
load("veracruz")
```

#### `play`: Add a layer of this soundscape

```js
play("veracruz")
```
`play` can have the following options

```js
play("veracruz", {
  index: 1, // index of the sound to be played, i.e. second sound in the `veracruz` list. Defaults to random. Use the `getSounds()` function to see what audios are on any given list.
  start: 60, // starting point of the audio in seconds. Defaults to 0.
  dur: 5, // duration of the audio. Defaults to the full lenght of the audio.
  vol: 1, // Sets the volume of the audio. Range is `0 - 1`. Defaults to 0.75
})
```
#### `getSounds`: Returns an object with all the loaded sounds grouped by query

Usually you'd want to call this function in the browser's *console*.

```js
getSounds()
```    

#### `stop`: Remove a layer from this soundscape


```js
stop("veracruz")
```

#### `showInfo`: Shows information about the sounds and music currently playing. The numerical parameter determines the speed with which one goes from one track to the next (in milliseconds)

```js
showInfo(7000)
```

#### `setInfoPosition`: Determines the position on the screen where the text will appear

```js
setInfoPosition("center") // other options "left", "right" "top" "bottom"
```

#### `randNth`: Choose an element from an array randomly every 5 seconds

```js
landscapes = ["ocean", "bright", "sand", "forest", "faraway", "nightingale"]

landscapes.forEach(p => load (p))

landscapes.forEach(p => {play (p); play (p)}) // initialize two landscapes of each type

positions = ["center", "down", "up", "left", "right"]


// Every 20 seconds select a landscape at random to activate
// and one to disable from the `landscapes` list,
// also, change the position of the information
interval = setInterval(
    () => {
        play(randNth(landscapes))
        stop(randNth (landscapes))
        setInfoPosition(randNth (positions))
    },
    20000
)

clearInterval (interval) // stop changes
```

#### `uploadSelections`: Uploads and loads a `json` file with the audio selections.

The file must be in `json` format, and should have the following spec, where `name` is the name of the list, equivalent to the query as it appears on the UI after a search, and `data` is the list of source audios.

```json
{
  "name": "String",
  "data": [{
    "title": "String",
    "description": "String",
    "duration": "Number, duration in seconds (optional)",
    "url": "String: location where was the file found, this will be displayed",
    "mp3": "URL of the mp3 file, this will not be displayed",
    "other fields": "Any other field is possible, but nothing will be done with it, however you will be able to see this if you log the sounds to the console with getSounds()... it is useful for notes."
    }]
}
```

#### `initHydra`: Activates the hydra-synth visual synthesizer. More information

```js
initHydra ()
osc (1,1,1) .out ()
```

## License
Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
