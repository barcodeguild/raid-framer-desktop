package com.reoky.raidframer.core.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Returns a pulsing "golden sheen" border color while [active] is true, fading out over
 * [durationSec] seconds. Mirrors the flash used to draw attention to the Check-for-Updates
 * section in SettingsOverlay, standardized here so any section in any overlay can reuse it.
 *
 * @param active when true the pulse runs; when false (or after [durationSec]) returns [RFColors.CardBorder].
 * @param durationSec how long the highlight animation lasts.
 * @param highlightColor the base color to pulse.
 */
@Composable
fun rememberSectionPulse(
  active: Boolean,
  durationSec: Float = 7f,
  highlightColor: Color = RFColors.UpdateGold
): Color {
  var elapsed by remember(active) { mutableStateOf(0f) }
  LaunchedEffect(active) {
    if (active) {
      val start = System.nanoTime()
      while (true) {
        delay(50)
        elapsed = (System.nanoTime() - start) / 1_000_000_000f
        if (elapsed >= durationSec) break
      }
    }
  }
  return if (active && elapsed < durationSec) {
    val cycle = (elapsed % 1.5f) / 1.5f
    val pulse = (sin(cycle * Math.PI.toFloat()) * 0.3f + 0.35f).coerceIn(0f, 1f)
    highlightColor.copy(alpha = pulse)
  } else RFColors.CardBorder
}