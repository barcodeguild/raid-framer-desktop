package com.reoky.raidframer.ui.capture

import java.awt.event.KeyEvent
import java.awt.KeyboardFocusManager
import java.awt.KeyEventDispatcher

/** Installs a temporary global Ctrl/Cmd+C listener only while a preview exists. */
object ScreenshotCopyHotkey {
  private var dispatcher: KeyEventDispatcher? = null

  @Synchronized
  fun start(onCopy: () -> Unit) {
    if (dispatcher != null) return
    val installed = KeyEventDispatcher { event ->
      if (event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_C &&
        (event.isControlDown || event.isMetaDown)
      ) {
        onCopy()
        true
      } else false
    }
    dispatcher = installed
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(installed)
  }

  @Synchronized
  fun stop() {
    dispatcher?.let {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(it)
      dispatcher = null
    }
  }
}
