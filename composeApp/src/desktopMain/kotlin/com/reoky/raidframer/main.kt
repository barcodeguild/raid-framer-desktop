package com.reoky.raidframer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import com.reoky.raidframer.core.database.initialize
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.OverlayWindow
import com.reoky.raidframer.ui.overlay.AboutOverlay
import com.reoky.raidframer.ui.overlay.RaidOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

data class RaidMember(val name: String, val health: Int, val role: String = "Healer")

typealias Party = List<RaidMember>

/* ~ Entry Point ~ */
fun main() = application {

  // Initialize the database
  val database = try {
    // This will create the database and tables if they don't exist
    initialize()
  } catch (e: Exception) {
    println("eeeeeek: ${e.message}")
    exitProcess(1)
  }

  /*
   * Test loading the positions of the windows from the database.
   */
  CoroutineScope(Dispatchers.Main).launch {
    try {
      val windowStates = database.getWindowStateDao().getAll()
      windowStates.forEach {
        println(it)
      }
    } catch (e: Exception) {
      println("eeeeeek: ${e.message}")
      exitProcess(1)
    }
  }

//  val raid: List<Party> = List(10) { partyIndex ->
//    List(5) { memberIndex ->
//      RaidMember("P${partyIndex+1}M${memberIndex+1}", 100)
//    }
//  }

//  OverlayWindow(
//    ".: Raid Framer Raid Summary :.",
//    initialPosition = WindowPosition(
//      x = Dp(0f), // Dp(lol.rfcloud.AppState.windowStates.aboutState?.lastPositionXDp ?: lol.rfcloud.scaleDpForScreenResolution(860f)),
//      y = Dp(0f) // Dp(lol.rfcloud.AppState.windowStates.aboutState?.lastPositionYDp ?: lol.rfcloud.scaleDpForScreenResolution(350f))
//    ),
//    initialSize = DpSize(
//      width = Dp(600f),
//      height = Dp(750f)
//    ),
//    overlayType = OverlayType.ABOUT,
//    isObstructing = mutableStateOf(false), // Always show opaque windows
//    isVisible = mutableStateOf(true),
//    isEverythingVisible = mutableStateOf(true),
//    isResizable = AppState.isEverythingResizable,
//    isFocusable = false,
//    {}
//  ) {
//    RaidOverlay(raid)
//    TimeRangeSlider(
//      minTime = 0f,
//      maxTime = 100f,
//      startTime = 20f,
//      endTime = 80f,
//      activityLevels = List(10) { it / 10f }, // Example activity levels
//      onRangeChange = { start, end ->
//        println("Selected range: $start to $end")
//      }
//    )
//  }
  OverlayWindow(
    ".: Raid Framer About :.",
    initialPosition = WindowPosition(
      x = Dp(0f), // Dp(lol.rfcloud.AppState.windowStates.aboutState?.lastPositionXDp ?: lol.rfcloud.scaleDpForScreenResolution(860f)),
      y = Dp(0f) // Dp(lol.rfcloud.AppState.windowStates.aboutState?.lastPositionYDp ?: lol.rfcloud.scaleDpForScreenResolution(350f))
    ),
    initialSize = DpSize(
      width = Dp(600f),
      height = Dp(750f)
    ),
    overlayType = OverlayType.ABOUT,
    isObstructing = mutableStateOf(false), // Always show opaque windows
    isVisible = mutableStateOf(true),
    isEverythingVisible = mutableStateOf(true),
    isResizable = AppState.isEverythingResizable,
    isFocusable = false,
    {}
  ) {
    AboutOverlay()
  }
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

data class PieChartData(
  val value: Float,
  val color: Color
)
