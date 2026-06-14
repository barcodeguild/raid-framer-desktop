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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.application
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.database.initialize
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.DeathAccumulatorInteractor
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.InstallationInteractor
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.core.interactor.PetAccumulatorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayContainer
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.app_already_running
import raid_framer_desktop.composeapp.generated.resources.general_help_window_postions_reset
import raid_framer_desktop.composeapp.generated.resources.raidframer

const val TAG = "Main"

private val lockFile = Paths.get(System.getProperty("user.home"), ".RaidFramer", ".raidframer.lock")
private var lockChannel: FileChannel? = null
private var tray: SystemTray? = null

/* ~ Entry Point ~ */
fun main(args: Array<String>) = application {

  // Prevent duplicate launch
  if (!acquireLock()) {
    messageBox(AppGlobals.APP_NAME, stringResource(Res.string.app_already_running))
    exitProcess(1)
  }

  val context = rememberCoroutineScope()

  // Initialize the database : critical that this occurs first and only once
  remember {
    try {
      initialize().also { db -> RFDao.init(db) }
    } catch (e: Exception) {
      println("Oh eek, the database wouldn't open, friend: ${e.message}")
      exitProcess(1)
    }
  }

  RFConfig.init(RFDao.configDao)

  // Start core of the app
  LoggingInteractor.initialize() // prunes old logs
  LoggingInteractor.start()
  PlayerCacheInteractor.start()
  //GameMonitorInteractor.start()
  InstallationInteractor.start(delay = 3000L) // delay to allow user to set game path in settings if needed
  CompanionInteractor.start(delay = 1000L) // delay to allow game monitor to start first
  OverlayInteractor.start(delay = 150L) // show to allow for hiding overlays quickly
  DeathAccumulatorInteractor.start()
  PetAccumulatorInteractor.start()
  
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

  // not viewing a replay so pick the path from settings
  } else {
    // choose game path default automatically
    context.launch(Dispatchers.IO) {
      Log.info(TAG, "Searching for combat log path...")
      GameMonitorInteractor.locateArcheRageDirectory()
    }
    LaunchedEffect(GameMonitorInteractor.isSearching) {
      val automaticLogPath = GameMonitorInteractor.possiblePaths.value.firstOrNull()
      automaticLogPath?.let { path ->
        Log.info(TAG, "Automatically choosing the combat log file here: $path")
        GameMonitorInteractor.chooseCombatLog(path)
        GameMonitorInteractor.setOptions(GameMonitorInteractor.MonitorModes.MONITOR, 0L, Long.MAX_VALUE)
      }
    }
  }

  val wm = remember {
    WindowManager(scope = context, dao = RFDao.windowStateDao)
  }

  var statesLoaded by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (AppGlobals.DEBUG_WIPE_DB_AND_CACHE_ON_LAUNCH) RFDao.windowStateDao.deleteAll()
    val startTime: Long = System.currentTimeMillis()
    Log.info(TAG, "Loading saved window states...")
    wm.loadStates()
    Log.info(TAG, "Finished loading saved window states. Took ${System.currentTimeMillis() - startTime} ms")

    RFDao.configDao.getConfig()?.let { config ->
      if (config.firstLaunch) {
        wm.openWindow(OverlayType.ABOUT)
        RFDao.configDao.insert(config.copy(firstLaunch = false))
      }
      if (config.miniGraphEnabled) wm.openWindow(OverlayType.MINI)
    }

    statesLoaded = true
  }

  if (statesLoaded) {
    tray = spawnSystemTray(wm)
    Log.info(TAG, "Opening default windows...")
    OverlayContainer(wm)
  }
}

/*
 * Displays a simple Windows style message box with the given title and message.
 */
fun messageBox(title: String, message: String) {
  JOptionPane.showMessageDialog(
    null,
    message,
    title,
    JOptionPane.INFORMATION_MESSAGE
  )
}

/*
 * Spawns the system tray icon and menu. I basically added this in case the overlay's go behind other windows or
 * off the screen, so the user has a way to get them back.
 */
@Composable
fun spawnSystemTray(wm: WindowManager): SystemTray {
  val tray = SystemTray.get()
  val helpString = stringResource(Res.string.general_help_window_postions_reset)

  // Updated to use the type-safe Res accessor
  val iconImage = painterResource(Res.drawable.raidframer).toAwtImage(
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
  tray.menu.add(MenuItem("Reset Window Positions") {
    wm.resetAllWindowPositions()
    messageBox(AppGlobals.APP_NAME, helpString)
  })
  tray.menu.add(MenuItem("Exit") {
    quit()
  })
  tray.setImage(iconImage)
  return tray
}

private fun acquireLock(): Boolean {
  return try {
    Files.createDirectories(lockFile.parent)
    lockChannel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    val fileLock = lockChannel?.tryLock()
    if (fileLock == null) {
      lockChannel?.close()
      lockChannel = null
      false
    } else {
      true
    }
  } catch (e: Exception) {
    lockChannel?.close()
    lockChannel = null
    true
  }
}

/*
 * Stops all the interactors and exits the application cleanly.
 */
fun quit() {
  Log.info(TAG, "Shutting down Raid Framer...")
  PlayerCacheInteractor.stop()
  GameMonitorInteractor.stop()
  DeathAccumulatorInteractor.stop()
  CompanionInteractor.stop()
  InstallationInteractor.stop()
  tray?.shutdown()
  tray?.remove()
  LoggingInteractor.stop() // should be last because this is the thing that logs shutdown message
  lockChannel?.close()
  exitProcess(0)
}
