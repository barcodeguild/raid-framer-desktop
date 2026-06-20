package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.config.RFConfig
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary

object OverlayInteractor : Interactor() {

  /*
   * Tries to programmatically manage overlay windows so that they can be hidden when the game is tabbed-out.
   */
  override suspend fun interact() {
    if (RFConfig.state.value.tabbedDetectionEnabled) {
      AppState.setEverythingVisible(isGameForegrounded())
    } else {
      AppState.setEverythingVisible(true)
    }
  }

  /*
   * See if ArcheRage is in the foreground for automatic hiding of overlay windows.
   */
  private fun isGameForegrounded(): Boolean {
    val hwnd = User32.INSTANCE.GetForegroundWindow() ?: return false
    val buffer = Memory(1024 * 2)
    val textLength = User32.INSTANCE.GetWindowTextA(hwnd, buffer, 1024)
    if (textLength > 0) {
      return buffer.getString(0).contains("ArcheRage")
    }
    return false
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