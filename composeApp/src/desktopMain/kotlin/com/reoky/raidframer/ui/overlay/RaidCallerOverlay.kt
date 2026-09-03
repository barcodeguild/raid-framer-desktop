package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.rememberMetaSpecs
import com.reoky.raidframer.core.definitions.lootBuffTotalForIds
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.definitions.parseRaidBuffRequirements
import com.reoky.raidframer.core.definitions.serialize
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.factionHighlightColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helpers.openRaidTab
import com.reoky.raidframer.core.helpers.openSettingsGeneral
import com.reoky.raidframer.core.helpers.openMetaSpecs
import com.reoky.raidframer.core.helpers.openSummaryTab
import com.reoky.raidframer.core.helpers.rememberSectionPulse
import com.reoky.raidframer.core.helpers.togglePokemon
import com.reoky.raidframer.core.interactor.CombatLogInteractor
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.RaidBuffGracePeriod
import com.reoky.raidframer.core.serialization.IPCMessagePayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.capture.GameSnippingService
import com.reoky.raidframer.ui.capture.PocketWindowCaptureCoordinator
import com.reoky.raidframer.ui.capture.ScreenshotPreviewCoordinator
import com.reoky.raidframer.OverlayNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.raid_caller_abort_discard
import raid_framer_desktop.composeapp.generated.resources.raid_caller_avg_gs
import raid_framer_desktop.composeapp.generated.resources.raid_caller_cc
import raid_framer_desktop.composeapp.generated.resources.raid_caller_clump
import raid_framer_desktop.composeapp.generated.resources.raid_caller_co_raid
import raid_framer_desktop.composeapp.generated.resources.raid_caller_coherence
import raid_framer_desktop.composeapp.generated.resources.raid_caller_dancer
import raid_framer_desktop.composeapp.generated.resources.raid_caller_dragon
import raid_framer_desktop.composeapp.generated.resources.raid_caller_healer
import raid_framer_desktop.composeapp.generated.resources.raid_caller_highest_gs
import raid_framer_desktop.composeapp.generated.resources.raid_caller_loot
import raid_framer_desktop.composeapp.generated.resources.raid_caller_lowest_gs
import raid_framer_desktop.composeapp.generated.resources.raid_caller_mage
import raid_framer_desktop.composeapp.generated.resources.raid_caller_main_raid
import raid_framer_desktop.composeapp.generated.resources.raid_caller_melee
import raid_framer_desktop.composeapp.generated.resources.raid_caller_meta
import raid_framer_desktop.composeapp.generated.resources.raid_caller_min_buffed
import raid_framer_desktop.composeapp.generated.resources.raid_caller_minimal
import raid_framer_desktop.composeapp.generated.resources.raid_caller_none
import raid_framer_desktop.composeapp.generated.resources.raid_caller_no_raid
import raid_framer_desktop.composeapp.generated.resources.raid_caller_non_meta
import raid_framer_desktop.composeapp.generated.resources.raid_caller_position_hint
import raid_framer_desktop.composeapp.generated.resources.raid_caller_raid
import raid_framer_desktop.composeapp.generated.resources.raid_caller_show_anyways
import raid_framer_desktop.composeapp.generated.resources.raid_caller_ranged
import raid_framer_desktop.composeapp.generated.resources.raid_caller_rebirth
import raid_framer_desktop.composeapp.generated.resources.raid_caller_render
import raid_framer_desktop.composeapp.generated.resources.raid_caller_riso
import raid_framer_desktop.composeapp.generated.resources.raid_caller_save_stop
import raid_framer_desktop.composeapp.generated.resources.raid_caller_so_tf
import raid_framer_desktop.composeapp.generated.resources.raid_caller_title
import raid_framer_desktop.composeapp.generated.resources.raid_caller_recording
import raid_framer_desktop.composeapp.generated.resources.raid_caller_faction_haranya
import raid_framer_desktop.composeapp.generated.resources.raid_caller_faction_nuia
import raid_framer_desktop.composeapp.generated.resources.raid_caller_faction_pirate
import raid_framer_desktop.composeapp.generated.resources.raid_caller_loot_threshold
import raid_framer_desktop.composeapp.generated.resources.raid_caller_buff_mode
import raid_framer_desktop.composeapp.generated.resources.raid_caller_buff_grace

private const val REBIRTH_BUFF_ID = 2385
private const val FLASH_DURATION_MS = 5_000L
// SummaryOverlay dropdown index for the "Coherence" tab (the last entry in its tab list).
private const val SUMMARY_COHERENCE_TAB_INDEX = 23

// Slightly-brighter section/title text so headers stand out against the faint-grey labels.
private val SectionTitleColor = Color(0xFFD5D5D5)
private val CallerTitleColor = Color(0xFFE8E8E8)

/** Compact pie-slice representation: color to normalized fraction. */
private data class Slice(val color: Color, val fraction: Float)

