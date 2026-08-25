package com.reoky.raidframer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.locale.AppLocale
import com.reoky.raidframer.ui.overlay.AboutOverlay
import com.reoky.raidframer.ui.overlay.SummaryOverlay
import com.reoky.raidframer.ui.overlay.CombatOverlay
import com.reoky.raidframer.ui.overlay.CompanionOverlay
import com.reoky.raidframer.ui.overlay.MiniOverlay
import com.reoky.raidframer.ui.overlay.NewSessionOverlay
import com.reoky.raidframer.ui.overlay.PokemonOverlay
import com.reoky.raidframer.ui.overlay.RaidOverlay
import com.reoky.raidframer.ui.overlay.SettingsOverlay
import com.reoky.raidframer.ui.overlay.HelpOverlay
import com.reoky.raidframer.ui.overlay.PlayerCardOverlay
import com.reoky.raidframer.ui.overlay.TrackerOverlay
import com.reoky.raidframer.ui.overlay.BattleGraphOverlay
import com.reoky.raidframer.ui.overlay.ItemUseOverlay
import com.reoky.raidframer.ui.overlay.RaidCallerOverlay
import com.reoky.raidframer.ui.overlay.MetaSpecsOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.GraphicsEnvironment
import java.awt.event.ComponentAdapter

@Composable
fun OverlayContainer(wm: WindowManager) {
  println("Rendering Window Containers...")

  val config by RFConfig.state.collectAsState()

  LaunchedEffect(config.preferredLanguage) {
    AppLocale.apply(config.preferredLanguage)
  }

  OverlayType.entries.forEach { type ->
    val visible = wm.isVisible(type).value
    if (visible) {
      val state by wm.getWindowState(type)
      val everythingVisible by AppState.isEverythingVisible.collectAsState()
      val resizable = remember { mutableStateOf(true) }

      // When the combat tool-tip mode is enabled, treat the Combat window as a tool-tip so it
      // stays visible when tabbed-out, is draggable without shift, and renders like other tool-tips.
      val effectiveWindowType = if (type == OverlayType.COMBAT && config.combatOverlayAsTooltipEnabled) {
        OverlayWindowType.TOOLTIP
      } else {
        state.windowType
      }

      OverlayWindow(
        title = type.name,
        overlayType = type,
        initialPosition = WindowPosition(Dp(state.lastPositionXDp), Dp(state.lastPositionYDp)),
        initialSize = DpSize(Dp(state.lastWidthDp), Dp(state.lastHeightDp)),
        windowType = effectiveWindowType,
        isVisible = wm.visibilityStates[type] ?: mutableStateOf(false),
        isEverythingVisible = if (everythingVisible) mutableStateOf(true) else mutableStateOf(effectiveWindowType == OverlayWindowType.TOOLTIP),
        isResizable = resizable,
        isFocusable = type == OverlayType.NEW_SESSION || type == OverlayType.BATTLE_GRAPH,
        transparentBackground = type == OverlayType.ITEM_USE,
        onCloseRequest = { wm.closeWindow(type) }
      ) { window ->
        val scope = rememberCoroutineScope()

        when (type) {
          OverlayType.ABOUT -> AboutOverlay(wm)
          OverlayType.COMBAT -> CombatOverlay(wm)
          OverlayType.SUMMARY -> SummaryOverlay(wm)
          OverlayType.MINI -> MiniOverlay(wm)
          OverlayType.SETTINGS -> SettingsOverlay(wm)
          OverlayType.HELP -> HelpOverlay(wm)
          OverlayType.COMPANION -> CompanionOverlay(wm)
          OverlayType.POKEMON -> PokemonOverlay(wm)
          OverlayType.TRACKER -> TrackerOverlay(wm)
          OverlayType.NEW_SESSION -> NewSessionOverlay(wm)
          OverlayType.RAID -> RaidOverlay(wm)
          OverlayType.PLAYER_CARD -> PlayerCardOverlay(wm)
          OverlayType.BATTLE_GRAPH -> BattleGraphOverlay(wm)
          OverlayType.ITEM_USE -> ItemUseOverlay()
          OverlayType.RAID_CALLER -> RaidCallerOverlay(wm)
          OverlayType.META_SPECS -> MetaSpecsOverlay(wm)
          else -> {}
        }

        // The below code listens for the window to be moved or resized and notifies
        // the WindowManager to update the stored state accordingly. Debounces so we
        // don't spam updates during drag/resize.
        DisposableEffect(window) {
          var updateJob: Job? = null
          val debounceMs = 600L

          // Compose/AWT may report a transient 0/default geometry while the native window is opening.
          // Do not let that initial value overwrite the restored database state.
          updateJob = scope.launch {
            delay(debounceMs)
            notifyWindowManagerStateChanged(type, wm, window)
          }

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
    val correctedY = moveDownIfAboveVisibleMonitor(window)
    val pos = window.locationOnScreen
    val size = window.size
    if (size.width > 0 && size.height > 0) {
      windowManager.updateWindowState(type) {
        copy(
          lastPositionXDp = pos.x.toFloat(),
          lastPositionYDp = correctedY?.toFloat() ?: pos.y.toFloat(),
          lastWidthDp = size.width.toFloat(),
          lastHeightDp = size.height.toFloat()
        )
      }
    }
    println("Updated window state for $type: pos=(${pos.x}, ${pos.y}), size=(${size.width}, ${size.height})")
  } catch (e: Exception) {
    // window isn't open yet
    // println("Could not get window position/size for $type: ${e.message}")
  }
}

/**
 * Correct windows that are above the top of the monitor they are actually on.
 * A negative Y is valid when a secondary monitor is arranged above the primary one,
 * so the monitor is selected using the window center rather than the primary display.
 */
private fun moveDownIfAboveVisibleMonitor(window: ComposeWindow): Int? {
  val bounds = window.bounds
  if (bounds.width <= 0 || bounds.height <= 0) return null

  val center = java.awt.Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2)
  val monitor = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    .map { it.defaultConfiguration.bounds }
    .firstOrNull { it.contains(center) }
    ?: return null

  if (bounds.y >= monitor.y) return null

  val correctedY = monitor.y + 100
  window.setLocation(bounds.x, correctedY)
  return correctedY
}
