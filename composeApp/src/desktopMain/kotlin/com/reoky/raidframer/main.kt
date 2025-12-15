package com.reoky.raidframer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.application
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.database.initialize
import com.reoky.raidframer.core.helpers.ParserHelper
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayContainer
import com.reoky.raidframer.ui.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.system.exitProcess


const val TAG = "Main"

/* ~ Entry Point ~ */
fun main(args: Array<String>) = application {

  val context = rememberCoroutineScope() // correct context for Compose

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

  // file path args processing
  val incoming = args.firstOrNull { it.endsWith(".log", ignoreCase = true) }
  if (incoming != null) {
    try {
      // Trim surrounding quotes if any (Windows may quote paths with spaces).
      val cleaned = incoming.trim().trim('"')
      val p = Paths.get(cleaned)
      if (Files.exists(p)) {
        messageBox("Eek!", "You are viewing a replay of combat data from: $p. Live monitoring is disabled while viewing replays.")
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
      messageBox("Eek!", "Failed to open the specified combat log file: $incoming")
      exitProcess(1)
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
    GameMonitorInteractor.locateCombatLog()
  }
  LaunchedEffect(GameMonitorInteractor.isSearching) {
    GameMonitorInteractor.possiblePaths.value.onEach { path ->
      Log.info(TAG, "Found possible combat log at: ")
    }
  }

  GameMonitorInteractor.chooseCombatLog(Path("C:\\Users\\reoky\\OneDrive\\Documents\\ArcheRage\\combat.log"))
  GameMonitorInteractor.setOptions(GameMonitorInteractor.MonitorModes.MONITOR, Long.MIN_VALUE, Long.MAX_VALUE)

  Log.info(TAG, "Opening default windows...")
  OverlayContainer(wm)
}

/*
 * Cleans up the application by stopping the interactors, closing all the windows, removing listeners, and
 * finally exiting the application.
 */
fun quit() {
  PlayerCacheInteractor.stop()
  GameMonitorInteractor.stop()
  // AppState.tray?.shutdown()
  // AppState.tray?.remove()
  LoggingInteractor.stop()
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
