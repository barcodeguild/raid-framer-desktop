package com.reoky.raidframer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.config.RenderingBackendConfig
import com.reoky.raidframer.core.database.AppDatabase
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.database.initialize
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.core.pocket.initializePocketDraftCoordinator
import com.reoky.raidframer.core.helpers.UpdateHelper
import com.reoky.raidframer.core.helpers.UpdateStatus
import com.reoky.raidframer.core.recovery.RecoveryMode
import com.reoky.raidframer.ui.RecoveryWindow
import com.reoky.raidframer.core.interactor.BattleGraphInteractor
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.CombatLogInteractor
import com.reoky.raidframer.core.interactor.CrashLogger
import com.reoky.raidframer.core.interactor.DeathAccumulatorInteractor
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.GraphDataInteractor
import com.reoky.raidframer.core.interactor.InstallationInteractor
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.core.interactor.OverlayInteractor
import com.reoky.raidframer.core.interactor.PetAccumulatorInteractor
import com.reoky.raidframer.core.interactor.ProtectiveWingsAttributorInteractor
import com.reoky.raidframer.core.interactor.AreaEffectAttributorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.interactor.PatternAnalysisInteractor
import com.reoky.raidframer.core.seedtable.SeedTableInteractor
import com.reoky.raidframer.ui.OverlayContainer
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.SystemTrayComponent
import com.reoky.raidframer.core.locale.AppLocale
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT.HANDLE
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.app_replay_viewing_format
import raid_framer_desktop.composeapp.generated.resources.seed_table_apply_prompt
import raid_framer_desktop.composeapp.generated.resources.seed_table_apply_title
import raid_framer_desktop.composeapp.generated.resources.seed_table_applied_success
import raid_framer_desktop.composeapp.generated.resources.seed_table_file_not_found

const val TAG = "Main"
private const val SINGLE_INSTANCE_MUTEX = "Local\\RaidFramerDesktopSingleInstance"

private var appMutexHandle: HANDLE? = null

/* ~ Entry Point ~ */
fun main(args: Array<String>) {
  System.setProperty("skiko.renderApi", RenderingBackendConfig.load())
  CrashLogger.install()

  // Prevent duplicate launch
  if (!acquireSingleInstanceMutex()) {
    messageBox(AppGlobals.APP_NAME, "Raid Framer is already running.")
    exitProcess(1)
  }

  // Recovery shim: a --recovery argument (or the marker file left by Settings) boots
  // the app straight into the recovery window without ever touching the database.
  // The marker is consumed only by the instance that wins the single-instance mutex.
  val recoveryRequested = args.any { it.equals(RecoveryMode.LAUNCH_ARG, ignoreCase = true) } ||
    RecoveryMode.consumeMarkerFile()

  application {
    val context = rememberCoroutineScope()

    /*
     * ~ Database bootstrap gate ~
     * Nothing of the main app (UI, interactors, config) initializes until the database
     * is ready; the rest of the app simply waits here. If initialization fails (a
     * migration failure or a corrupted database), the app defers to the recovery
     * window instead of dying. The gate runs off the main thread, so composition is
     * never blocked on the database.
     */
    var bootstrap by remember { mutableStateOf<Bootstrap>(Bootstrap.Booting) }

    LaunchedEffect(Unit) {
      if (recoveryRequested) {
        RecoveryMode.enter(RecoveryMode.Reason.REQUESTED_BY_USER)
        bootstrap = Bootstrap.Recovery
        return@LaunchedEffect
      }
      try {
        withContext(Dispatchers.IO) { bootstrapDatabase() }
        bootstrap = Bootstrap.Ready
      } catch (e: Throwable) {
        Log.error(TAG, e, "The database would not open, entering recovery mode")
        RecoveryMode.enter(RecoveryMode.Reason.MIGRATION_FAILURE, e)
        bootstrap = Bootstrap.Recovery
      }
    }

    when (bootstrap) {
      Bootstrap.Booting -> {
        // Waiting for the database; nothing of the app has initialized yet.
        KeepAliveWindow()
      }
      Bootstrap.Recovery -> RecoveryWindow(
        reason = RecoveryMode.reason,
        error = RecoveryMode.error
      )
      Bootstrap.Ready -> MainAppContent(context, args)
    }
  }
}

/*
 * Compose Desktop ends the application the moment its root composition holds no
 * windows, which would kill the app while the database bootstraps or while saved
 * window states are loading. This invisible placeholder keeps the composition alive
 * until the real windows (or the recovery window) exist.
 */
@Composable
private fun KeepAliveWindow() {
  Window(
    onCloseRequest = {},
    visible = false,
    undecorated = true,
    resizable = false
  ) {}
}

