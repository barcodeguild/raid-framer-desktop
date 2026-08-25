package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.hoverable
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
import com.reoky.raidframer.core.definitions.META_CC_SPECS
import com.reoky.raidframer.core.definitions.META_DANCER_SPECS
import com.reoky.raidframer.core.definitions.META_HEALER_SPECS
import com.reoky.raidframer.core.definitions.META_MAGE_SPECS
import com.reoky.raidframer.core.definitions.META_MELEE_SPECS
import com.reoky.raidframer.core.definitions.META_RANGED_SPEC
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.lootBuffAmountForIds
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.definitions.parseRaidBuffRequirements
import com.reoky.raidframer.core.definitions.serialize
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.factionHighlightColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
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
import kotlinx.coroutines.delay
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

// Slightly-brighter section/title text so headers stand out against the faint-grey labels.
private val SectionTitleColor = Color(0xFFD5D5D5)
private val CallerTitleColor = Color(0xFFE8E8E8)

/** Compact pie-slice representation: color to normalized fraction. */
private data class Slice(val color: Color, val fraction: Float)

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
  centerMiddleColor: Color = RFColors.TextSecondary
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
      if (centerMiddle != null) {
        Text(centerMiddle, color = centerMiddleColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
      }
      Text(centerBottom, color = centerBottomColor, fontSize = 8.sp, maxLines = 1)
    }
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
private fun SectionTitle(text: String) {
  Text(text, color = SectionTitleColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
  labelColor: Color = RFColors.TextSecondary
) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(label, color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

  // Rebuild the rebirth cache whenever the raid scan produces fresher data.
  LaunchedEffect(allMembers) {
    val updated = rebirthCache.toMutableMap()
    allMembers.forEach { member ->
      val scanTs = member.buffScanTimestamp.takeIf { it > 0L }?.times(1000L) ?: return@forEach
      val rebirth = member.buffs.firstOrNull { it.buff_id == REBIRTH_BUFF_ID }
      if (rebirth == null) {
        updated.remove(member.playerName)
      } else {
        val cached = updated[member.playerName]
        if (cached == null || scanTs > cached.first) {
          updated[member.playerName] = scanTs to rebirth.tooltip.timeLeft.toLong()
        }
      }
    }
    rebirthCache = updated
  }

  // Per-member estimated rebirth remaining (ms) at the current tick.
  val rebirthEstimates = remember(rebirthCache, nowTick) {
    val estimates = mutableMapOf<String, Long>()
    rebirthCache.forEach { (name, pair) ->
      val (scanTs, timeLeft) = pair
      estimates[name] = (timeLeft - (nowTick - scanTs)).coerceAtLeast(0L)
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
  // The ring shows the ratio of raid members WITH rebirth; the average is computed only
  // across members who actually have rebirth (not diluted by unbuffed members as zeroes).
  val rebirthCount = rebirthEstimates.size
  val noneCount = (allMembers.size - rebirthCount).coerceAtLeast(0)
  val shortCount = rebirthEstimates.values.count { it in 15_000L until 45_000L }
  val mediumCount = rebirthEstimates.values.count { it in 45_000L until 90_000L }
  val highCount = rebirthEstimates.values.count { it in 90_000L until 120_000L }
  val criticalCount = rebirthEstimates.values.count { it >= 120_000L }
  // Remember the slices keyed on the band counts so the donut only redraws when a member
  // crosses a threshold (or enters/exits rebirth), not on every second's tick — this avoids
  // the continuous Canvas redraw that presented as flicker.
  val totalForRebirth = allMembers.size.toFloat().coerceAtLeast(1f)
  val rebirthSlices = remember(rebirthCount, noneCount, shortCount, mediumCount, highCount, criticalCount) {
    listOf(
      Slice(RFColors.traumaNone, noneCount / totalForRebirth),
      Slice(RFColors.traumaShort, shortCount / totalForRebirth),
      Slice(RFColors.traumaMedium, mediumCount / totalForRebirth),
      Slice(RFColors.traumaHigh, highCount / totalForRebirth),
      Slice(RFColors.traumaCritical, criticalCount / totalForRebirth)
    )
  }
  // Average rebirth time across ONLY members who currently have rebirth.
  val rebirthAvg = if (rebirthEstimates.isEmpty()) 0L else rebirthEstimates.values.sum() / rebirthEstimates.size
  val rebirthAvgText = when {
    rebirthCount == 0 -> stringResource(Res.string.raid_caller_none)
    rebirthAvg < 15_000L -> stringResource(Res.string.raid_caller_minimal)
    else -> formatMinutesSeconds(rebirthAvg)
  }

  // --- Loot buffs (threshold from config, 100% in-game baseline) ---
  val lootThreshold = config.raidCallerLootBuffThreshold
  val gracePeriod = RaidBuffGracePeriod.entries.firstOrNull { it.name == config.raidCallerBuffGracePeriod }
    ?: RaidBuffGracePeriod.FIFTEEN_MINUTES
  val lootSums = remember(allMembers, gracePeriod) {
    allMembers.map { member ->
      val obs = PlayerCacheInteractor.resolveRaidBuffObservation(member, gracePeriod)
      val ids = obs.snapshot?.buffIds ?: emptySet()
      100 + lootBuffAmountForIds(ids)
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
      }))
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
  val metaCc = specs.count { it.first in META_CC_SPECS }
  val metaHealer = specs.count { it.first in META_HEALER_SPECS }
  val metaMelee = specs.count { it.first in META_MELEE_SPECS }
  val metaMage = specs.count { it.first in META_MAGE_SPECS }
  val metaDancer = specs.count { it.first in META_DANCER_SPECS }
  val metaRanged = specs.count { it.first in META_RANGED_SPEC }
  val metaKnown = META_CC_SPECS + META_MELEE_SPECS + META_HEALER_SPECS + META_MAGE_SPECS + META_DANCER_SPECS + META_RANGED_SPEC
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
        Text(
          stringResource(Res.string.raid_caller_title),
          color = CallerTitleColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        LabeledValue("${stringResource(Res.string.raid_caller_main_raid)}:", mainCount.toString(), RFColors.callerMainRaid, SectionTitleColor)
        LabeledValue("${stringResource(Res.string.raid_caller_co_raid)}:", coCount.toString(), RFColors.callerCoRaid, SectionTitleColor)
        if (isRecording) {
          LabeledValue("${stringResource(Res.string.raid_caller_recording)}:", formatMinutesSeconds(recordingMs), RFColors.AccentRed, SectionTitleColor)
        }
      }
      RecordingControlButton(wm, isRecording)
    }

    Divider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)

    // Gear score + SoTF (flow).
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      LabeledValue("${stringResource(Res.string.raid_caller_avg_gs)}:", "$avgGs", RFColors.callerAvgGs, SectionTitleColor)
      LabeledValue("${stringResource(Res.string.raid_caller_lowest_gs)}:", "$lowestGs", RFColors.callerLowestGs, SectionTitleColor)
      LabeledValue("${stringResource(Res.string.raid_caller_highest_gs)}:", "$highestGs", RFColors.callerHighestGs, SectionTitleColor)
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
          centerMiddle = "$rebirthCount/${allMembers.size}",
          centerMiddleColor = if (rebirthAvg >= 120_000L) RFColors.traumaCritical else if (rebirthAvg >= 90_000L) RFColors.traumaHigh else if (rebirthAvg >= 45_000L) RFColors.traumaMedium else Color.White,
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
        LabeledValue("${stringResource(Res.string.raid_caller_loot_threshold)}:", "$lootThreshold%", RFColors.dpsOrange, SectionTitleColor)
        LabeledValue("${stringResource(Res.string.raid_caller_buff_mode)}:", buffModeLabel, RFColors.callerMetaMage, SectionTitleColor)
        LabeledValue("${stringResource(Res.string.raid_caller_buff_grace)}:", gracePeriod.label(), RFColors.itemSkillYellow, SectionTitleColor)
      }
    }

    // Dragon + Riso rows (flow so the faction groups wrap instead of clipping).
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionTitle("${stringResource(Res.string.raid_caller_dragon)}:")
      FactionDamageRow(dragonCounts, dragonDamages, dragonFlash, nowTick, Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionTitle("${stringResource(Res.string.raid_caller_riso)}:")
      FactionDamageRow(risoCounts, risoDamages, risoFlash, nowTick, Modifier.weight(1f))
    }

    // Coherence (flow).
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      SectionTitle("${stringResource(Res.string.raid_caller_coherence)}:")
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
      SectionTitle("${stringResource(Res.string.raid_caller_meta)}:")
      LabeledValue("${stringResource(Res.string.raid_caller_cc)}:", metaCc.toString(), RFColors.ccCyan)
      LabeledValue("${stringResource(Res.string.raid_caller_healer)}:", metaHealer.toString(), RFColors.healsGreen)
      LabeledValue("${stringResource(Res.string.raid_caller_melee)}:", metaMelee.toString(), RFColors.callerMetaMelee)
      LabeledValue("${stringResource(Res.string.raid_caller_mage)}:", metaMage.toString(), RFColors.callerMetaMage)
      LabeledValue("${stringResource(Res.string.raid_caller_dancer)}:", metaDancer.toString(), RFColors.callerMetaDancer)
      LabeledValue("${stringResource(Res.string.raid_caller_ranged)}:", metaRanged.toString(), RFColors.callerMetaRanged)
      LabeledValue("${stringResource(Res.string.raid_caller_non_meta)}:", metaNonMeta.toString(), RFColors.callerMetaNonMeta)
    }
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

  Box(modifier = Modifier.fillMaxSize()) {
    if (!inRaid) {
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
      }
    } else {
      RaidCallerOverlayContent(wm, nowTick)
    }
  }
}
