# deck.gl Compose

[deck.gl](https://deck.gl) layers for [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/),
in the spirit of [maplibre-compose](https://github.com/maplibre/maplibre-compose).

| Target          | Renderer                                                                                                                         |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------|
| JS (browser)    | deck.gl 9.4 (WebGL2)                                                                                                             |
| JVM (desktop)   | [deck.gl-native](https://github.com/birkskyum/deck.gl-native) (Rust/wgpu: Metal, Vulkan, DirectX 12), through the Java FFM API |

```kotlin
val state = rememberDeckState(MapViewState(longitude = 8.54, latitude = 47.37, zoom = 11.0))

DeckGL(Modifier.fillMaxSize(), state) {
  RasterTileLayer(id = "basemap", data = "https://tile.openstreetmap.org/{z}/{x}/{y}.png")

  ScatterplotLayer(
    id = "stations",
    data = stations,
    getPosition = accessor { it.position },
    getRadius = accessor { it.passengers / 10.0 },
    getFillColor = constant(Color.Red),
    radiusMinPixels = 3.0,
    autoHighlight = true,
    onClick = { station, _ -> selected = station },
  )
}
```

## Using it

- `DeckGL(modifier, state, controller, onClick, onHover) { layers }` shows a `MapView`. Layers are composables declared in its content and drawn in declaration order. They update when their parameters change, and `if`/`when` add and remove them.
- `rememberDeckState(initialViewState)` holds the camera (`MapViewState`: longitude, latitude, zoom, pitch, bearing). Gestures update it, and assigning `state.viewState` moves the camera.
- `ControllerOptions` enables or disables drag-pan, drag-rotate (right button or a modifier key on desktop), scroll zoom and double-click zoom, and sets the zoom and pitch limits.
- **Accessors** come in two forms:
  - `constant(value)`: one value for all items.
  - `accessor { item -> value }`: computed per item, in Kotlin. Accessor values can be `Position`, `Color`, numbers, strings, and lists of these.
- **Picking:** a layer's `onClick`/`onHover` receive the picked item of its `data` and a `PickInfo` (layer id, index, coordinate, position in dp). Clicks no layer handles go to `DeckGL`'s own `onClick`.
- **Typed layers:**
  - `ScatterplotLayer`, `LineLayer`, `ArcLayer`, `PathLayer`, `PolygonLayer`, `ColumnLayer`, `TextLayer`
  - `HexagonLayer`, `HeatmapLayer`
  - `GeoJsonLayer`, `BitmapLayer`, `RasterTileLayer`
- **Escape hatches** for anything else:
  - `Layer(type, id, data, props, accessors)` declares any layer type with Kotlin data.
  - `JsonLayer(json)` takes a layer in deck.gl's JSON format (`@deck.gl/json`/pydeck, `"@@=expression"` accessors). URLs work as `data`.

The deck has a transparent background. Put Compose UI on top of it by placing it after `DeckGL` in a `Box`, as the demo app does with its control panel.

## How it works

Layer composables emit nodes into a separate composition, using a custom `Applier` like maplibre-compose's style nodes. On every change the nodes are turned into layer descriptions in deck.gl's JSON format (`{"@@type": "ScatterplotLayer", "getPosition": "@@=c0", "data": [{"c0": [8.5, 47.4]}], ...}`). Per-item accessors are evaluated once into columns of the rows. Both deck.gls read the same descriptions:

- **JS** (`src/jsMain`)
  - A deck.gl `Deck` lives in an `HtmlElementView`. Layers are converted with `@deck.gl/json`'s `JSONConverter`, one at a time and cached per description, so deck.gl only updates layers that changed.
  - Compose for web places HTML elements *above* its canvas. To let Compose UI draw over the map, the element is moved below the canvas, and `DeckGL` punches a transparent hole in the canvas (`BlendMode.Clear`).
  - The pointer events Compose receives over the deck are re-dispatched to deck.gl's canvas. deck.gl's own controller, picking and `autoHighlight` therefore work unchanged.
  - Consequence: Compose content drawn *behind* `DeckGL`, such as a `background` on a parent, is cut out where the deck is.
- **JVM** (`src/jvmMain`)
  - `libdeckgl`, deck.gl-native's C API (`crates/deck-gl-ffi/include/deckgl.h`), is called with the FFM API (`java.lang.foreign`), so no JNI glue is needed.
  - Each deck runs on its own headless GPU device, on a dedicated thread. Frames are rendered offscreen when the layers, camera or size change, and drawn by Compose as an `ImageBitmap`. Frames keep coming while tiles or URLs load.
  - Gestures are handled in Kotlin (`MapController`), because the C API has no controller. Picking and hover use `deckgl_pick`/`deckgl_pointer_move`.

## Building

Requirements:
- JDK 25: the FFM API needs Java 22+, and the build uses a 25 toolchain.
- A Rust toolchain for the desktop target: `brew install rustup && rustup default stable`, or <https://rustup.rs>.
- Clone with submodules: `git submodule update --init`.

```sh
./gradlew :demo-app:run                              # desktop demo
./gradlew :demo-app:jsBrowserDevelopmentRun          # browser demo (webpack dev server)
./gradlew :lib:deckgl-compose:jvmTest                # native rendering, picking and gesture tests
```

`:lib:deckgl-compose` builds deck.gl-native with cargo from `native/deck.gl-native` (`cargoBuildDeckgl`). Cargo is found on the `PATH`, in `~/.cargo/bin`, or in Homebrew's keg-only `rustup`. The build packages the resulting `libdeckgl` for the host platform into the JVM jar, under `org/deckgl/compose/natives/<os>-<arch>/`. At runtime the library is loaded from:
1. `-Ddeckgl.library.path=/path/to/libdeckgl.dylib` or `DECKGL_LIBRARY_PATH`
2. the packaged resource
3. the system library path

Pass `-Pdeckgl.skipNativeBuild=true` to build without Rust. Applications must run with `--enable-native-access=ALL-UNNAMED`, which the demo sets.

## Limitations

- **Map view only.** deck.gl-native's C API only takes a map camera; multiple views, globe, orbit and orthographic views are not exposed yet.
- **HiDPI on desktop.** deck.gl-native's `deckgl_snapshot` copies only `width × height` pixels and does not handle a pixel ratio other than 1. Frames are rendered at the physical size with the zoom raised by log2(density), which shows exactly the same area. Sizes in *pixels* (`radiusMinPixels`, line widths in pixels, text sizes) therefore appear smaller on HiDPI screens until this is fixed upstream (`crates/deck-gl-ffi/src/lib.rs`, `deckgl_snapshot`).
- **Desktop frames are read back from the GPU.** deck.gl-native has no shared-texture path outside of Metal host renderers. This is fine for interactive use, but slower than WebGL in the browser at large window sizes.
- **Platform differences.**
  - Layer types and props must exist in both deck.gl and deck.gl-native; deck.gl-native warns on stderr about props it skips. See deck.gl-native's [docs/json.md](https://github.com/birkskyum/deck.gl-native/blob/main/docs/json.md) for its supported layers.
  - Keyboard navigation is only available in the browser.
- **Native builds are host-only** for now: the jar contains the library of the platform it was built on.
