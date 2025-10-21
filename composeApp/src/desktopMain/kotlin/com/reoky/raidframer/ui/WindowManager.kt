package com.reoky.raidframer.ui

import com.reoky.raidframer.core.database.WindowStateEntity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.reoky.raidframer.core.database.WindowStateDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WindowManager(
  private val scope: CoroutineScope,
  private val dao: WindowStateDao? = null // Optional
) {
  // Holds actual state for each window
  private val windowStates: MutableMap<OverlayType, MutableState<WindowStateEntity>> = mutableMapOf()

  // Visibility flags tracked per window type
  val visibilityStates: MutableMap<OverlayType, MutableState<Boolean>> = mutableMapOf()

  // So we don't accidentally implement this same logic anywhere else except the defaults
  fun defaultVisibility(type: OverlayType): MutableState<Boolean> {
    return mutableStateOf(defaultWindowStateForTypeFor(type).isVisible)
  }

  // Public getter for Compose to read from - return the stored MutableState (create if missing)
  fun isVisible(type: OverlayType): MutableState<Boolean> =
    visibilityStates.getOrPut(type) { mutableStateOf(false) }

  // Opens the overlay window
  fun openWindow(type: OverlayType) {
    val visibility = visibilityStates.getOrPut(type) { mutableStateOf(false) }
    visibility.value = true
  }

  // Closes the overlay window and saves its last known state
  fun closeWindow(type: OverlayType) {
    val visibility = visibilityStates[type] ?: return
    visibility.value = false

    // Save state
    scope.launch {
      windowStates[type]?.value?.let { state ->
        dao?.insert(state)
      }
    }
  }

  // Call this on app startup
  suspend fun loadStates() {

    // pre-populate the default states for every window in case there's nothing in the database
    OverlayType.entries.forEach { type ->
      windowStates.getOrPut(type) {
        val defaultState = defaultWindowStateForTypeFor(type)
        visibilityStates.getOrPut(type) { defaultVisibility(type) }
        println("Pre-populating $type window state to be: $defaultState")
        mutableStateOf(defaultState)
      }
    }

    // go into the database and get any previous window states
    dao?.getAll()?.forEach { entity ->
      val type = OverlayType.valueOf(entity.overlayType)
      val existingWindowState = windowStates.getOrPut(type) {
        mutableStateOf(
          WindowStateEntity(
            overlayType = type.name,
            lastPositionXDp = entity.lastPositionXDp,
            lastPositionYDp = entity.lastPositionYDp,
            lastWidthDp = entity.lastWidthDp,
            lastHeightDp = entity.lastHeightDp,
            isVisible = entity.isVisible // summary always visible so the app doesn't close
          )
        )
      }

      // replace pre-populated values with the one from the database
      existingWindowState.value = entity
      visibilityStates.getOrPut(type) { mutableStateOf(type == OverlayType.COMBAT) }

      // debug
      println("Using previous window state for $type." )
    }
  }

  /*
   * Returns the window state for the given overlay type, creating a default one if none exists. (Uses the defaults in OverlayWindow.kt)
   */
  fun getWindowState(type: OverlayType): MutableState<WindowStateEntity> {
    return windowStates.getOrPut(type) {
      mutableStateOf(defaultWindowStateForTypeFor(type))
    }
  }

  fun updateWindowState(
    type: OverlayType,
    update: WindowStateEntity.() -> WindowStateEntity
  ) {
    val state = getWindowState(type)
    val newState = state.value.update()
    windowStates[type]?.value = newState

    scope.launch {
      dao?.insert(newState)

      // debug
      println("Updated window state for $type: $newState")
    }
  }
}