private val traumaBands = listOf(
  RFColors.gearBlue,
  RFColors.gearGreen,
  RFColors.gearYellow,
  RFColors.gearOrange,
  RFColors.gearRed
)

private fun traumaTextColor(count: Int, total: Int): Color {
  if (count <= 0 || total <= 0) return Color.White
  val ratio = count.toFloat() / total
  val stops = listOf(Color.White, RFColors.gearBlue, RFColors.gearGreen, RFColors.gearYellow, RFColors.gearOrange, RFColors.gearRed)
  val position = (ratio * (stops.lastIndex)).coerceIn(0f, stops.lastIndex.toFloat())
  val lower = position.toInt()
  return if (lower == stops.lastIndex) stops.last() else androidx.compose.ui.graphics.lerp(stops[lower], stops[lower + 1], position - lower)
}

private fun traumaRingSlices(count: Int, total: Int): List<Slice> {
  if (total <= 0) return listOf(Slice(RFColors.gearBlue, 1f))
  val ratio = (count.toFloat() / total).coerceIn(0f, 1f)
  if (ratio <= 0f) return listOf(Slice(RFColors.gearBlue, 1f))

  // Red is truthful: its arc is exactly the fraction of the raid with trauma.
  // The other colors form a compact severity trail beside it, while untouched
  // circumference remains blue. This keeps one afflicted player mostly blue.
  val redFraction = ratio
  val trailFraction = minOf(ratio * 0.8f, 1f - redFraction)
  val blueFraction = 1f - redFraction - trailFraction
  return listOf(
    Slice(RFColors.gearRed, redFraction),
    Slice(RFColors.gearOrange, trailFraction * 0.28f),
    Slice(RFColors.gearYellow, trailFraction * 0.18f),
    Slice(RFColors.gearGreen, trailFraction * 0.22f),
    Slice(RFColors.gearBlue, blueFraction + trailFraction * 0.32f)
  ).filter { it.fraction > 0f }
}

private fun Faction.factionLabelRes() = when (this) {
  Faction.HARANYA -> Res.string.raid_caller_faction_haranya
  Faction.NUIA -> Res.string.raid_caller_faction_nuia
  Faction.PIRATE -> Res.string.raid_caller_faction_pirate
  Faction.UNKNOWN -> null
}

private fun Faction.factionColor(): Color = factionHighlightColor(this)

private fun formatMinutesSeconds(ms: Long): String {
  val totalSeconds = (ms / 1000).coerceAtLeast(0)
  val m = totalSeconds / 60
  val s = totalSeconds % 60
  return if (m >= 60) {
    val h = m / 60
    String.format("%d:%02d:%02d", h, m % 60, s)
  } else {
    String.format("%d:%02d", m, s)
  }
}

