import androidx.compose.ui.window.WindowState
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import core.database.OverlayType
import core.database.RFDao
import core.database.Schema
import core.interactor.Interactor
import kotlinx.coroutines.*
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import java.awt.Color
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import kotlin.math.abs

object OverlayInteractor : Interactor() {

  private val GAME_WINDOW_COLOR_MODIFIER_BASE = Color(237, 233, 214)
  private val GAME_WINDOW_COLOR_MODIFIER = Color(137, 137, 130)
  private val GAME_WINDOW_COLOR = Color(240, 240, 226)
  const val COLOR_DETECTION_THRESHOLD = 13.0 // 13%
  const val MAX_VARIANCE = 7

  /*
   * Tries to programmatically manage overlay windows so that they can be hidden when the game is tabbed-out.
   */
  override suspend fun interact() {
    // check to see if the player is tabbed-out of the game by window title
    if (AppState.config.tabbedDetectionEnabled) {
      val gameForegrounded = getActiveWindowTitle().contains("ArcheRage")
      AppState.isEverythingVisible.value = gameForegrounded
    } else {
      AppState.isEverythingVisible.value = true
    }

    // check to see if it looks like an in-game window is being covered by an overlay
    val supportedWindows = listOf(
      AppState.windowStates.combatState,
      AppState.windowStates.trackerState
    )

    if (AppState.config.colorAndTextDetectionEnabled) {
      val ss = takeScreenshot()
      supportedWindows.filterNotNull().forEach { state ->
        when (state) {
          AppState.windowStates.combatState -> AppState.isCombatObstructing.value = shouldHideWindow(
            alreadyObstructing = AppState.isCombatObstructing.value,
            windowState = state,
            image = ss
          )

          AppState.windowStates.trackerState -> AppState.isTrackerObstructing.value = shouldHideWindow(
            alreadyObstructing = AppState.isTrackerObstructing.value,
            windowState = state,
            image = ss
          )
        }
      }
    } else {
      supportedWindows.onEach {
        when (it) {
          AppState.windowStates.combatState -> AppState.isCombatObstructing.value = false
          AppState.windowStates.trackerState -> AppState.isTrackerObstructing.value = false
        }
      }
    }
  }

  private suspend fun shouldHideWindow(
    alreadyObstructing: Boolean,
    windowState: Schema.RFWindowState,
    image: BufferedImage
  ): Boolean {
    val overlayRegion = Rectangle(
      windowState.lastPositionXDp.toInt(),
      windowState.lastPositionYDp.toInt(),
      windowState.lastWidthDp.toInt(),
      windowState.lastHeightDp.toInt()
    )

    val isCovered = try {
      isRegionObstructed(image, overlayRegion)
    } catch (E: Exception) {
      println("Error checking region obstruction because: ${E.message}")
      return false // fail open
    }

    val regionImage = try {
      image.getSubimage(overlayRegion.x, overlayRegion.y, overlayRegion.width, overlayRegion.height)
    } catch (E: Exception) {
      println("Error getting subimage because: ${E.message}")
      return true // fail open
    }

    val isTextPresent = isTextPresent(regionImage)

    return if (alreadyObstructing) isCovered || isTextPresent else isCovered
  }

  /*
   * Tries to determine if a game window is being covered by an overlay window in the specified region.
   * It's looking for that classes inventory window background color. (genius eh? ~_~)
   */
  private suspend fun isRegionObstructed(image: BufferedImage, region: Rectangle): Boolean {
    return isColorAmountAboveThreshold(
      image = image,
      region = region,
      targetColors = listOf(GAME_WINDOW_COLOR, GAME_WINDOW_COLOR_MODIFIER, GAME_WINDOW_COLOR_MODIFIER_BASE),
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
  private fun isColorAmountAboveThreshold(
    image: BufferedImage,
    region: Rectangle,
    targetColors: List<Color>,
    threshold: Double
  ): Boolean {
    var matchingPixels = 0L

    for (x in region.x until region.x + region.width) {
      for (y in region.y until region.y + region.height) {
        val rgb = image.getRGB(x, y)
        val color = Color(rgb, true)

        targetColors.forEach {
          if (abs(color.red - it.red) <= MAX_VARIANCE &&
            abs(color.green - it.green) <= MAX_VARIANCE &&
            abs(color.blue - it.blue) <= MAX_VARIANCE
          ) {
            matchingPixels++
          }
        }
      }
    }

    val totalPixels = region.width * region.height
    val percentage = matchingPixels.toDouble() / totalPixels * 100

    return percentage > threshold
  }

  private fun isTextPresent(image: BufferedImage): Boolean {
    AppState.tessTempDirectory?.let {
      try {
        Tesseract().let { tesseract ->
          tesseract.setDatapath(it.toString())
          tesseract.setLanguage("eng")
          tesseract.setTessVariable("user_defined_dpi", "96")
          tesseract.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
          val result: String = tesseract.doOCR(image)
          val words = filterWords(result)
          return words.size > 2
        }
      } catch (e: TesseractException) {
        println("Tesseract failed: ${e.message}")
        return false
      }
    } ?: return false // no tessdata directory
  }

  fun filterWords(input: String): List<String> {
    val regex = Regex("\\b[A-Z][a-zA-Z]{4,}\\b")
    return regex.findAll(input).map { it.value }.toList()
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
      println("Failed to get a window handle.. Failing Open.")
      return "ArcheRage"
    }
  }

  interface User32 : StdCallLibrary {
    fun GetForegroundWindow(): HWND
    fun GetWindowTextA(hWnd: HWND, lpString: Pointer, nMaxCount: Int): Int

    companion object {
      val INSTANCE: User32 = Native.load("user32", User32::class.java)
    }
  }

  /*
   * Updates the window state for a specific overlay type in the app state. The window states later gets saved
   * to the disk for persistence.
   */
  fun updateWindowStateFor(overlayType: OverlayType, windowState: WindowState) {
    val newState = Schema.RFWindowState(
      type = when (overlayType) {
        OverlayType.COMBAT -> OverlayType.COMBAT.ordinal
        OverlayType.SETTINGS -> OverlayType.SETTINGS.ordinal
        OverlayType.TRACKER -> OverlayType.TRACKER.ordinal
        OverlayType.ABOUT -> OverlayType.ABOUT.ordinal
        OverlayType.AGGRO -> OverlayType.AGGRO.ordinal
        OverlayType.FILTERS -> OverlayType.FILTERS.ordinal
        else -> return
      },
      lastPositionXDp = windowState.position.x.value,
      lastPositionYDp = windowState.position.y.value,
      lastWidthDp = windowState.size.width.value,
      lastHeightDp = windowState.size.height.value
    )
    when (overlayType) {
      OverlayType.COMBAT -> AppState.windowStates.combatState = newState
      OverlayType.SETTINGS -> AppState.windowStates.settingsState = newState
      OverlayType.TRACKER -> AppState.windowStates.trackerState = newState
      OverlayType.ABOUT -> AppState.windowStates.aboutState = newState
      OverlayType.AGGRO -> AppState.windowStates.aggroState = newState
      OverlayType.FILTERS -> AppState.windowStates.filterState = newState
      else -> return
    }
    CoroutineScope(Dispatchers.Default).launch {
      RFDao.saveWindowStates(AppState.windowStates)
    }
  }
}