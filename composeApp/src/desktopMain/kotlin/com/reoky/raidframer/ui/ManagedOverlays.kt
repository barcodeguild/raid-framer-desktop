package com.reoky.raidframer.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.reoky.raidframer.core.database.WindowStateEntity

// Separating the overlay management into its own composable to keep main.kt cleaner
// because in v1.5.4 the window management was inline with the main file.
@Composable
fun ManagedOverlays(
  windowManager: WindowManager,
  // Map each overlay to its content composable
  contents: Map<OverlayType, @Composable () -> Unit>
) {
  LaunchedEffect(Unit) {
    windowManager.loadStates()
    windowManager.ensureAtLeastOneWindowOpen()
  }

  // Render every visible overlay
  windowManager.allOverlayTypes().forEach { type ->
    val state = windowManager.getState(type)
    // Drive visibility through a derived mutable to interop with OverlayWindow API
    val visible = rememberSaveable(type) { mutableStateOf(state.value.isVisible) }

    // Sync outward -> inward
    LaunchedEffect(state.value.isVisible) {
      if (visible.value != state.value.isVisible) visible.value = state.value.isVisible
    }
    // Sync inward -> outward (user closes)
    LaunchedEffect(visible.value) {
      if (visible.value != state.value.isVisible) {
        if (visible.value) {
          windowManager.openWindow(type)
        } else {
          windowManager.closeWindow(type)
        }
      }
    }

    if (!visible.value) return@forEach

    val entity: WindowStateEntity = state.value
    OverlayWindow(
      title = titleFor(type),
      initialPosition = androidx.compose.ui.window.WindowPosition(
        x = Dp(entity.lastPositionXDp),
        y = Dp(entity.lastPositionYDp)
      ),
      initialSize = DpSize(
        width = Dp(entity.lastWidthDp),
        height = Dp(entity.lastHeightDp)
      ),
      overlayType = type,
      isObstructing = mutableStateOf(false),
      isVisible = visible,
      isEverythingVisible = mutableStateOf(true),
      isResizable = mutableStateOf(true),
      isFocusable = (type != OverlayType.ABOUT),
      onCloseRequest = {
        visible.value = false
      },
//      onMoved = { xDp, yDp ->
//        // should we debounce this? should use an actual callback signature
//        state.value = state.value.copy(
//          lastPositionXDp = xDp.value,
//          lastPositionYDp = yDp.value
//        )
//      },
//      onResized = { wDp, hDp ->
//        state.value = state.value.copy(
//          lastWidthDp = wDp.value,
//          lastHeightDp = hDp.value
//        )
//      }
    ) {
      contents[type]?.invoke()
    }
  }
}

private fun titleFor(type: OverlayType): String =
  ".: Raid Framer ${type.name.lowercase().replaceFirstChar { it.titlecase() }} :."