@Composable
private fun MiniPieChart(
  slices: List<Slice>,
  size: Dp,
  centerTop: String,
  centerBottom: String,
  centerTopColor: Color = SectionTitleColor,
  centerBottomColor: Color = Color.White,
  centerMiddle: String? = null,
  centerMiddleColor: Color = RFColors.TextSecondary,
  centerMiddleContent: (@Composable () -> Unit)? = null
) {
  Box(
    modifier = Modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(alpha = 0.99f) // Forces hardware acceleration — prevents desktop canvas flicker
    ) {
      val stroke = Stroke(width = size.toPx() * 0.07f, cap = StrokeCap.Butt)
      val inset = stroke.width / 2
      val radius = (size.toPx() / 2) - inset
      // Canvas angles advance clockwise; -90 degrees is 12 o'clock.
      val start = -90f
      var sweep = 0f
      slices.forEach { slice ->
        val arcSweep = slice.fraction.coerceIn(0f, 1f) * 360f
        drawArc(
          color = slice.color,
          startAngle = start + sweep,
          sweepAngle = arcSweep,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
          style = stroke
        )
        sweep += arcSweep
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(centerTop, color = centerTopColor, fontSize = 7.sp, maxLines = 1)
      if (centerMiddleContent != null) {
        centerMiddleContent()
      } else if (centerMiddle != null) {
        Text(centerMiddle, color = centerMiddleColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(centerBottom, color = centerBottomColor, fontSize = 8.sp, maxLines = 1)
    }
  }
}

@Composable
private fun TraumaCount(
  text: String,
  color: Color,
  critical: Boolean
) {
  if (critical) {
    val transition = rememberInfiniteTransition()
    val pulseColor by transition.animateColor(
      initialValue = color,
      targetValue = RFColors.gearRed.copy(alpha = 0.45f),
      animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse)
    )
    Text(text, color = pulseColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
  } else {
    Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
  }
}

@Composable
private fun FlashCount(
  count: Int,
  flashUntil: Long,
  now: Long,
  baseColor: Color,
  flashColor: Color = RFColors.dpsOrange
) {
  val flashing = now < flashUntil
  if (flashing) {
    // Only run the infinite color animation while actively flashing; otherwise the constant
    // invalidation causes visible flicker across the whole overlay.
    val transition = rememberInfiniteTransition()
    val animatedColor by transition.animateColor(
      initialValue = baseColor,
      targetValue = flashColor,
      animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse)
    )
    Text(text = count.toString(), color = animatedColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
  } else {
    Text(text = count.toString(), color = baseColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun SectionTitle(text: String, onClick: (() -> Unit)? = null) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val modifier = if (onClick != null) {
    Modifier
      .hoverable(interactionSource)
      .clip(RoundedCornerShape(4.dp))
      .clickable(interactionSource = interactionSource, indication = null) { onClick() }
  } else {
    Modifier
  }
  Text(
    text,
    color = if (onClick != null && isHovered) Color.White else SectionTitleColor,
    fontSize = 9.sp,
    fontWeight = FontWeight.Bold,
    modifier = modifier
  )
}

@Composable
private fun FactionDamageRow(
  counts: Map<Faction, Int>,
  damages: Map<Faction, Long>,
  flashUntil: Map<Faction, Long>,
  now: Long,
  modifier: Modifier = Modifier
) {
  val factions = Faction.entries.filter { it != Faction.UNKNOWN }
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    factions.forEach { faction ->
      val count = counts[faction] ?: 0
      val damage = damages[faction] ?: 0L
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val labelRes = faction.factionLabelRes()
        if (labelRes != null) {
          Text(stringResource(labelRes) + ":", color = RFColors.TextSecondary, fontSize = 9.sp)
        }
        FlashCount(count, flashUntil[faction] ?: 0L, now, faction.factionColor())
        Text("(${damage.humanReadableAbbreviation()})", color = RFColors.dpsOrange, fontSize = 9.sp)
      }
    }
  }
}

@Composable
private fun LabeledValue(
  label: String,
  value: String,
  valueColor: Color,
  labelColor: Color = RFColors.TextSecondary,
  onClick: (() -> Unit)? = null
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val rowModifier = if (onClick != null) {
    Modifier
      .hoverable(interactionSource)
      .clip(RoundedCornerShape(4.dp))
      .clickable(interactionSource = interactionSource, indication = null) { onClick() }
  } else {
    Modifier
  }
  Row(
    rowModifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Text(
      label,
      color = if (onClick != null && isHovered) Color.White else labelColor,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold
    )
    Text(value, color = valueColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun RaidCallerOverlayContent(wm: WindowManager?, nowTick: Long) {
  val config by RFConfig.state.collectAsState()
  val mainRaid by PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid by PlayerCacheInteractor.getRaidById(1).collectAsState()
  val pets by PlayerCacheInteractor.activePets.collectAsState()
  val players by PlayerCacheInteractor.realPlayers.collectAsState()
  val isRecording by CombatLogInteractor.isRecording.collectAsState()

  // Emulate a smooth count-down for Rebirth Trauma between the ~5s scan intervals.
  var rebirthCache by remember { mutableStateOf<Map<String, Pair<Long, Long>>>(emptyMap()) } // name -> (scanTsMs, timeLeftMs)
  val gracePeriod = RaidBuffGracePeriod.entries.firstOrNull { it.name == config.raidCallerBuffGracePeriod }
    ?: RaidBuffGracePeriod.FIFTEEN_MINUTES
  val liveMembers = remember(mainRaid, coRaid) {
    (mainRaid.flatten() + coRaid.flatten()).filter { it.playerName.isNotBlank() }
  }
  // Cache the last non-empty roster so the overlay doesn't blank out during a session
  // reset (which briefly clears the in-memory raid until the Lua addon re-emits it).
  var lastKnownMembers by remember { mutableStateOf<List<RaidFramePayload>>(emptyList()) }
  LaunchedEffect(liveMembers) {
    if (liveMembers.isNotEmpty()) lastKnownMembers = liveMembers
  }
  val allMembers = if (liveMembers.isNotEmpty()) liveMembers else lastKnownMembers
  val memberNames = remember(allMembers) { allMembers.map { it.playerName }.toSet() }

  // Rebuild the rebirth cache whenever the raid scan produces fresher data. Presence is
  // resolved through the same grace-aware observation used by the other buffs, so a player
  // who walks out of buff-scan range keeps counting down from their last known time-left
  // (and stays counted) for the grace window instead of disappearing from the ring.
  LaunchedEffect(allMembers, gracePeriod) {
    val updated = rebirthCache.toMutableMap()
    allMembers.forEach { member ->
      val scanTs = member.buffScanTimestamp.takeIf { it > 0L }?.times(1000L) ?: return@forEach
      val obs = PlayerCacheInteractor.resolveRaidBuffObservation(member, gracePeriod)
      val hasRebirth = obs.snapshot?.buffIds?.contains(REBIRTH_BUFF_ID) == true
      if (!hasRebirth) {
        updated.remove(member.playerName)
        return@forEach
      }
      // Only a fresh in-range scan carries a real time-left to re-arm the count-down.
      val fresh = member.buffs.firstOrNull { it.buff_id == REBIRTH_BUFF_ID }
      if (fresh != null) {
        val cached = updated[member.playerName]
        if (cached == null || scanTs > cached.first) {
          updated[member.playerName] = scanTs to fresh.tooltip.timeLeft.toLong()
        }
      }
      // Grace hit (no fresh tooltip): keep the existing entry untouched so the timer keeps
      // counting down from the last known value. Nothing to do here.
    }
    rebirthCache = updated
  }

  // Per-member estimated rebirth remaining (ms) at the current tick.
  val rebirthEstimates = remember(rebirthCache, nowTick) {
    val estimates = mutableMapOf<String, Long>()
    rebirthCache.forEach { (name, pair) ->
      val (scanTs, timeLeft) = pair
      val remaining = timeLeft - (nowTick - scanTs)
      // Once the last known duration expires, the observation is no longer
      // actionable. Do not keep counting an out-of-range player as afflicted.
      if (remaining > 0L) estimates[name] = remaining
    }
    estimates
  }

  // --- Gear score (raid members only) ---
  val memberCards = remember(allMembers) {
    allMembers.mapNotNull { member -> PlayerCacheInteractor.getCard(member.playerName) }
  }
  val knownGs = remember(memberCards) { memberCards.map { it.lastKnownGearScore }.filter { it > 0 } }
  val avgGs = if (knownGs.isEmpty()) 0 else knownGs.sum() / knownGs.size
  val lowestGs = knownGs.minOrNull() ?: 0
  val highestGs = knownGs.maxOrNull() ?: 0

  // --- Rebirth trauma pie + average ---
  val rebirthCount = rebirthEstimates.size
  val totalForRebirth = allMembers.size.toFloat().coerceAtLeast(1f)
  val traumaColor = traumaTextColor(rebirthCount, allMembers.size)
  val traumaCritical = allMembers.isNotEmpty() && rebirthCount.toFloat() / allMembers.size > 0.5f
  val rebirthSlices = remember(rebirthCount, allMembers.size) { traumaRingSlices(rebirthCount, allMembers.size) }
  // Average rebirth time across ONLY members who currently have rebirth.
  val rebirthAvg = if (rebirthEstimates.isEmpty()) 0L else rebirthEstimates.values.sum() / rebirthEstimates.size
  val rebirthAvgText = when {
    rebirthCount == 0 -> stringResource(Res.string.raid_caller_none)
    rebirthAvg < 15_000L -> stringResource(Res.string.raid_caller_minimal)
    else -> formatMinutesSeconds(rebirthAvg)
  }

  // --- Loot buffs (threshold from config, 100% in-game baseline) ---
  val lootThreshold = config.raidCallerLootBuffThreshold
  val lootSums = remember(allMembers, gracePeriod) {
    allMembers.map { member ->
      val obs = PlayerCacheInteractor.resolveRaidBuffObservation(member, gracePeriod)
      val ids = obs.snapshot?.buffIds ?: emptySet()
       lootBuffTotalForIds(ids)
    }
  }
  val lootAvg = if (lootSums.isEmpty()) 0 else lootSums.sum() / lootSums.size
  val lootBuffedCount = lootSums.count { it >= lootThreshold }
  val lootTotal = lootSums.size.toFloat().coerceAtLeast(1f)
  val lootSlices = remember(lootBuffedCount, lootTotal) {
    listOf(
      Slice(RFColors.dpsOrange, lootBuffedCount / lootTotal),
      Slice(RFColors.lootBuffColor, (lootSums.size - lootBuffedCount) / lootTotal)
    )
  }

  // --- Min buffed % (requirements from config + grace) ---
  val requirements = remember(config.raidCallerBuffRequirements) {
    parseRaidBuffRequirements(config.raidCallerBuffRequirements)
  }
  val buffModeLabel = remember(requirements) {
    BUFF_PRESETS.firstOrNull { it.requirements.serialize() == requirements.serialize() }?.label ?: "Custom"
  }
  val buffedMembers = remember(allMembers, requirements, gracePeriod) {
    allMembers.filter { member ->
      val obs = PlayerCacheInteractor.resolveRaidBuffObservation(member, gracePeriod)
      val snapshot = obs.snapshot ?: return@filter false
      requirements.matches(member.copy(buffs = snapshot.buffIds.map { id ->
        com.reoky.raidframer.core.serialization.BuffPayload(buff_id = id)
      }), config.raidCallerAllowGuildBuff)
    }
  }
  val minBuffedCount = buffedMembers.size
  val minBuffedTotal = allMembers.size
  val minBuffedTotalF = minBuffedTotal.toFloat().coerceAtLeast(1f)
  val minBuffedSlices = remember(minBuffedCount, minBuffedTotal) {
    listOf(
      Slice(RFColors.healsGreen, minBuffedCount / minBuffedTotalF),
      Slice(RFColors.TextTertiary.copy(alpha = 0.35f), (minBuffedTotal - minBuffedCount) / minBuffedTotalF)
    )
  }

  // --- Has SoTF / War Time (Faction War Time buff) ---
  val soTfIds = setOf(23717, 32025)
  val soTfCount = remember(allMembers, gracePeriod) {
    allMembers.count { member ->
      val obs = PlayerCacheInteractor.resolveRaidBuffObservation(member, gracePeriod)
      obs.snapshot?.buffIds?.any { it in soTfIds } == true
    }
  }
  val soTfPct = if (allMembers.isEmpty()) 0 else soTfCount * 100 / allMembers.size

  // --- Coherence averages ---
  val coherenceRecordingMs = PlayerCacheInteractor.coherenceRecordingMsMs
  fun avgCoherence(extract: (PlayerCard) -> Long): Float {
    if (memberCards.isEmpty() || coherenceRecordingMs <= 0L) return 0f
    val sum = memberCards.sumOf { extract(it) }
    return (sum.toFloat() / (memberCards.size * coherenceRecordingMs)) * 100f
  }
  val coherenceRender = avgCoherence { it.sessionCoherenceRenderMs }
  val coherenceRaid = avgCoherence { it.sessionCoherenceRaidMs }
  val coherenceClump = avgCoherence { it.sessionCoherenceClumpMs }

  // --- Dragon breath / riso missiles by faction ---
  fun factionRiderCounts(petFilter: (com.reoky.raidframer.core.model.PetCard) -> Int): Map<Faction, Int> {
    val result = mutableMapOf<Faction, Int>()
    pets.forEach { pet ->
      val owner = PlayerCacheInteractor.getCard(pet.owner)
      val faction = owner?.let { Faction.fromString(it.lastKnownFaction) } ?: Faction.UNKNOWN
      result[faction] = (result[faction] ?: 0) + petFilter(pet)
    }
    return result
  }
  fun factionRiderDamage(petFilter: (com.reoky.raidframer.core.model.PetCard) -> Long): Map<Faction, Long> {
    val result = mutableMapOf<Faction, Long>()
    pets.forEach { pet ->
      val owner = PlayerCacheInteractor.getCard(pet.owner)
      val faction = owner?.let { Faction.fromString(it.lastKnownFaction) } ?: Faction.UNKNOWN
      result[faction] = (result[faction] ?: 0L) + petFilter(pet)
    }
    return result
  }
  val dragonCounts = factionRiderCounts { it.sessionBreathCasts.size }
  val dragonDamages = factionRiderDamage { it.sessionBreathCasts.sumOf { c -> c.damage } }
  val risoCounts = factionRiderCounts { it.sessionRocketCasts.size }
  val risoDamages = factionRiderDamage { it.sessionRocketCasts.sumOf { c -> c.damage } }

  // --- Dragon / riso flash timestamps (count increase triggers a 5s flash) ---
  var lastDragonCounts by remember { mutableStateOf<Map<Faction, Int>>(emptyMap()) }
  var lastRisoCounts by remember { mutableStateOf<Map<Faction, Int>>(emptyMap()) }
  var dragonFlash by remember { mutableStateOf<Map<Faction, Long>>(emptyMap()) }
  var risoFlash by remember { mutableStateOf<Map<Faction, Long>>(emptyMap()) }
  LaunchedEffect(dragonCounts, risoCounts, nowTick) {
    val newDragonFlash = dragonFlash.toMutableMap()
    dragonCounts.forEach { (faction, count) ->
      if (count > (lastDragonCounts[faction] ?: 0)) newDragonFlash[faction] = nowTick + FLASH_DURATION_MS
    }
    dragonFlash = newDragonFlash
    lastDragonCounts = dragonCounts

    val newRisoFlash = risoFlash.toMutableMap()
    risoCounts.forEach { (faction, count) ->
      if (count > (lastRisoCounts[faction] ?: 0)) newRisoFlash[faction] = nowTick + FLASH_DURATION_MS
    }
    risoFlash = newRisoFlash
    lastRisoCounts = risoCounts
  }

  // --- Meta class breakdown (CC, Healer, Melee, Mage, Dancer, Ranged, Non-Meta) ---
  val specs = remember(players, memberNames) {
    players.filter { it.name in memberNames }.mapNotNull { card -> SpecType.fromName(card.currentBuild) to card }
  }
  val meta = rememberMetaSpecs()
  val metaCc = specs.count { it.first in meta.cc }
  val metaHealer = specs.count { it.first in meta.healer }
  val metaMelee = specs.count { it.first in meta.melee }
  val metaMage = specs.count { it.first in meta.mage }
  val metaDancer = specs.count { it.first in meta.dancer }
  val metaRanged = specs.count { it.first in meta.ranged }
  val metaKnown = meta.cc + meta.melee + meta.healer + meta.mage + meta.dancer + meta.ranged
  val metaNonMeta = specs.count { it.first !in metaKnown }

  // --- Recording duration ---
  val recordingMs = if (isRecording && config.lastSessionStart > 0L) {
    (nowTick - config.lastSessionStart).coerceAtLeast(0L)
  } else if (config.lastSessionDurationMs > 0L) {
    config.lastSessionDurationMs
  } else 0L

  val mainCount = mainRaid.flatten().count { it.playerName.isNotBlank() }
  val coCount = coRaid.flatten().count { it.playerName.isNotBlank() }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 8.dp, vertical = 2.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    // Header row: title + raid counts + recording control pinned to the top-right.
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FlowRow(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically
      ) {
        val titleInteraction = remember { MutableInteractionSource() }
        val titleHovered by titleInteraction.collectIsHoveredAsState()
        Text(
          stringResource(Res.string.raid_caller_title),
          color = if (titleHovered) Color.White else CallerTitleColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .hoverable(titleInteraction)
            .clip(RoundedCornerShape(4.dp))
            .clickable(interactionSource = titleInteraction, indication = null) { openSettingsGeneral(wm) }
        )
        LabeledValue("${stringResource(Res.string.raid_caller_main_raid)}:", mainCount.toString(), RFColors.callerMainRaid, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.ATTENDANCE) })
        LabeledValue("${stringResource(Res.string.raid_caller_co_raid)}:", coCount.toString(), RFColors.callerCoRaid, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.ATTENDANCE) })
        if (isRecording) {
          LabeledValue("${stringResource(Res.string.raid_caller_recording)}:", formatMinutesSeconds(recordingMs), RFColors.AccentRed, SectionTitleColor)
        }
      }
      GameSnippingButton(wm)
      RecordingControlButton(wm, isRecording)
    }

    Divider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)

    // Gear score + SoTF (flow).
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      LabeledValue("${stringResource(Res.string.raid_caller_avg_gs)}:", "$avgGs", RFColors.callerAvgGs, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.NEARBY_GEAR) })
      LabeledValue("${stringResource(Res.string.raid_caller_lowest_gs)}:", "$lowestGs", RFColors.callerLowestGs, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.NEARBY_GEAR) })
      LabeledValue("${stringResource(Res.string.raid_caller_highest_gs)}:", "$highestGs", RFColors.callerHighestGs, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.NEARBY_GEAR) })
      SectionTitle("${stringResource(Res.string.raid_caller_so_tf)}:")
      Text("$soTfCount/${allMembers.size} ($soTfPct%)", color = RFColors.TextTertiary, fontSize = 9.sp)
    }

    // Pie charts row + mode settings stack to the right.
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        MiniPieChart(
          slices = rebirthSlices,
          size = 56.dp,
          centerTop = stringResource(Res.string.raid_caller_rebirth),
          centerMiddleContent = { TraumaCount("$rebirthCount/${allMembers.size}", traumaColor, traumaCritical) },
          centerBottom = rebirthAvgText
        )
        MiniPieChart(
          slices = lootSlices,
          size = 56.dp,
          centerTop = stringResource(Res.string.raid_caller_loot),
          centerMiddle = "$lootBuffedCount/${lootSums.size}",
          centerMiddleColor = Color.White,
          centerBottom = "$lootAvg%",
          centerBottomColor = RFColors.dpsOrange
        )
        MiniPieChart(
          slices = minBuffedSlices,
          size = 56.dp,
          centerTop = stringResource(Res.string.raid_caller_min_buffed),
          centerMiddle = "$minBuffedCount/$minBuffedTotal",
          centerMiddleColor = Color.White,
          centerBottom = "${(minBuffedCount / minBuffedTotalF * 100).toInt()}%",
          centerBottomColor = RFColors.healsGreen
        )
      }
      Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.End
      ) {
        LabeledValue("${stringResource(Res.string.raid_caller_loot_threshold)}:", "$lootThreshold%", RFColors.dpsOrange, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.BUFFS, highlightBuffSelect = true) })
        LabeledValue("${stringResource(Res.string.raid_caller_buff_mode)}:", buffModeLabel, RFColors.callerMetaMage, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.BUFFS, highlightBuffSelect = true) })
        LabeledValue("${stringResource(Res.string.raid_caller_buff_grace)}:", gracePeriod.label(), RFColors.itemSkillYellow, SectionTitleColor, onClick = { openRaidTab(wm, RaidTab.BUFFS, highlightBuffSelect = true) })
      }
    }

    // Dragon + Riso rows (flow so the faction groups wrap instead of clipping).
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionTitle("${stringResource(Res.string.raid_caller_dragon)}:", onClick = { togglePokemon(wm) })
      FactionDamageRow(dragonCounts, dragonDamages, dragonFlash, nowTick, Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionTitle("${stringResource(Res.string.raid_caller_riso)}:", onClick = { togglePokemon(wm) })
      FactionDamageRow(risoCounts, risoDamages, risoFlash, nowTick, Modifier.weight(1f))
    }

    // Coherence (flow).
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      SectionTitle("${stringResource(Res.string.raid_caller_coherence)}:", onClick = { openSummaryTab(wm, SUMMARY_COHERENCE_TAB_INDEX) })
      LabeledValue("${stringResource(Res.string.raid_caller_render)}:", "${coherenceRender.toInt()}%", RFColors.callerCoherenceRender)
      LabeledValue("${stringResource(Res.string.raid_caller_raid)}:", "${coherenceRaid.toInt()}%", RFColors.callerCoherenceRaid)
      LabeledValue("${stringResource(Res.string.raid_caller_clump)}:", "${coherenceClump.toInt()}%", RFColors.callerCoherenceClump)
    }

    // Meta breakdown (flow).
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      SectionTitle("${stringResource(Res.string.raid_caller_meta)}:", onClick = { openRaidTab(wm, RaidTab.COMPOSITION) })
      LabeledValue("${stringResource(Res.string.raid_caller_cc)}:", metaCc.toString(), RFColors.ccCyan)
      LabeledValue("${stringResource(Res.string.raid_caller_healer)}:", metaHealer.toString(), RFColors.healsGreen)
      LabeledValue("${stringResource(Res.string.raid_caller_melee)}:", metaMelee.toString(), RFColors.callerMetaMelee)
      LabeledValue("${stringResource(Res.string.raid_caller_mage)}:", metaMage.toString(), RFColors.callerMetaMage)
      LabeledValue("${stringResource(Res.string.raid_caller_dancer)}:", metaDancer.toString(), RFColors.callerMetaDancer)
      LabeledValue("${stringResource(Res.string.raid_caller_ranged)}:", metaRanged.toString(), RFColors.callerMetaRanged)
      LabeledValue("${stringResource(Res.string.raid_caller_non_meta)}:", metaNonMeta.toString(), RFColors.callerMetaNonMeta)
      val pencilInteraction = androidx.compose.foundation.interaction.MutableInteractionSource()
      val pencilHovered by pencilInteraction.collectIsHoveredAsState()
      Text(
        text = "✎",
        color = if (pencilHovered) Color.White else RFColors.TextSecondary,
        fontSize = 11.sp,
        modifier = Modifier
          .offset(x = (-2).dp, y = (-1).dp)
          .hoverable(pencilInteraction)
          .clip(RoundedCornerShape(4.dp))
          .clickable(interactionSource = pencilInteraction, indication = null) { openMetaSpecs(wm) }
          .padding(horizontal = 0.dp)
      )
    }
  }
}