private enum class Bootstrap { Booting, Recovery, Ready }

/*
 * Initializes the database and reads the config once. Any failure here (most likely a
 * failed migration) is what triggers recovery mode. The first query is what actually
 * opens the database and runs migrations.
 */
private fun bootstrapDatabase() {
  var db: AppDatabase? = null
  try {
    db = initialize().also { initializeDb -> RFDao.init(initializeDb) }
    RFConfig.init(RFDao.configDao)
    // The first query is what actually opens the database and runs migrations, and it
    // must not race anything else touching the database: on a migration failure every
    // other connection still has to be released before recovery mode can move the
    // files. So the synchronous config load goes first and the coordinator (which
    // queries the database the moment it is initialized) only starts once the database
    // is known to open.
    RFConfig.initSync()
    initializePocketDraftCoordinator(RFDao.pocketDao)
  } catch (e: Throwable) {
    // Stop every component that could still be touching the database before handing it
    // to recovery mode, so its file lock can actually be released.
    RFConfig.shutdown()
    PocketDraftCoordinator.shutdown()
    // Hand the half-opened database to recovery mode so its file lock can be released.
    RecoveryMode.attachFailedDatabase(db)
    throw e
  }
}

/*
 * The main app: everything that depends on the database. Only called once the
 * bootstrap gate reports the database is ready.
 */
