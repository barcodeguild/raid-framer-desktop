package com.reoky.raidframer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.application
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.database.initialize
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.InstallationInteractor
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayContainer
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Locale
import kotlin.io.path.Path
import kotlin.system.exitProcess

const val TAG = "Main"

var tray: SystemTray? = null

/* ~ Entry Point ~ */
fun main(args: Array<String>) = application {

  val context = rememberCoroutineScope() // correct context for Compose
  var languageCode by remember { mutableStateOf(Locale.getDefault().language) } // default system language

  fun updateLanguage(newCode: String) {
    if (newCode == languageCode) return
    val locale = Locale.Builder().setLanguage(newCode).build()
    Locale.setDefault(locale)
    languageCode = newCode
  }

  // Initialize the database : critical that this occurs first and only once
  val database = remember {
    try {
      // This will create the database and tables if they don't exist
      initialize().also { db ->
        RFDao.init(db)
      }
    } catch (e: Exception) {
      println("Oh eek, the database wouldn't open, friend: ${e.message}")
      exitProcess(1)
    }
  }

  RFConfig.init(RFDao.configDao)

  // Start core of the app
  LoggingInteractor.start()
  PlayerCacheInteractor.start()
  GameMonitorInteractor.start()
  InstallationInteractor.start(delay = 3000L) // delay to allow user to set game path in settings if needed
  CompanionInteractor.start(delay = 1000L) // delay to allow game monitor to start first

  // file path args processing
  val incoming = args.firstOrNull { it.endsWith(".log", ignoreCase = true) }
  if (incoming != null) {
    try {
      // Trim surrounding quotes if any (Windows may quote paths with spaces).
      val cleaned = incoming.trim().trim('"')
      val p = Paths.get(cleaned)
      if (Files.exists(p)) {
        messageBox(AppGlobals.APP_NAME, "You are viewing a replay of combat data from: $p. Live monitoring is disabled while viewing replays.")
        GameMonitorInteractor.chooseCombatLog(p)
        GameMonitorInteractor.setOptions(
          mode = GameMonitorInteractor.MonitorModes.REPLAY,
          startMarker = 0L,
          endMarker = Long.MAX_VALUE
        )
        GameMonitorInteractor.restart()
      }
    } catch (_: Exception) {
      Log.error(TAG, "Failed to process incoming log path: $incoming")
      messageBox(AppGlobals.APP_NAME, "Failed to open the specified combat log file: $incoming")
      exitProcess(1)
    }
  } else {
    // choose game path default automatically
    context.launch(Dispatchers.IO) {
      Log.info(TAG, "Searching for combat log path...")
      GameMonitorInteractor.locateArcheRageDirectory()
    }
    LaunchedEffect(GameMonitorInteractor.isSearching) {
      val automaticLogPath = GameMonitorInteractor.possiblePaths.value.firstOrNull()
      automaticLogPath?.let {
        it
        Log.info(TAG, "Automatically choosing the combat log file here: $it")
        GameMonitorInteractor.chooseCombatLog(it)
        GameMonitorInteractor.setOptions(GameMonitorInteractor.MonitorModes.MONITOR, 0L, Long.MAX_VALUE)
      }
    }
  }

  val wm = remember {
    WindowManager(scope = context, dao = RFDao.windowStateDao)
  }

  // at least one window has to be open on app start to prevent immediate exit
  // however, we also want to load saved states of windows before opening them
  runBlocking {
    if (AppGlobals.DEBUG_WIPE_DB_AND_CACHE_ON_LAUNCH) RFDao.windowStateDao.deleteAll() // clear saved window positions and sizes for testing
    val startTime: Long = System.currentTimeMillis()
    Log.info(TAG, "Loading saved window states...")
    wm.loadStates()
    Log.info(TAG, "Finished loading saved window states. Took ${System.currentTimeMillis() - startTime} ms")
  }

  // start the game monitor
  context.launch(Dispatchers.IO) {
    GameMonitorInteractor.locateArcheRageDirectory()
  }
  LaunchedEffect(GameMonitorInteractor.isSearching) {
    GameMonitorInteractor.possiblePaths.value.onEach { path ->
      Log.info(TAG, "Found possible combat log at: ")
    }
  }

  // spawn the system tray
  tray = spawnSystemTray(wm)

  Log.info(TAG, "Opening default windows...")
  OverlayContainer(wm)

  // gets the config and show the about window on first launch then updates the firstLaunch flag
  LaunchedEffect(Unit) {
    RFDao.configDao.getConfig()?.let { config ->
      if (config.firstLaunch) {
        wm.openWindow(OverlayType.ABOUT)
        RFDao.configDao.insert(config.copy(firstLaunch = false))
      }
    }
  }
}

/*
 * Cleans up the application by stopping the interactors, closing all the windows, removing listeners, and
 * finally exiting the application.
 */
fun quit() {
  PlayerCacheInteractor.stop()
  GameMonitorInteractor.stop()
  tray?.shutdown()
  tray?.remove()
  LoggingInteractor.stop() // should be last because this is the thing that logs shutdown messages
  exitProcess(0)
}

/*
 * Displays a simple Windows style message box with the given title and message.
 */
fun messageBox(title: String, message: String) {
  javax.swing.JOptionPane.showMessageDialog(
    null,
    message,
    title,
    javax.swing.JOptionPane.INFORMATION_MESSAGE
  )
}

/*
 * Spawns the system tray icon and menu. I basically added this in case the overlay's go behind other windows or
 * off the screen, so the user has a way to get them back.
 */
@Composable
fun spawnSystemTray(wm: WindowManager): SystemTray {
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
    wm.openWindow(OverlayType.SETTINGS)
  })
  tray.menu.add(MenuItem("About") {
    wm.openWindow(OverlayType.ABOUT)
  })
  tray.setImage(iconImage)
  return tray
}