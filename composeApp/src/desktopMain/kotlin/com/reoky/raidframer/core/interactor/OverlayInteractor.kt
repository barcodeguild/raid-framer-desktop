package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.config.RFConfig
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

object OverlayInteractor : Interactor() {

  var lastWindowTitle = ""

  /*
   * Tries to programmatically hide overlays when user has tabbed-out of the game if tabbed detection is enabled.
   * Toggles overlay an additional time if user switched windows regardless because of apps like Windows Image Preview
   * that change window flags when they full-screen a preview, breaking some overlays.
   */
  override suspend fun interact() {
    val currentWindowTitle = getWindowTitle()
    if (RFConfig.state.value.tabbedDetectionEnabled) {
      AppState.setEverythingVisible(isGameForegrounded(currentWindowTitle))
    } else {
      AppState.setEverythingVisible(true)
      if (currentWindowTitle != lastWindowTitle && isGameForegrounded(currentWindowTitle)) {
        AppState.setEverythingVisible(false)
        CoroutineScope(Dispatchers.Main).launch {
          delay(5000.milliseconds)
          AppState.setEverythingVisible(true)
          Log.info("OverlayInteractor", "Resetting overlay visibility to true.")
        }
      }
    }
    lastWindowTitle = currentWindowTitle
  }

  /*
   * See if ArcheRage is in the foreground for automatic hiding of overlay windows.
   */
  private fun getWindowTitle(): String {
    val hwnd = User32.INSTANCE.GetForegroundWindow() ?: return ""
    val buffer = Memory(1024 * 2)
    val textLength = User32.INSTANCE.GetWindowTextA(hwnd, buffer, 1024)
    return if (textLength > 0) buffer.getString(0) else ""
  }

  /*
   * Matching based on string comparison only for now. Probably good enough.
   */
  private fun isGameForegrounded(windowTitle: String): Boolean {
    return windowTitle.contains("ArcheRage", ignoreCase = true)
  }

  /*
   * JNA interface for User32.dll functions we need.
   */
  interface User32 : StdCallLibrary {
    fun GetForegroundWindow(): HWND
    fun GetWindowTextA(hWnd: HWND, lpString: Pointer, nMaxCount: Int): Int

    companion object {
      val INSTANCE: User32 = Native.load("user32", User32::class.java)
    }
  }
}