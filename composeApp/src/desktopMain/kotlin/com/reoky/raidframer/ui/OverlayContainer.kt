package com.reoky.raidframer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import com.reoky.raidframer.ui.overlay.AboutOverlay
import com.reoky.raidframer.ui.overlay.CombatOverlay
import com.reoky.raidframer.ui.overlay.CompanionOverlay
import com.reoky.raidframer.ui.overlay.MiniOverlay
import com.reoky.raidframer.ui.overlay.SettingsOverlay
import com.reoky.raidframer.ui.overlay.SummaryOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.event.ComponentAdapter

@Composable
fun OverlayContainer(wm: WindowManager) {
  println("Rendering Window Containers...")
  OverlayType.entries.forEach { type ->
    val visible = wm.isVisible(type).value
    if (visible) {
      val state by wm.getWindowState(type)
      val obstructing = remember { mutableStateOf(false) }
      val everythingVisible = remember { mutableStateOf(true) }
      val resizable = remember { mutableStateOf(true) }

      OverlayWindow(
        title = type.name,
        initialPosition = WindowPosition(Dp(state.lastPositionXDp), Dp(state.lastPositionYDp)),
        initialSize = DpSize(Dp(state.lastWidthDp), Dp(state.lastHeightDp)),
        windowType = state.windowType,
        isObstructing = obstructing,
        isVisible = wm.visibilityStates[type] ?: mutableStateOf(false),
        isEverythingVisible = everythingVisible,
        isResizable = resizable,
        isFocusable = true,
        onCloseRequest = { wm.closeWindow(type) }
      ) { window ->
        val scope = rememberCoroutineScope()

        when (type) {
          OverlayType.ABOUT -> AboutOverlay(wm)
          OverlayType.COMBAT -> CombatOverlay(wm)
          OverlayType.MINI -> MiniOverlay(wm)
          OverlayType.SETTINGS -> SettingsOverlay(wm)
          OverlayType.COMPANION -> CompanionOverlay(wm)
          OverlayType.SUMMARY -> SummaryOverlay(wm)
          else -> {}//throw Exception("Overlay type $type not implemented")
        }

        // initial notify
        notifyWindowManagerStateChanged(type, wm, window)

        // The below code listens for the window to be moved or resized and notifies
        // the WindowManager to update the stored state accordingly. Debounces so we
        // don't spam updates during drag/resize.
        DisposableEffect(window) {
          var updateJob: Job? = null
          val debounceMs = 600L

          val listener = object : ComponentAdapter() {
            private fun scheduleUpdate() {
              // cancel previous pending update and schedule a new delayed one
              updateJob?.cancel()
              updateJob = scope.launch {
                kotlinx.coroutines.delay(debounceMs)
                notifyWindowManagerStateChanged(type, wm, window)
              }
            }

            override fun componentMoved(e: java.awt.event.ComponentEvent?) {
              scheduleUpdate()
            }

            override fun componentResized(e: java.awt.event.ComponentEvent?) {
              scheduleUpdate()
            }
          }

          window.addComponentListener(listener)

          onDispose {
            window.removeComponentListener(listener)
            updateJob?.cancel()
          }
        }

      }
    }
  }
}

fun notifyWindowManagerStateChanged(
  type: OverlayType,
  windowManager: WindowManager,
  window: ComposeWindow
) {
  try {
    val pos = window.locationOnScreen
    val size = window.size
    windowManager.updateWindowState(type) {
      copy(
        lastPositionXDp = pos.x.toFloat(),
        lastPositionYDp = pos.y.toFloat(),
        lastWidthDp = size.width.toFloat(),
        lastHeightDp = size.height.toFloat()
      )
    }
    println("Updated window state for $type: pos=(${pos.x}, ${pos.y}), size=(${size.width}, ${size.height})")
  } catch (e: Exception) {
    // window isn't open yet
    // println("Could not get window position/size for $type: ${e.message}")
  }
}