@Composable
private fun GameSnippingButton(wm: WindowManager?) {
  val scope = rememberCoroutineScope()
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()

  IconButton(
    onClick = {
      scope.launch {
        val image = GameSnippingService.capture(
          windowsToHide = listOfNotNull(wm?.nativeWindow(OverlayType.RAID_CALLER))
        ) ?: return@launch
        // Always persist a copy of the snipped PNG into the month's snippets folder so it's
        // never lost, then open the dedicated preview window for the user to decide what to do.
        val result = PocketWindowCaptureCoordinator.saveSnippet(image) ?: return@launch
        ScreenshotPreviewCoordinator.show(result)
        wm?.openWindow(OverlayType.SCREENSHOT_PREVIEW)
      }
    },
    modifier = Modifier.size(32.dp)
  ) {
    Text(
      text = "\uf03e",
      fontFamily = FontsHelper.faSolid(),
      fontSize = 13.sp,
      color = if (isHovered) RFColors.AccentRed else Color.White,
      modifier = Modifier.hoverable(interactionSource)
    )
  }
}

@Composable
private fun RecordingControlButton(wm: WindowManager?, isRecording: Boolean) {
  if (isRecording) {
    RecordingStopButton()
  } else {
    val interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    IconButton(
      onClick = { wm?.openWindow(OverlayType.NEW_SESSION) },
      modifier = Modifier.size(32.dp)
    ) {
      Text(
        text = "\uf067",
        fontFamily = FontsHelper.faSolid(),
        fontSize = 13.sp,
        color = if (isHovered) RFColors.AccentRed else Color.White,
        modifier = Modifier.hoverable(interactionSource = interactionSource)
      )
    }
  }
}