@Composable
private fun ApplicationScope.MainAppContent(context: CoroutineScope, args: Array<String>) {
  val configuredBackend = RFConfig.state.value.renderingBackend
    .uppercase()
    .takeIf { it in setOf("OPENGL", "DIRECTX", "SOFTWARE") }
  if (configuredBackend != null) {
    RenderingBackendConfig.save(configuredBackend)
  }
  Log.info(TAG, "Configured Skiko rendering backend: ${System.getProperty("skiko.renderApi")}")

  // Apply language preference before Compose starts (must be before first composition)
  try {
    AppLocale.apply(RFConfig.state.value.preferredLanguage.orEmpty())
  } catch (e: Exception) {
    println("Could not load language preference: ${e.message}")
  }

  // Start core of the app
  LoggingInteractor.initialize() // prunes old logs
  LoggingInteractor.start()
  PlayerCacheInteractor.start()
  GraphDataInteractor.start(delay = 60000L)
  //GameMonitorInteractor.start()
  InstallationInteractor.start(delay = 3000L)
  CompanionInteractor.start(delay = 1000L)
  OverlayInteractor.start(delay = 50L)
  DeathAccumulatorInteractor.start()
  PetAccumulatorInteractor.start()
  AreaEffectAttributorInteractor.start(delay = 1000L)
  ProtectiveWingsAttributorInteractor.start()
  PatternAnalysisInteractor.start(delay = 1000L)
  CombatLogInteractor.start(delay = 3000L)
  SeedTableInteractor.start(delay = 2000L)
  BattleGraphInteractor.start()

  // Background update check on startup (non-blocking, silent)
  context.launch(Dispatchers.IO) {
    if (RFConfig.state.value.autoUpdateEnabled) {
      Log.info(TAG, "Checking for updates on startup...")
      UpdateHelper.checkForUpdates { status ->
        if (status is UpdateStatus.Available) {
          Log.info(TAG, "Update available: ${status.updateInfo.version}")
        } else {
          Log.info(TAG, "No update available or check failed.")
        }
      }
    }
  }

  // file path args processing
  val incoming = args.firstOrNull { it.endsWith(".log", ignoreCase = true) || it.endsWith(".rfst", ignoreCase = true) }
  if (incoming != null) {
    val cleaned = incoming.trim().trim('"')
    val p = Paths.get(cleaned)

    if (cleaned.endsWith(".rfst", ignoreCase = true)) {
      if (Files.exists(p)) {
        val prompt = String.format(stringResource(Res.string.seed_table_apply_prompt), p.toString())
        // Always-on-top confirm (see messageBox) so the prompt is reachable over the game.
        val pane = JOptionPane(prompt, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION)
        val dialog = pane.createDialog(stringResource(Res.string.seed_table_apply_title))
        dialog.isAlwaysOnTop = true
        dialog.isVisible = true
        val result = pane.value as? Int ?: JOptionPane.CLOSED_OPTION
        if (result == JOptionPane.YES_OPTION) {
          SeedTableInteractor.importSeedTable(p.toFile())
          messageBox(AppGlobals.APP_NAME, stringResource(Res.string.seed_table_applied_success))
        }
      } else {
        messageBox(AppGlobals.APP_NAME, String.format(stringResource(Res.string.seed_table_file_not_found), incoming))
      }
    } else if (cleaned.endsWith(".log", ignoreCase = true)) {
      if (Files.exists(p)) {
        messageBox(AppGlobals.APP_NAME, stringResource(Res.string.app_replay_viewing_format, p.toString()))
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
    val possiblePaths by GameMonitorInteractor.possiblePaths.collectAsState()
    LaunchedEffect(possiblePaths) {
      possiblePaths.firstOrNull()?.let { path ->
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
      if (config.itemUseOverlayEnabled) wm.openWindow(OverlayType.ITEM_USE)
      if (config.raidCallerOverlayEnabled) wm.openWindow(OverlayType.RAID_CALLER)
      if (config.lastSessionStart > 0) {
        Log.info(TAG, "Stale session detected (lastSessionStart=${config.lastSessionStart}), clearing on launch.")
        AppState.crashRecoverySessionTitle = config.lastSessionTitle
        RFConfig.update {
          it.copy(
            lastSessionStart = 0L,
            lastSessionTitle = "",
            lastSessionType = "",
            lastSessionDurationMs = 0L,
            previousSessionStart = config.lastSessionStart
          )
        }
      }
    }


    statesLoaded = true
  }

  if (statesLoaded) {
    SystemTrayComponent(wm, ::quit)
    Log.info(TAG, "Opening default windows...")
    OverlayContainer(wm)
  } else {
    // Window states are still loading; keep the composition alive meanwhile.
    KeepAliveWindow()
  }
}

/*
 * Displays a simple Windows style message box with the given title and message, forced
 * on top of other windows. This app is a game overlay: a plain dialog opens behind the
 * game where it cannot be focused or even alt-tabbed to.
 */
fun messageBox(title: String, message: String) {
  val pane = JOptionPane(message, JOptionPane.INFORMATION_MESSAGE)
  val dialog = pane.createDialog(title)
  dialog.isAlwaysOnTop = true
  dialog.isVisible = true
}

/*
 * Acquires the single-instance mutex, retrying briefly while the previous instance shuts
 * down. Recovery mode relaunches a fresh process before quitting the old one (both the
 * recovery window's "Launch App" and the Settings entry point do this), so without the
 * wait the new instance would bounce off the mutex and show a confusing "already
 * running" message instead of the window it was spawned to show. A user genuinely
 * double-launching the app just waits out the same short window before the notice.
 */
private fun acquireSingleInstanceMutex(totalWaitMillis: Long = 5_000L): Boolean {
  val deadline = System.currentTimeMillis() + totalWaitMillis
  while (true) {
    val handle = Kernel32.INSTANCE.CreateMutex(null, false, SINGLE_INSTANCE_MUTEX)
    if (handle == null) {
      Log.error(TAG, "CreateMutex returned null for single-instance mutex")
    } else {
      val alreadyExists = Kernel32.INSTANCE.GetLastError() == WinError.ERROR_ALREADY_EXISTS
      if (!alreadyExists) {
        appMutexHandle = handle
        return true
      }
      Kernel32.INSTANCE.CloseHandle(handle)
    }
    if (System.currentTimeMillis() >= deadline) return false
    Thread.sleep(250L)
  }
}

/*
 * Stops all the interactors and exits the application cleanly.
 */
fun quit() {
  Log.info(TAG, "Shutting down Raid Framer...")
  BattleGraphInteractor.stop()
  GraphDataInteractor.stop()
  PlayerCacheInteractor.stop()
  GameMonitorInteractor.stop()
  DeathAccumulatorInteractor.stop()
  PatternAnalysisInteractor.stop()
  CompanionInteractor.stop()
  InstallationInteractor.stop()
  CombatLogInteractor.stop()
  LoggingInteractor.stop() // should be last because this is the thing that logs shutdown message
  appMutexHandle?.let { Kernel32.INSTANCE.CloseHandle(it) }
  appMutexHandle = null
  exitProcess(0)
}

/*
 * Stops the recording session, waits for the PNG export and session archive to complete,
 * then quits the application. Falls through to quit() immediately if not recording.
 */
suspend fun quitAfterSessionStop() {
  if (CombatLogInteractor.isRecording.value) {
    Log.info(TAG, "Session active — stopping recording and awaiting export before quit...")
    PlayerCacheInteractor.stopSession()
  }
  // The session may already have been stopped from the combat/settings controls.
  // Always await any jobs started by that stop before terminating the process.
  CombatLogInteractor.awaitExport()
  PlayerCacheInteractor.awaitArchive()
  quit()
}
