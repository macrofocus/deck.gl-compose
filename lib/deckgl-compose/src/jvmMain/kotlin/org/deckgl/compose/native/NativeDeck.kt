package org.deckgl.compose.native

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_DOUBLE
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import java.lang.foreign.ValueLayout.JAVA_INT
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.deckgl.compose.MapViewState
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/** The camera of one frame, in physical pixels. */
internal data class NativeCamera(val viewState: MapViewState, val width: Int, val height: Int, val density: Float)

internal data class NativePick(
  val layerId: String?,
  val index: Int,
  val longitude: Double,
  val latitude: Double,
)

/**
 * A deck.gl-native deck on its own headless GPU device. Not thread safe: only use it on
 * [dispatcher].
 *
 * Frames are rendered at the physical pixel size with a pixel ratio of 1 and the zoom raised
 * by log2(density), which shows the same area as the logical size at the device's density.
 * (deck.gl-native's `deckgl_snapshot` does not support pixel ratios other than 1 yet.)
 */
internal class NativeDeck private constructor(private val handle: MemorySegment) : AutoCloseable {
  private val arena = Arena.ofShared()
  private val camera = arena.allocate(DeckglLibrary.Camera.SIZE, 8)
  private val pickingInfo = arena.allocate(DeckglLibrary.PickingInfo.SIZE, 8)
  private var pixelArena: Arena? = null
  private var pixels: MemorySegment = MemorySegment.NULL
  private var closed = false

  /** Replaces the layers with a deck.gl JSON description. Returns an error message on failure. */
  fun setLayers(json: String): String? = Arena.ofConfined().use { temp ->
    val result = DeckglLibrary.setLayersJson.invoke(handle, temp.allocateFrom(json), MemorySegment.NULL) as Int
    if (result == 0) null else lastError()
  }

  /** Whether URLs or tiles are still loading, so more frames are needed. */
  fun isLoading(): Boolean = DeckglLibrary.isLoading.invoke(handle) as Int != 0

  fun setCamera(frame: NativeCamera) {
    val c = DeckglLibrary.Camera
    val v = frame.viewState
    camera.fill(0)
    camera.set(JAVA_DOUBLE, c.LONGITUDE, v.longitude)
    camera.set(JAVA_DOUBLE, c.LATITUDE, v.latitude)
    camera.set(JAVA_DOUBLE, c.ZOOM, v.zoom + kotlin.math.log2(frame.density.toDouble()))
    camera.set(JAVA_DOUBLE, c.BEARING, v.bearing)
    camera.set(JAVA_DOUBLE, c.PITCH, v.pitch)
    camera.set(JAVA_INT, c.WIDTH, frame.width)
    camera.set(JAVA_INT, c.HEIGHT, frame.height)
    camera.set(JAVA_FLOAT, c.PIXEL_RATIO, 1f)
    DeckglLibrary.setCamera.invoke(handle, camera)
  }

  /** Renders a frame with a transparent background. */
  fun render(frame: NativeCamera): Result<ImageBitmap> {
    setCamera(frame)
    val byteSize = frame.width.toLong() * frame.height * 4
    if (pixels.byteSize() != byteSize) {
      pixelArena?.close()
      pixelArena = Arena.ofShared().also { pixels = it.allocate(byteSize) }
    }
    val result = DeckglLibrary.snapshot.invoke(handle, frame.width, frame.height, pixels) as Int
    if (result != 0) return Result.failure(IllegalStateException(lastError()))
    val info = ImageInfo(frame.width, frame.height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    val image = Image.makeRaster(info, pixels.toArray(JAVA_BYTE), frame.width * 4)
    return Result.success(image.toComposeImageBitmap())
  }

  /** Picks at physical pixel coordinates with the camera of the last frame. */
  fun pick(x: Double, y: Double): NativePick? {
    val p = DeckglLibrary.PickingInfo
    val result = DeckglLibrary.pick.invoke(handle, x, y, pickingInfo) as Int
    if (result != 0) return null
    val picked = pickingInfo.get(JAVA_INT, p.PICKED) != 0
    val layerId = pickingInfo.get(ADDRESS, p.LAYER_ID)
    return NativePick(
      layerId = if (picked && layerId != MemorySegment.NULL) layerId.reinterpret(Long.MAX_VALUE).getString(0) else null,
      index = if (picked) pickingInfo.get(JAVA_INT, p.INDEX) else -1,
      longitude = pickingInfo.get(JAVA_DOUBLE, p.LONGITUDE),
      latitude = pickingInfo.get(JAVA_DOUBLE, p.LATITUDE),
    )
  }

  /** Moves the pointer for hover highlighting, at physical pixel coordinates. */
  fun pointerMove(x: Double, y: Double) {
    DeckglLibrary.pointerMove.invoke(handle, x, y)
  }

  fun pointerLeave() {
    DeckglLibrary.pointerLeave.invoke(handle)
  }

  private fun lastError(): String {
    val message = DeckglLibrary.lastError.invoke(handle) as MemorySegment
    return if (message == MemorySegment.NULL) "unknown error" else message.reinterpret(Long.MAX_VALUE).getString(0)
  }

  override fun close() {
    if (closed) return
    closed = true
    DeckglLibrary.destroy.invoke(handle)
    pixelArena?.close()
    arena.close()
  }

  companion object {
    /** The thread all native decks are used on. */
    val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, "deck.gl-native").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    /** Creates a deck on a headless GPU device. Call on [dispatcher]. */
    fun create(): NativeDeck {
      val handle = DeckglLibrary.headlessCreate.invoke() as MemorySegment
      check(handle != MemorySegment.NULL) { "deck.gl-native found no GPU adapter" }
      return NativeDeck(handle)
    }
  }
}