@Composable
private fun RecordingStopButton() {
  var showStopPopup by remember { mutableStateOf(false) }

  IconButton(
    onClick = { showStopPopup = !showStopPopup },
    modifier = Modifier.size(32.dp)
  ) {
    Text(
      text = "\uf04d",
      fontFamily = FontsHelper.faSolid(),
      fontSize = 13.sp,
      color = RFColors.AccentRed
    )
  }

  if (showStopPopup) {
    Popup(
      alignment = Alignment.TopEnd,
      offset = IntOffset(0, 36),
      onDismissRequest = { showStopPopup = false }
    ) {
      Surface(
        shape = RoundedCornerShape(4.dp),
        elevation = 4.dp,
        color = Color.Black.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.Gray)
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
          verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
          TextButton(
            onClick = {
              showStopPopup = false
              PlayerCacheInteractor.stopSession()
              val currentSessionStart = RFConfig.state.value.lastSessionStart
              RFConfig.update { it.copy(lastSessionStart = 0L, previousSessionStart = currentSessionStart) }
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Text(text = stringResource(Res.string.raid_caller_save_stop), color = Color.White, fontSize = 11.sp)
          }
          TextButton(
            onClick = {
              showStopPopup = false
              PlayerCacheInteractor.abortSession()
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Text(text = stringResource(Res.string.raid_caller_abort_discard), color = RFColors.AccentRed, fontSize = 11.sp)
          }
        }
      }
    }
  }
}

