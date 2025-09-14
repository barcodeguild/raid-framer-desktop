package com.reoky.raidframer.ui

import com.reoky.raidframer.core.database.WindowStateEntity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.reoky.raidframer.core.database.WindowStateDao

/*
 * Class used to control the visibility and position of overlay windows. Needs to keep at least one window open at all times.
 * Also tries to save and restore the last position and size of each window.
 */
class WindowManager(private val dao: WindowStateDao) {
  private val windowStates = mutableMapOf<OverlayType, MutableState<WindowStateEntity>>()

  private val isAnyWindowOpen: Boolean
    get() = windowStates.values.any { it.value.isVisible }

  suspend fun loadStates() {
    if (windowStates.isNotEmpty()) return
    dao.getAll().forEach { entity ->
      windowStates[OverlayType.valueOf(entity.overlayType)] = mutableStateOf(entity)
    }
  }

  fun getState(type: OverlayType): MutableState<WindowStateEntity> =
    windowStates.getOrPut(type) {
      mutableStateOf(
        WindowStateEntity(
          overlayType = type.name,
          lastPositionXDp = 0f,
            lastPositionYDp = 0f,
          lastWidthDp = 600f,
          lastHeightDp = 750f,
          isVisible = (type == OverlayType.ABOUT) // initial fallback
        )
      )
    }

  suspend fun updateState(
    type: OverlayType,
    update: WindowStateEntity.() -> WindowStateEntity
  ) {
    val state = getState(type)
    state.value = state.value.update()
    dao.insert(state.value)
  }

  fun openWindow(type: OverlayType) {
    val s = getState(type)
    if (!s.value.isVisible) {
      s.value = s.value.copy(isVisible = true)
    }
  }

  fun closeWindow(type: OverlayType) {
    val s = getState(type)
    if (s.value.isVisible) {
      s.value = s.value.copy(isVisible = false)
      ensureAtLeastOneWindowOpen()
    }
  }

  fun ensureAtLeastOneWindowOpen() {
    if (!isAnyWindowOpen) {
      openWindow(OverlayType.ABOUT) // switch to combat when that's working again
    }
  }

  fun allOverlayTypes(): Array<OverlayType> = OverlayType.values()
}