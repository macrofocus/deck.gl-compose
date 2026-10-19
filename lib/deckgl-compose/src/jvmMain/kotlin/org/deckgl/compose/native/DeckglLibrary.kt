package org.deckgl.compose.native

import java.io.File
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_DOUBLE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Downcalls into deck.gl-native's C API (`crates/deck-gl-ffi/include/deckgl.h`) through the
 * Foreign Function & Memory API.
 *
 * The library is looked up, in order, at the path in the `deckgl.library.path` system property
 * or `DECKGL_LIBRARY_PATH` environment variable, as a classpath resource packaged for this
 * platform, and on the system library path.
 */
internal object DeckglLibrary {
  private val linker = Linker.nativeLinker()
  private val lookup: SymbolLookup = loadLibrary()

  private fun function(name: String, descriptor: FunctionDescriptor): MethodHandle =
    linker.downcallHandle(
      lookup.find(name).orElseThrow { UnsatisfiedLinkError("libdeckgl has no symbol $name") },
      descriptor,
    )

  val headlessCreate = function("deckgl_headless_create", FunctionDescriptor.of(ADDRESS))
  val destroy = function("deckgl_destroy", FunctionDescriptor.ofVoid(ADDRESS))
  val setLayersJson = function("deckgl_set_layers_json", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS))
  val isLoading = function("deckgl_is_loading", FunctionDescriptor.of(JAVA_INT, ADDRESS))
  val setCamera = function("deckgl_set_camera", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS))
  val snapshot = function("deckgl_snapshot", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS))
  val pick = function("deckgl_pick", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_DOUBLE, JAVA_DOUBLE, ADDRESS))
  val pointerMove = function("deckgl_pointer_move", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_DOUBLE, JAVA_DOUBLE))
  val pointerLeave = function("deckgl_pointer_leave", FunctionDescriptor.of(JAVA_INT, ADDRESS))
  val lastError = function("deckgl_last_error", FunctionDescriptor.of(ADDRESS, ADDRESS))
  val setLogLevel = function("deckgl_set_log_level", FunctionDescriptor.ofVoid(JAVA_INT))

  /** `DeckglCamera`, with C alignment. */
  object Camera {
    const val LONGITUDE = 0L
    const val LATITUDE = 8L
    const val ZOOM = 16L
    const val BEARING = 24L
    const val PITCH = 32L
    const val FOV_DEGREES = 40L
    const val NEAR_Z_PIXELS = 48L
    const val FAR_Z_PIXELS = 56L
    const val WIDTH = 64L
    const val HEIGHT = 68L
    const val PIXEL_RATIO = 72L
    const val PADDING_LEFT = 80L
    const val ROLL_DEGREES = 112L
    const val CENTER_ELEVATION = 120L
    const val HAS_PROJECTION_MATRIX = 128L
    const val SIZE = 264L
  }

  /** `DeckglPickingInfo`, with C alignment. */
  object PickingInfo {
    const val PICKED = 0L
    const val INDEX = 4L
    const val X = 8L
    const val Y = 16L
    const val LONGITUDE = 24L
    const val LATITUDE = 32L
    const val LAYER_ID = 40L
    const val SIZE = 48L
  }


  private fun loadLibrary(): SymbolLookup {
    val arena = Arena.global()
    val explicit = System.getProperty("deckgl.library.path") ?: System.getenv("DECKGL_LIBRARY_PATH")
    if (explicit != null) return SymbolLookup.libraryLookup(File(explicit).toPath(), arena)

    val libraryName = System.mapLibraryName("deckgl")
    val resource = "/org/deckgl/compose/natives/$platform/$libraryName"
    DeckglLibrary::class.java.getResourceAsStream(resource)?.use { input ->
      val directory = Files.createTempDirectory("deckgl-compose")
      val file = directory.resolve(libraryName)
      Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING)
      file.toFile().deleteOnExit()
      directory.toFile().deleteOnExit()
      return SymbolLookup.libraryLookup(file, arena)
    }

    return try {
      SymbolLookup.libraryLookup(libraryName, arena)
    } catch (e: IllegalArgumentException) {
      throw UnsatisfiedLinkError(
        "deck.gl-native ($libraryName) was not found for $platform: it is neither packaged as $resource " +
          "nor on the library path. Set -Ddeckgl.library.path=/path/to/$libraryName.",
      ).initCause(e)
    }
  }

  // A function rather than a property: it is used while `lookup` is initialized.
  private val platform: String get() = run {
    val os = System.getProperty("os.name").lowercase()
    val osName = when {
      "mac" in os -> "macos"
      "win" in os -> "windows"
      else -> "linux"
    }
    val arch = when (val a = System.getProperty("os.arch")) {
      "aarch64", "arm64" -> "aarch64"
      "x86_64", "amd64" -> "x86_64"
      else -> a
    }
    "$osName-$arch"
  }
}
