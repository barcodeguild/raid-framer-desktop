import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import core.database.OverlayType
import core.database.RFDao
import core.helpers.getScreenSizeInDp
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import kotlinx.coroutines.*
import ui.OverlayWindow
import ui.overlay.*
import java.awt.Image
import java.awt.Toolkit
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() = application {
  val appState = AppState

  // wait for database and state to become available
  runBlocking {
    AppState.config = RFDao.loadConfig()
    AppState.windowStates = RFDao.loadWindowStates()
    appState.isEverythingResizable.value = AppState.config.overlayResizingEnabled
    appState.isAboutOverlayVisible.value = AppState.config.firstLaunch
    CombatInteractor.updateSelectedPath(AppState.config.defaultLogPath)
    CombatInteractor.shouldSearchEverywhere = AppState.config.searchEverywhere
    AppState.config.firstLaunch = false
    RFDao.saveConfig(AppState.config)
  }

  // copies the tesseract training data from resources to the disk
  try {
    Files.createTempDirectory("tessdata").let { tempDir ->
      AppState.tessTempDirectory = tempDir
      val trainedDataTempPath = tempDir.resolve("eng.traineddata")
      javaClass.getResourceAsStream("/eng.traineddata").use { inputStream ->
        if (inputStream != null) {
          Files.copy(inputStream, trainedDataTempPath, StandardCopyOption.REPLACE_EXISTING)
        }
      }
    }
  } catch (e: Exception) {
    println("Failed to create tessdata directory: ${e.message}")
  }

  // spawns the system tray menu
  val tray = spawnSystemTray()

  // spawns the overlay windows
  spawnDefaultWindows(tray)
  AppState.tray = tray

  // starts the interactors
  CombatInteractor.start()
  OverlayInteractor.start()
}

/*
 * Cleans up the application by stopping the interactors, closing all the windows, removing listeners, and
 * finally exiting the application.
 */
fun quit() {
  CombatInteractor.stop()
  OverlayInteractor.stop()
  AppState.tray?.shutdown()
  AppState.tray?.remove()
  exitProcess(0)
}

@Composable
fun spawnSystemTray(): SystemTray {
  val tray = SystemTray.get()
  val iconImage = painterResource("raidframer.ico").toAwtImage(
    density = Density(1f),
    layoutDirection = LayoutDirection.Ltr,
    size = Size(32f, 32f)
  )
  val titleMenuItem = MenuItem(".: Raid Framer :.")
  titleMenuItem.setImage(iconImage)
  tray.menu.add(titleMenuItem)
  tray.menu.add(MenuItem("Settings") {
    AppState.isSettingsOverlayVisible.value = true
  })
  tray.menu.add(MenuItem("About") {
    AppState.isAboutOverlayVisible.value = true
  })
  tray.setImage(iconImage)
  return tray
}