@Composable
fun RaidCallerOverlay(wm: WindowManager? = null) {
  var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      nowTick = System.currentTimeMillis()
      delay(1_000L)
    }
  }

  val mainRaid by PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid by PlayerCacheInteractor.getRaidById(1).collectAsState()

  // Once a raid has been detected, stay in "raid" mode for the lifetime of the overlay. This
  // prevents a brief flash back to the "not in a raid" state when a recording session is
  // stopped/started, which clears the in-memory roster until the Lua addon re-emits it.
  var raidWasDetected by remember { mutableStateOf(false) }
  if (!raidWasDetected && (mainRaid.flatten().isNotEmpty() || coRaid.flatten().isNotEmpty())) {
    raidWasDetected = true
  }
  // When the roster empties (e.g. a session reset cleared it), prompt the Lua addon to re-emit
  // the current raid roster so the overlay repopulates immediately rather than waiting for the
  // next buff scan or combat event.
  LaunchedEffect(mainRaid, coRaid) {
    if (raidWasDetected && mainRaid.flatten().isEmpty() && coRaid.flatten().isEmpty()) {
      delay(600L)
      CompanionInteractor.sendMessage(IPCMessagePayload.TestPing())
    }
  }

  val inRaid = raidWasDetected

  // User can force the caller stats to show even when not in a raid. Mirrors the
  // "stay in raid mode" behavior of raidWasDetected: once opted in, it holds for the
  // lifetime of the overlay window (re-opening the overlay resets it).
  var showAnyways by remember { mutableStateOf(false) }

  // Flash the overlay border when first enabled so the user can find it.
  var raidCallerPulseActive by remember { mutableStateOf(false) }
  LaunchedEffect(OverlayNav.highlightRaidCallerOverlay.value) {
    if (OverlayNav.highlightRaidCallerOverlay.value) {
      raidCallerPulseActive = true
      OverlayNav.highlightRaidCallerOverlay.value = false
    }
  }
  val raidCallerBorder = rememberSectionPulse(raidCallerPulseActive, restColor = Color.Transparent)

  Box(modifier = Modifier.fillMaxSize().border(2.dp, raidCallerBorder, RoundedCornerShape(4.dp))) {
    if (!inRaid && !showAnyways) {
      // When not in a raid, show the positioning helper text (same pattern as ItemUseOverlay).
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(Res.string.raid_caller_no_raid),
          color = Color.White.copy(alpha = 0.6f),
          fontSize = 12.sp,
          fontWeight = FontWeight.Light,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(Res.string.raid_caller_position_hint),
          color = Color.White.copy(alpha = 0.4f),
          fontSize = 10.sp,
          fontWeight = FontWeight.Light,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        val showAnywaysInteraction = remember { MutableInteractionSource() }
        val showAnywaysHovered by showAnywaysInteraction.collectIsHoveredAsState()
        Text(
          text = stringResource(Res.string.raid_caller_show_anyways),
          color = Color.White.copy(alpha = if (showAnywaysHovered) 0.7f else 0.4f),
          fontSize = 10.sp,
          fontWeight = FontWeight.Light,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .hoverable(showAnywaysInteraction)
            .clip(RoundedCornerShape(4.dp))
            .clickable(interactionSource = showAnywaysInteraction, indication = null) { showAnyways = true }
            .padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    } else {
      RaidCallerOverlayContent(wm, nowTick)
    }
  }
}
