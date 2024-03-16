import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.awt.Color
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage

object OverlayInteractor {

  private val scope = CoroutineScope(Dispatchers.IO)
  val mutex = Mutex()

  val GAME_WINDOW_COLOR = Color(137, 137, 130, 255)
  const val COLOR_DETECTION_THRESHOLD = 10.0 // 10%
  const val MAX_VARIANCE = 5

  /*
   * Tries to programmatically manage overlay windows so that they can be hidden when the game is tabbed-out.
   */
  fun start() {
    scope.launch {
      while (isActive) {

        // check if the game window is in the forgeground
        try {
          val gameForegrounded = getActiveWindowTitle().contains("ArcheRage")

          // check if the game window is being covered by an overlay window (if in foreground)
          val overlayRegion = Rectangle(768, 512, 256, 256)
          takeScreenshot().let { ss ->
            val isCovered = isRegionObstructed(ss, overlayRegion)
            AppState.isEverythingVisible.value = !isCovered && gameForegrounded
          }
        } catch (e: Exception) {
          println("Failed to get window title: ${e.message}")
        }
        delay(1000)  // delay for 1 second
      }
    }
  }

  fun stop() {
    scope.cancel()
  }

  /*
   * Tries to determine if a game window is being covered by an overlay window in the specified region.
   * It's looking for that classes inventory window background color. (genius eh? ~_~)
   */
  private suspend fun isRegionObstructed(image: BufferedImage, region: Rectangle): Boolean {
    return isColorAmountAboveThreshold(
      image = image,
      region = region,
      targetColor = GAME_WINDOW_COLOR,
      threshold = COLOR_DETECTION_THRESHOLD
    )
  }

  /*
   * Helper that captures a bitmap to use for color detection.
   */
  private fun takeScreenshot(): BufferedImage {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    return Robot().let { robot ->
      Rectangle(screenSize).let {
        robot.createScreenCapture(it)
      }
    }
  }

  /*
   * The actual logic for looking at the pixels inside a rectangular region of the screen
   * and determining if the ratio of matching pixels is above the threshold.
   */
  fun isColorAmountAboveThreshold(image: BufferedImage, region: Rectangle, targetColor: Color, threshold: Double): Boolean {
    var matchingPixels = 0

    for (x in region.x until region.x + region.width) {
      for (y in region.y until region.y + region.height) {
        val rgb = image.getRGB(x, y)
        val color = Color(rgb, true)

        if (Math.abs(color.red - targetColor.red) <= MAX_VARIANCE &&
          Math.abs(color.green - targetColor.green) <= MAX_VARIANCE &&
          Math.abs(color.blue - targetColor.blue) <= MAX_VARIANCE &&
          Math.abs(color.alpha - targetColor.alpha) <= MAX_VARIANCE) {
          matchingPixels++
        }
      }
    }

    val totalPixels = region.width * region.height
    val percentage = matchingPixels.toDouble() / totalPixels * 100

    println(percentage)

    return percentage > threshold
  }

  /*
   * See if ArcheRage is in the foreground for automatic hiding of overlay windows.
   */
  private fun getActiveWindowTitle(): String {
    val buffer = Memory(1024 * 2) // Allocate memory for the buffer
    val hwnd = User32.INSTANCE.GetForegroundWindow()
    val textLength = User32.INSTANCE.GetWindowTextA(hwnd, buffer, 1024)
    if (textLength > 0) {
      return buffer.getString(0)
    } else {
      throw Exception("Failed to get window title")
    }
  }

  interface User32 : StdCallLibrary {
    fun GetForegroundWindow(): HWND
    fun GetWindowTextA(hWnd: HWND, lpString: Pointer, nMaxCount: Int): Int

    companion object {
      val INSTANCE: User32 = Native.load("user32", User32::class.java)
    }
  }


}