@Composable
private fun spawnDefaultWindows(tray: SystemTray) {

  var windowsOpen by remember { mutableStateOf(true) }
  var shouldReload by remember { mutableStateOf(false) }

  // Add the reset overlays to the tray menu
  var firstStarting by remember { mutableStateOf(true) }
  if (firstStarting) {
    tray.menu.add(MenuItem("Move All to Corner") {
      AppState.windowStates.combatState.apply {
        this?.lastPositionXDp = 10f
        this?.lastPositionYDp = 10f
      }
      AppState.windowStates.trackerState.apply {
        this?.lastPositionXDp = 10f
        this?.lastPositionYDp = 10f
      }
      AppState.windowStates.aggroState.apply {
        this?.lastPositionXDp = 10f
        this?.lastPositionYDp = 10f
      }
      AppState.windowStates.aboutState.apply {
        this?.lastPositionXDp = 10f
        this?.lastPositionYDp = 10f
      }
      AppState.windowStates.filterState.apply {
        this?.lastPositionXDp = 10f
        this?.lastPositionYDp = 10f
      }
      AppState.windowStates.settingsState.apply {
        this?.lastPositionXDp = 10f
        this?.lastPositionYDp = 10f
      }
      CoroutineScope(Dispatchers.IO).launch {
        RFDao.saveWindowStates(AppState.windowStates)
      }
      shouldReload = true
    })
    tray.menu.add(MenuItem("Reset Overlays") {
      AppState.windowStates.combatState = null
      AppState.windowStates.trackerState = null
      AppState.windowStates.aggroState = null
      AppState.windowStates.aboutState = null
      AppState.windowStates.filterState = null
      AppState.windowStates.settingsState = null
      CoroutineScope(Dispatchers.IO).launch {
        RFDao.saveWindowStates(AppState.windowStates)
      }
      shouldReload = true
    })
    tray.menu.add(MenuItem("Exit") {
      quit()
    })
    firstStarting = false
  }

  // should reload
  LaunchedEffect(shouldReload) {
    if (shouldReload) {
      shouldReload = false
      windowsOpen = false
      windowsOpen = true
    }
  }

  // windows open/closed
  if (windowsOpen && !shouldReload) {

    /* Shows combat-related statistics for the raid. */
    OverlayWindow(
      ".: Raid Framer Combat Overlay :.",
      initialPosition = WindowPosition(
        x = Dp(AppState.windowStates.combatState?.lastPositionXDp ?: scaleDpForScreenResolution(5f)),
        y = Dp(AppState.windowStates.combatState?.lastPositionYDp ?: scaleDpForScreenResolution(850f))
      ),
      initialSize = DpSize(
        width = Dp(AppState.windowStates.combatState?.lastWidthDp ?: scaleDpForScreenResolution(470f)),
        height = Dp(AppState.windowStates.combatState?.lastHeightDp ?: scaleDpForScreenResolution(270f))
      ),
      overlayType = OverlayType.COMBAT,
      isObstructing = AppState.isCombatObstructing,
      isVisible = AppState.isCombatOverlayVisible,
      isEverythingVisible = AppState.isEverythingVisible,
      isResizable = AppState.isEverythingResizable,
      isFocusable = false,
      {}
    ) {
      CombatOverlay()
    }

    /* Super Tracker Overlay */
    OverlayWindow(
      ".: Raid Framer Tracker Overlay :.",
      initialPosition = WindowPosition(
        x = Dp(AppState.windowStates.trackerState?.lastPositionXDp ?: scaleDpForScreenResolution(550f)),
        y = Dp(AppState.windowStates.trackerState?.lastPositionYDp ?: scaleDpForScreenResolution(16f))
      ),
      initialSize = DpSize(
        width = Dp(AppState.windowStates.trackerState?.lastWidthDp ?: scaleDpForScreenResolution(470f)),
        height = Dp(AppState.windowStates.trackerState?.lastHeightDp ?: scaleDpForScreenResolution(160f))
      ),
      overlayType = OverlayType.TRACKER,
      isObstructing = AppState.isTrackerObstructing,
      isVisible = AppState.isTrackerOverlayVisible,
      isEverythingVisible = AppState.isEverythingVisible,
      isResizable = AppState.isEverythingResizable,
      isFocusable = false,
      {}
    ) {
      TrackerOverlay()
    }

    /* Filters Window */
    OverlayWindow(
      ".: Raid Framer Filters :.",
      initialPosition = WindowPosition(
        x = Dp(AppState.windowStates.aboutState?.lastPositionXDp ?: scaleDpForScreenResolution(512f)),
        y = Dp(AppState.windowStates.aboutState?.lastPositionYDp ?: scaleDpForScreenResolution(512f))
      ),
      initialSize = DpSize(
        width = Dp(AppState.windowStates.aboutState?.lastWidthDp ?: scaleDpForScreenResolution(256f)),
        height = Dp(AppState.windowStates.aboutState?.lastHeightDp ?: scaleDpForScreenResolution(512f))
      ),
      overlayType = OverlayType.FILTERS,
      isObstructing = mutableStateOf(false),
      isVisible = AppState.isFiltersOverlayVisible,
      isEverythingVisible = mutableStateOf(true),
      isResizable = AppState.isEverythingResizable,
      isFocusable = true,
      {}
    ) {
      FiltersOverlay()
    }

    /* About Window */
    OverlayWindow(
      ".: Raid Framer About :.",
      initialPosition = WindowPosition(
        x = Dp(AppState.windowStates.aboutState?.lastPositionXDp ?: scaleDpForScreenResolution(860f)),
        y = Dp(AppState.windowStates.aboutState?.lastPositionYDp ?: scaleDpForScreenResolution(350f))
      ),
      initialSize = DpSize(
        width = Dp(AppState.windowStates.aboutState?.lastWidthDp ?: scaleDpForScreenResolution(600f)),
        height = Dp(AppState.windowStates.aboutState?.lastHeightDp ?: scaleDpForScreenResolution(750f))
      ),
      overlayType = OverlayType.ABOUT,
      isObstructing = mutableStateOf(false), // Always show opaque windows
      isVisible = AppState.isAboutOverlayVisible,
      isEverythingVisible = mutableStateOf(true),
      isResizable = AppState.isEverythingResizable,
      isFocusable = false,
      {}
    ) {
      AboutOverlay()
    }

    /* Settings Window : Always Opened */
    OverlayWindow(
      ".: Raid Framer Settings :.",
      initialPosition = WindowPosition(
        x = Dp(AppState.windowStates.settingsState?.lastPositionXDp ?: scaleDpForScreenResolution(800f)),
        y = Dp(AppState.windowStates.settingsState?.lastPositionYDp ?: scaleDpForScreenResolution(420f))
      ),
      initialSize = DpSize(
        width = Dp(AppState.windowStates.settingsState?.lastWidthDp ?: scaleDpForScreenResolution(450f)),
        height = Dp(AppState.windowStates.settingsState?.lastHeightDp ?: scaleDpForScreenResolution(740f))
      ),
      overlayType = OverlayType.SETTINGS,
      isObstructing = mutableStateOf(false), // Always show opaque windows
      isVisible = AppState.isSettingsOverlayVisible,
      isEverythingVisible = mutableStateOf(true), // always visible because it's a settings window
      isResizable = AppState.isEverythingResizable,
      isFocusable = true,
      {}
    ) {
      SettingsOverlay()
    }
  }

  /*
   * Dummy window just so the app doesn't close since the framework closes apps that don't have any windows open LOL.
   */
  OverlayWindow(
    ".: Raid Framer Placeholder Window :.",
    initialPosition = WindowPosition(
      x = Dp(0f),
      y = Dp(0f),
    ),
    initialSize = DpSize(
      width = Dp(0f),
      height = Dp(0f),
    ),
    overlayType = OverlayType.DUMMY,
    isObstructing = mutableStateOf(false), // Always show opaque windows
    isVisible = mutableStateOf(false),
    isEverythingVisible = mutableStateOf(true), // always visible because it's a settings window
    isResizable = mutableStateOf(false),
    isFocusable = false,
    {}
  ) {}

}

fun scaleDpForScreenResolution(dp: Float): Float {
  val screenSize = Toolkit.getDefaultToolkit().screenSize
  val userScreenWidth = screenSize.getWidth()
  val userScreenHeight = screenSize.getHeight()

  val baseScreenWidth = 2560.0
  val baseScreenHeight = 1440.0

  val widthScalingFactor = userScreenWidth / baseScreenWidth
  val heightScalingFactor = userScreenHeight / baseScreenHeight

  val scalingFactor = minOf(widthScalingFactor, heightScalingFactor)

  return dp * scalingFactor.toFloat()
}


