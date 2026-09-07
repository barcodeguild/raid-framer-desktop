package com.reoky.raidframer.ui.component

/**
 * One-shot navigation hints from other overlays into the Pocket Journal / Editor.
 * Set before opening the target overlay; the target consumes (and clears) it on open.
 */
object PocketNav {
  @Volatile var journalQuery: String? = null
}
