package com.reoky.raidframer.ui

import com.reoky.raidframer.core.database.WindowStateEntity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.reoky.raidframer.core.database.WindowStateDao
import com.reoky.raidframer.core.interactor.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Window

class WindowManager(
  private val scope: CoroutineScope,
  private val dao: WindowStateDao? = null // Optional
) {

  companion object {
    private const val TAG = "WindowManager"

    /** Overlay types that should always start closed, even if their saved state was open. */
    private val ALWAYS_START_CLOSED = setOf(
      OverlayType.PLAYER_CARD,
    )
  }

  // Holds actual state for each window
  private val windowStates: MutableMap<OverlayType, MutableState<WindowStateEntity>> = mutableMapOf()
  private val saveMutexes: MutableMap<OverlayType, Mutex> = mutableMapOf()

  // Visibility flags tracked per window type
  val visibilityStates: MutableMap<OverlayType, MutableState<Boolean>> = mutableMapOf()
  private val nativeWindows: MutableMap<OverlayType, Window> = mutableMapOf()

  fun registerNativeWindow(type: OverlayType, window: Window) {
    nativeWindows[type] = window
  }

  fun nativeWindow(type: OverlayType): Window? = nativeWindows[type]

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
    // Sync the entity so persisting captures the correct visibility
    windowStates[type]?.value?.let { entity ->
      windowStates[type]?.value = entity.copy(isVisible = true)
    }
  }

  // Closes the overlay window and saves its last known state
  fun closeWindow(type: OverlayType) {
    val visibility = visibilityStates[type] ?: return
    visibility.value = false
    // Sync the entity so persisting captures the correct visibility
    windowStates[type]?.value?.let { entity ->
      windowStates[type]?.value = entity.copy(isVisible = false)
    }

    // Save state
    windowStates[type]?.value?.let { saveState(it) }
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
            windowType = entity.windowType,
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
      visibilityStates[type]?.value = entity.isVisible
    }

    // Force always-start-closed windows shut (geometry is preserved, only visibility overridden).
    ALWAYS_START_CLOSED.forEach { type ->
      windowStates[type]?.value?.let { entity ->
        windowStates[type]?.value = entity.copy(isVisible = false)
      }
      visibilityStates[type]?.value = false
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

    saveState(newState)
  }

  private fun saveState(state: WindowStateEntity) {
    val type = OverlayType.valueOf(state.overlayType)
    val mutex = saveMutexes.getOrPut(type) { Mutex() }
    scope.launch {
      mutex.withLock {
        // Read the latest in-memory value after waiting for any prior write.
        // This prevents an older geometry event from overwriting a newer one.
        val latestState = windowStates[type]?.value ?: state
        dao?.insert(latestState)
      }
    }
  }

  /*
   * Used when the user freaks out and doesn't know where their windows went. Ensures they are all back to default positions and foregrounded.
   */
  fun resetAllWindowPositions() {
    scope.launch {
      // Remember which windows were open so we can restore them (a reset should only move
      // windows, not change whether any specific overlay is open).
      val wasVisible = OverlayType.entries.associateWith { type ->
        visibilityStates[type]?.value == true
      }

      // Close all windows first so they actually dispose and can be re-created with the
      // default geometry. Without the delay the visibility writes would coalesce into a
      // single recomposition and the native windows would keep their stale positions.
      OverlayType.entries.forEach { type ->
        visibilityStates[type]?.value = false
      }

      delay(50)

      // Reset each window to its default state
      OverlayType.entries.forEach { type ->
        val defaultState = defaultWindowStateForTypeFor(type).copy(isVisible = wasVisible[type] == true)
        windowStates[type]?.value = defaultState

        // Restore whatever was open before the reset (default state doesn't drive visibility)
        visibilityStates[type]?.value = wasVisible[type] == true

        // Persist to database
        dao?.insert(defaultState)
      }

      Log.info(TAG, "Reset all window positions to defaults upon user request.")
    }
  }

}
