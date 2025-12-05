package com.reoky.raidframer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.database.initialize
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayContainer
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

data class RaidMember(val name: String, val health: Int, val role: String = "Healer")

typealias Party = List<RaidMember>

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
        //GameMonitorInteractor.openLogFileFromExternal(p)
        //GameMonitorInteractor.setMode(GameMonitorInteractor.MonitorModes.REPLAY)
        //GameMonitorInteractor.play()
      }
    } catch (_: Exception) {
      // ignore malformed path
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
    println(wm.visibilityStates[OverlayType.COMBAT]?.value)
    println(wm.isVisible(OverlayType.COMBAT).value)
    assert(wm.visibilityStates[OverlayType.COMBAT]?.value ?: false)
  }

  // start the game monitor
  GameMonitorInteractor.locateCombatLog()
  LaunchedEffect(GameMonitorInteractor.isSearching) {
    GameMonitorInteractor.possiblePaths.value.onEach { path ->
      println(path)
    }
  }

  Log.info(TAG, "Opening default windows...")
  OverlayContainer(wm)
}

@Composable
fun TimeRangeSlider(
  minTime: Float,
  maxTime: Float,
  startTime: Float,
  endTime: Float,
  activityLevels: List<Float>, // values between 0..1
  onRangeChange: (Float, Float) -> Unit
) {
  val sliderWidth = 400.dp
  val handleRadius = 10.dp
  Box(
    modifier = Modifier.width(sliderWidth).height(60.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val widthPx = size.width
      val heightPx = size.height

      // Draw activity level bar
      val barHeight = heightPx * 0.3f
      activityLevels.forEachIndexed { i, level ->
        val x = i * widthPx / (activityLevels.size - 1)
        drawLine(
          color = Color.Green.copy(alpha = level),
          start = Offset(x, heightPx / 2 - barHeight / 2),
          end = Offset(x, heightPx / 2 + barHeight / 2),
          strokeWidth = 2f
        )
      }

      // Draw time markers (ticks)
      for (i in 0..10) {
        val x = i * widthPx / 10
        drawLine(
          color = Color.Gray,
          start = Offset(x, heightPx / 2 - 12),
          end = Offset(x, heightPx / 2 + 12),
          strokeWidth = 1f
        )
      }

      // Draw slider bar
      drawLine(
        color = Color.LightGray,
        start = Offset(0f, heightPx / 2),
        end = Offset(widthPx, heightPx / 2),
        strokeWidth = 4f
      )

      // Draw handles
      val startX = ((startTime - minTime) / (maxTime - minTime)) * widthPx
      val endX = ((endTime - minTime) / (maxTime - minTime)) * widthPx
      drawCircle(Color.Blue, handleRadius.toPx(), Offset(startX, heightPx / 2))
      drawCircle(Color.Red, handleRadius.toPx(), Offset(endX, heightPx / 2))
    }
    // Add gesture handling for dragging handles (omitted for brevity)
  }
}

/*
 * Cleans up the application by stopping the interactors, closing all the windows, removing listeners, and
 * finally exiting the application.
 */
fun quit() {
  //    CombatEventInteractor.stop()
  //    OverlayInteractor.stop()
  //    AppState.tray?.shutdown()
  //    AppState.tray?.remove()
  exitProcess(0)
}

fun messageBox(title: String, message: String) {
  javax.swing.JOptionPane.showMessageDialog(
    null,
    message,
    title,
    javax.swing.JOptionPane.INFORMATION_MESSAGE
  )
}

data class PieChartData(
  val value: Float,
  val color: Color
)
