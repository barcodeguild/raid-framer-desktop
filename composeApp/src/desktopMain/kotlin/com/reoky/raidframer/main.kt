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
import com.reoky.raidframer.core.interactor.CombatLogInteractor
import com.reoky.raidframer.core.interactor.DeathAccumulatorInteractor
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.InstallationInteractor
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.core.interactor.OverlayInteractor
import com.reoky.raidframer.core.interactor.PetAccumulatorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.seedtable.SeedTableInteractor
import com.reoky.raidframer.ui.OverlayContainer
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT.HANDLE
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.general_help_window_postions_reset
import raid_framer_desktop.composeapp.generated.resources.raidframer
import raid_framer_desktop.composeapp.generated.resources.seed_table_apply_prompt
import raid_framer_desktop.composeapp.generated.resources.seed_table_apply_title
import raid_framer_desktop.composeapp.generated.resources.seed_table_applied_success
import raid_framer_desktop.composeapp.generated.resources.seed_table_file_not_found

const val TAG = "Main"
private const val SINGLE_INSTANCE_MUTEX = "Local\\RaidFramerDesktopSingleInstance"

private var appMutexHandle: HANDLE? = null
private var tray: SystemTray? = null

/* ~ Entry Point ~ */
fun main(args: Array<String>) {
  // Prevent duplicate launch
  if (!acquireSingleInstanceMutex()) {
    messageBox(AppGlobals.APP_NAME, "Raid Framer is already running.")
    exitProcess(1)
  }

  application {
    val context = rememberCoroutineScope()

    // Initialize the database: critical that this occurs first and only once
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
    InstallationInteractor.start(delay = 3000L)
    CompanionInteractor.start(delay = 1000L)
    OverlayInteractor.start(delay = 50L)
    DeathAccumulatorInteractor.start()
    PetAccumulatorInteractor.start()
    CombatLogInteractor.start(delay = 3000L)
    SeedTableInteractor.start(delay = 2000L)

    // file path args processing
    val incoming = args.firstOrNull { it.endsWith(".log", ignoreCase = true) || it.endsWith(".rfst", ignoreCase = true) }
    if (incoming != null) {
      val cleaned = incoming.trim().trim('"')
      val p = Paths.get(cleaned)

      if (cleaned.endsWith(".rfst", ignoreCase = true)) {
        if (Files.exists(p)) {
          val prompt = String.format(stringResource(Res.string.seed_table_apply_prompt), p.toString())
          val result = JOptionPane.showConfirmDialog(
            null,
            prompt,
            stringResource(Res.string.seed_table_apply_title),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
          )
          if (result == JOptionPane.YES_OPTION) {
            SeedTableInteractor.importSeedTable(p.toFile())
            messageBox(AppGlobals.APP_NAME, stringResource(Res.string.seed_table_applied_success))
          }
        } else {
          messageBox(AppGlobals.APP_NAME, String.format(stringResource(Res.string.seed_table_file_not_found), incoming))
        }
      } else if (cleaned.endsWith(".log", ignoreCase = true)) {
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
      }
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
          RFConfig.update { it.copy(firstLaunch = false) }
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
 * Spawns the system tray icon and menu. I basically added this in case overlays appear behind other windows or
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

private fun acquireSingleInstanceMutex(): Boolean {
  val handle = Kernel32.INSTANCE.CreateMutex(null, false, SINGLE_INSTANCE_MUTEX)
  if (handle == null) {
    Log.error(TAG, "CreateMutex returned null for single-instance mutex")
    return false
  }

  val alreadyExists = Kernel32.INSTANCE.GetLastError() == WinError.ERROR_ALREADY_EXISTS
  if (alreadyExists) {
    Kernel32.INSTANCE.CloseHandle(handle)
    return false
  }

  appMutexHandle = handle
  return true
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
  CombatLogInteractor.stop()
  tray?.shutdown()
  tray?.remove()
  LoggingInteractor.stop() // should be last because this is the thing that logs shutdown message
  appMutexHandle?.let { Kernel32.INSTANCE.CloseHandle(it) }
  appMutexHandle = null
  exitProcess(0)
}
