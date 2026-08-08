package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.ItemSpell
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.ui.OverlayHoverState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class ItemUseEvent(
  val timestamp: Long,
  val sourceName: String,
  val sourceFaction: Faction,
  val itemSpell: ItemSpell,
  val targetName: String,
  val targetFaction: Faction,
)

@Composable
fun ItemUseOverlay() {
  val maxVisible = 8
  val fadeOutDurationMs = 10_000L
  val entryLifetimeMs = 60_000L

  var trackedEvents by remember { mutableStateOf<List<ItemUseEvent>>(emptyList()) }
  var knownKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
  var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }

  val config by RFConfig.state.collectAsState()
  val playerFaction = remember(config.playerFaction) {
    Faction.fromString(config.playerFaction)
  }

  // Poll for new item use events from all real players
  LaunchedEffect(Unit) {
    while (true) {
      nowTick = System.currentTimeMillis()
      val players = PlayerCacheInteractor.getRealPlayersSnapshot()
      val newEvents = mutableListOf<ItemUseEvent>()

      players.forEach { card ->
        card.recentSkillItemUsages.forEach { (timestamp, nameRes, target) ->
          val key = "${card.name}|${timestamp}|${nameRes}|${target}"
          if (key !in knownKeys) {
            val itemSpell = ItemSpell.entries.find { it.friendlyNameRes == nameRes }
            if (itemSpell != null) {
              val sourceFaction = Faction.fromString(card.lastKnownFaction)
              val targetCard = players.find { it.name == target }
              val targetFaction = targetCard?.let { Faction.fromString(it.lastKnownFaction) } ?: Faction.UNKNOWN
              newEvents.add(ItemUseEvent(timestamp, card.name, sourceFaction, itemSpell, target, targetFaction))
            }
          }
        }
      }

      if (newEvents.isNotEmpty()) {
        val merged = (trackedEvents + newEvents).sortedByDescending { it.timestamp }.take(maxVisible * 2)
        trackedEvents = merged
        knownKeys = merged.map { "${it.sourceName}|${it.timestamp}|${it.itemSpell.friendlyNameRes}|${it.targetName}" }.toSet()
      }

      // Prune expired events
      trackedEvents = trackedEvents.filter { nowTick - it.timestamp < entryLifetimeMs }

      delay(1_000L)
    }
  }

  val visibleEvents = trackedEvents
    .filter { nowTick - it.timestamp < entryLifetimeMs }
    .take(maxVisible)
    .reversed()

  // Use shared hover state from OverlayHoverState
  val isHovered by OverlayHoverState.isAnyOverlayHovered.collectAsState()

  val backgroundAlpha by animateFloatAsState(
    targetValue = if (isHovered) 0.42f else 0f,
    animationSpec = tween(durationMillis = 220)
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = backgroundAlpha))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.End
    ) {
      visibleEvents.forEach { event ->
        val ageMs = nowTick - event.timestamp
        val fadeProgress = if (ageMs > entryLifetimeMs - fadeOutDurationMs) {
          (entryLifetimeMs - ageMs).toFloat() / fadeOutDurationMs.toFloat()
        } else {
          1f
        }.coerceIn(0f, 1f)

        val animatedAlpha by animateFloatAsState(
          targetValue = fadeProgress,
          animationSpec = tween(durationMillis = 300)
        )

        val itemName = stringResource(event.itemSpell.friendlyNameRes)
        val sourceColor = playerFaction.getFactionHighlightColor(event.sourceFaction)
          .takeUnless { it == Color.Transparent } ?: Color.White
        val targetColor = playerFaction.getFactionHighlightColor(event.targetFaction)
          .takeUnless { it == Color.Transparent } ?: Color.White

        Row(
          modifier = Modifier
            .alpha(animatedAlpha)
            .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(2.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = event.sourceName,
            color = sourceColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Image(
              painter = painterResource(event.itemSpell.iconRes),
              contentDescription = itemName,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = itemName,
              color = RFColors.itemSkillYellow,
              fontSize = 9.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = event.targetName,
            color = targetColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
          )
        }
      }
    }

    if (isHovered) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Item Usage Feed",
          color = Color.White.copy(alpha = 0.6f),
          fontSize = 13.sp,
          fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Shift+Click+Drag to Position",
          color = Color.White.copy(alpha = 0.4f),
          fontSize = 11.sp,
          fontWeight = FontWeight.Light
        )
      }
    }
  }
}
