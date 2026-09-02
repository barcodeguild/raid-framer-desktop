package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.ui.LocalDragLock

/**
 * Holds the state for all nearby-player filters used on the Nearby, Nearby Gear, and Composition tabs.
 * Persisted via ConfigEntity so it survives app restarts.
 */
data class NearbyFilterState(
  val slidingWindowMinutes: Int = 15,   // 0 = disabled, 1..60
  val participationStage: Int = 1,      // 0=disabled, 1=25k, 2=50k, 3=100k
  val behaviorSensitivity: Int = 0,     // 0=disabled, 1..25
)

/**
 * The four participation stage labels for display in the UI.
 */
val PARTICIPATION_STAGE_LABELS = listOf(
  "Disabled",
  "25k DMG/Heals or 25 CC",
  "50k DMG/Heals or 50 CC",
  "100k DMG/Heals or 100 CC",
)

/**
 * Applies the [NearbyFilterState] to a list of nearby players.
 * This is the single shared filter function used by all three tabs.
 */
fun filterNearbyPlayers(
  players: List<PlayerCard>,
  filterState: NearbyFilterState
): List<PlayerCard> {
  val now = System.currentTimeMillis()
  val windowMs = filterState.slidingWindowMinutes * 60_000L
  return players.filter { card ->
    (filterState.slidingWindowMinutes == 0 || (now - card.lastEvent) <= windowMs) &&
        when (filterState.participationStage) {
          0 -> true
          1 -> card.sessionDamageTotal >= 25_000L || card.sessionHealTotal >= 25_000L || card.sessionCCTotal >= 25
          2 -> card.sessionDamageTotal >= 50_000L || card.sessionHealTotal >= 50_000L || card.sessionCCTotal >= 50
          3 -> card.sessionDamageTotal >= 100_000L || card.sessionHealTotal >= 100_000L || card.sessionCCTotal >= 100
          else -> true
        } &&
        (filterState.behaviorSensitivity == 0 || card.realPlayerBehaviorMetric >= filterState.behaviorSensitivity.toLong())
  }
}

/**
 * A Material3 slider that updates in real-time via [onValueChange],
 * and toggles [LocalDragLock] during drag to prevent window movement.
 */
@Composable
private fun FilterSlider(
  value: Int,
  onValueChange: (Int) -> Unit,
  valueRange: IntRange,
  modifier: Modifier = Modifier
) {
  val dragLock = LocalDragLock.current
  val interactionSource = remember { MutableInteractionSource() }

  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      when (interaction) {
        is DragInteraction.Start -> dragLock.value = true
        is DragInteraction.Stop, is DragInteraction.Cancel -> dragLock.value = false
      }
    }
  }

  Slider(
    value = value.toFloat(),
    onValueChange = { onValueChange(it.toInt().coerceIn(valueRange.first, valueRange.last)) },
    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
    interactionSource = interactionSource,
    modifier = modifier,
    colors = SliderDefaults.colors(
      thumbColor = Color(0xFFDC143C),
      activeTrackColor = Color(0xFFDC143C),
      inactiveTrackColor = Color(0xFF2A2A2A)
    )
  )
}

/**
 * Well-styled container that matches the SettingsSection card pattern.
 */
@Composable
private fun FilterWell(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(RFColors.CardBackground)
      .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    content = content
  )
}

/**
 * Standardized filter controls for the Nearby, Nearby Gear, and Composition tabs.
 * Encapsulates the sliding window, participation, and behavior sensitivity filters.
 */
@Composable
fun NearbyFilterControls(
  state: NearbyFilterState,
  onStateChange: (NearbyFilterState) -> Unit,
  modifier: Modifier = Modifier,
  textColor: Color = Color.White,
) {
  FilterWell(modifier = modifier) {
    // --- Sliding Window ---
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Seen Within", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
          text = if (state.slidingWindowMinutes == 0) "Disabled" else "${state.slidingWindowMinutes} min",
          color = RFColors.TextSecondary,
          fontSize = 12.sp
        )
      }
      FilterSlider(
        value = state.slidingWindowMinutes,
        onValueChange = { onStateChange(state.copy(slidingWindowMinutes = it)) },
        valueRange = 0..60,
        modifier = Modifier.fillMaxWidth()
      )
    }

    // --- Participation Stage ---
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("PvP Participation", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        PARTICIPATION_STAGE_LABELS.forEachIndexed { index, label ->
          val isSelected = state.participationStage == index
          Box(
            modifier = Modifier
              .weight(1f)
              .padding(vertical = 2.dp)
              .clip(RoundedCornerShape(6.dp))
              .clickable { onStateChange(state.copy(participationStage = index)) },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              color = if (isSelected) Color.White else Color.LightGray,
              fontSize = 10.sp,
              modifier = Modifier
                .background(
                  if (isSelected) Color(0xFFDC143C).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
                  RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
              maxLines = 1
            )
          }
        }
      }
    }

    // --- Behavior Sensitivity ---
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Player-Only Behavior", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
          text = if (state.behaviorSensitivity == 0) "Disabled" else "${state.behaviorSensitivity}",
          color = RFColors.TextSecondary,
          fontSize = 12.sp
        )
      }
      FilterSlider(
        value = state.behaviorSensitivity,
        onValueChange = { onStateChange(state.copy(behaviorSensitivity = it)) },
        valueRange = 0..25,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
