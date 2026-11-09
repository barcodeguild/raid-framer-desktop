package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.Surface
import com.reoky.raidframer.ui.component.DragLockedSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.ConfigEntity
import com.reoky.raidframer.ui.component.PerformanceDisabledBanner
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.definitions.RaidBuffKey
import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.definitions.RAID_BUFF_DEFINITIONS
import com.reoky.raidframer.core.definitions.RaidBuffSection
import com.reoky.raidframer.core.definitions.lootBuffTotalForIds
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.definitions.matchesResolved
import com.reoky.raidframer.core.definitions.parseRaidBuffRequirements
import com.reoky.raidframer.core.definitions.serialize
import com.reoky.raidframer.core.model.hasPvPParticipation
import com.reoky.raidframer.core.model.RaidBuffGracePeriod
import com.reoky.raidframer.core.model.RaidBuffObservation
import com.reoky.raidframer.core.definitions.SKILL_TREE_DISPLAY_ORDER
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.MetaSpecs
import com.reoky.raidframer.core.definitions.rememberMetaSpecs
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.factionHighlightColor
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.helpers.rememberSectionPulse
import com.reoky.raidframer.core.helpers.timeAgo
import com.reoky.raidframer.core.helpers.resolveLocalizedString
import com.reoky.raidframer.core.helpers.openMetaSpecs
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.OverlayNav
import com.reoky.raidframer.core.serialization.BuffPayload
import com.reoky.raidframer.core.serialization.IPCMessagePayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.titleBarCaptureActions
import com.reoky.raidframer.ui.component.CheckBoxComponent
import com.reoky.raidframer.ui.component.NearbyFilterControls
import com.reoky.raidframer.ui.component.NearbyFilterState
import com.reoky.raidframer.ui.component.filterNearbyPlayers
import com.reoky.raidframer.ui.component.CompositionChartComponent
import com.reoky.raidframer.ui.component.CompositionBreakdown
import com.reoky.raidframer.ui.component.CompositionBreakdownListComponent
import com.reoky.raidframer.ui.component.FactionComposition
import com.reoky.raidframer.ui.component.GearScoreHistogram
import com.reoky.raidframer.ui.component.GearFactionSeries
import com.reoky.raidframer.ui.component.OverlaidGearScoreChart
import com.reoky.raidframer.ui.component.RaidComponent
import com.reoky.raidframer.ui.component.SelectableTextField
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.BuffPlayerDetailPane
import com.reoky.raidframer.ui.component.buffDetailPopupPositionProvider
import com.reoky.raidframer.ui.component.localizedBuffLabels
import com.reoky.raidframer.ui.component.graphs.BuffHistoryGraph
import com.reoky.raidframer.ui.component.graphs.BuffHistoryMode
import com.reoky.raidframer.ui.component.graphs.BuffHistoryPlayerStatus
import com.reoky.raidframer.ui.component.graphs.BuffHistorySnapshotCard
import com.reoky.raidframer.ui.component.graphs.computeBuffHistoryDataPoints
import com.reoky.raidframer.ui.component.graphs.computeSnapshotCards
import com.reoky.raidframer.ui.component.graphs.formatBuffHistoryTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.nearby_avg_gs_format
import raid_framer_desktop.composeapp.generated.resources.raid_attendance_title
import raid_framer_desktop.composeapp.generated.resources.raid_close
import raid_framer_desktop.composeapp.generated.resources.raid_copy_attendance
import raid_framer_desktop.composeapp.generated.resources.raid_coraid_label
import raid_framer_desktop.composeapp.generated.resources.raid_haranya_faction
import raid_framer_desktop.composeapp.generated.resources.raid_include_coraid
import raid_framer_desktop.composeapp.generated.resources.raid_include_departed
import raid_framer_desktop.composeapp.generated.resources.raid_include_main_raid
import raid_framer_desktop.composeapp.generated.resources.raid_include_nearby_opposite_faction
import raid_framer_desktop.composeapp.generated.resources.raid_include_nearby_same_faction
import raid_framer_desktop.composeapp.generated.resources.raid_main_raid_label
import raid_framer_desktop.composeapp.generated.resources.raid_nearby_disclaimer
import raid_framer_desktop.composeapp.generated.resources.raid_nearby_require_pvp
import raid_framer_desktop.composeapp.generated.resources.raid_no_raid_detected
import raid_framer_desktop.composeapp.generated.resources.raid_nuian_faction
import raid_framer_desktop.composeapp.generated.resources.raid_pirate_faction
import raid_framer_desktop.composeapp.generated.resources.raid_refresh_button
import raid_framer_desktop.composeapp.generated.resources.raid_require_pvp_filter
import raid_framer_desktop.composeapp.generated.resources.raid_tab_attendance
import raid_framer_desktop.composeapp.generated.resources.raid_tab_nearby
import raid_framer_desktop.composeapp.generated.resources.raid_tab_nearby_gear
import raid_framer_desktop.composeapp.generated.resources.raid_tab_composition
import raid_framer_desktop.composeapp.generated.resources.raid_tab_buff_history
import raid_framer_desktop.composeapp.generated.resources.raid_composition_require_pvp
import raid_framer_desktop.composeapp.generated.resources.*
import raid_framer_desktop.composeapp.generated.resources.raid_control_deck_subtitle
import raid_framer_desktop.composeapp.generated.resources.raid_control_deck_title
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

enum class RaidTab { ATTENDANCE, BUFFS, BUFF_HISTORY, NEARBY, NEARBY_GEAR, COMPOSITION }

@Composable
fun RaidOverlay(wm: WindowManager? = null, window: ComposeWindow? = null) {
  val playerFaction = Faction.fromString(RFConfig.state.collectAsState().value.playerFaction)
  val mainRaid = PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid = PlayerCacheInteractor.getRaidById(1).collectAsState()
  val nearbyNuia = PlayerCacheInteractor.nearbyNuianRaidParties.collectAsState()
  val nearbyHaranya = PlayerCacheInteractor.nearbyHaraniRaidParties.collectAsState()
  val nearbyPirate = PlayerCacheInteractor.nearbyPirateRaidParties.collectAsState()
  val raidDepartures = PlayerCacheInteractor.raidDeparturesFlow.collectAsState()
  val raidMemberNames = PlayerCacheInteractor.combinedRaidMemberNames.collectAsState()
  var selectedTab by remember { mutableStateOf(RaidTab.ATTENDANCE) }
  // Consume any cross-overlay tab request (e.g. from the Raid Caller overlay) and select it.
  LaunchedEffect(OverlayNav.pendingRaidTab.value) {
    OverlayNav.pendingRaidTab.value?.let { requested ->
      selectedTab = requested
      OverlayNav.pendingRaidTab.value = null
    }
  }
  // Flash the buff-selection pane when a raid-caller setting points the user here.
  var buffSelectPulseActive by remember { mutableStateOf(false) }
  LaunchedEffect(OverlayNav.highlightRaidBuffSelect.value) {
    if (OverlayNav.highlightRaidBuffSelect.value) {
      buffSelectPulseActive = true
      OverlayNav.highlightRaidBuffSelect.value = false
    }
  }
  val buffSelectBorder = rememberSectionPulse(buffSelectPulseActive)
  val config by RFConfig.state.collectAsState()
  var filterState by rememberSaveable {
    mutableStateOf(
      NearbyFilterState(
        slidingWindowMinutes = config.nearbySlidingWindowMinutes,
        participationStage = config.nearbyParticipationStage,
        behaviorSensitivity = config.nearbyBehaviorSensitivity,
        showInRaidOnly = config.nearbyShowInRaidOnly,
      )
    )
  }
  LaunchedEffect(filterState) {
    RFConfig.update {
      it.copy(
        nearbySlidingWindowMinutes = filterState.slidingWindowMinutes,
        nearbyParticipationStage = filterState.participationStage,
        nearbyBehaviorSensitivity = filterState.behaviorSensitivity,
        nearbyShowInRaidOnly = filterState.showInRaidOnly,
      )
    }
  }
  var raidWasDetected by remember { mutableStateOf(false) }
  var showFilterMenu by remember { mutableStateOf(false) }
  if (!raidWasDetected && (mainRaid.value.isNotEmpty() || coRaid.value.isNotEmpty())) {
    raidWasDetected = true
  }
  LaunchedEffect(Unit) {
    delay(1500L.milliseconds)
    raidWasDetected = true
  }
  // Every time the Raid overlay opens, ask the Lua companion to re-emit the current raid
  // roster over IPC. The Lua side replies to a TEST_PING with a fresh FRAMES_UPDATE, which
  // repopulates the roster even when no combat events are firing in game.
  LaunchedEffect(Unit) {
    CompanionInteractor.sendMessage(IPCMessagePayload.TestPing())
  }
  Box(modifier = Modifier.fillMaxSize().background(Color(0xCC121212))) {
    if (mainRaid.value.isEmpty() && coRaid.value.isEmpty() && !raidWasDetected) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        CircularProgressIndicator(
          color = Color.White.copy(alpha = 0.7f),
          strokeWidth = 2.dp,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = stringResource(Res.string.raid_join_raid_hint),
          color = Color.White.copy(alpha = 0.6f),
          fontSize = 12.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 24.dp)
        )
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        TitleBarComponent(
          title = stringResource(Res.string.raid_attendance_title),
          onClose = { wm?.closeWindow(OverlayType.RAID) },
          rightActions = {
            Box {
              val filterInteractionSource = remember { MutableInteractionSource() }
              val isFilterHovered by filterInteractionSource.collectIsHoveredAsState()
              IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                Text(
                  text = "\uf337", // fa-sliders
                  fontFamily = FontsHelper.faSolid(),
                  fontSize = 16.sp,
                  color = if (isFilterHovered) RFColors.AccentRed else Color.White,
                  modifier = Modifier.hoverable(filterInteractionSource)
                )
              }
              if (showFilterMenu) {
                Popup(
                  alignment = Alignment.TopEnd,
                  offset = IntOffset(0, 50),
                  onDismissRequest = { showFilterMenu = false }
                ) {
                  Box(
                    modifier = Modifier
                      .width(350.dp)
                      .background(Color(0xFF141414).copy(alpha = 0.97f), RoundedCornerShape(10.dp))
                      .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                      .padding(12.dp)
                  ) {
                    NearbyFilterControls(
                      state = filterState,
                      onStateChange = { filterState = it },
                      textColor = Color.White
                    )
                  }
                }
              }
            }
          },
          captureActions = if (window != null && wm != null) titleBarCaptureActions(
            window, wm, stringResource(Res.string.raid_attendance_title)
          ) else null
        )
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF141414).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
              .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            TabRow(
              selectedTabIndex = selectedTab.ordinal,
              backgroundColor = Color.Transparent,
              contentColor = Color.White,
              divider = {},
              indicator = {},
              modifier = Modifier.fillMaxWidth()
            ) {
              val tabs = listOf(
                RaidTab.ATTENDANCE to Res.string.raid_tab_attendance,
                RaidTab.BUFFS to Res.string.raid_tab_buffs,
                RaidTab.BUFF_HISTORY to Res.string.raid_tab_buff_history,
                RaidTab.NEARBY to Res.string.raid_tab_nearby,
                RaidTab.NEARBY_GEAR to Res.string.raid_tab_nearby_gear,
                RaidTab.COMPOSITION to Res.string.raid_tab_composition
              )
              tabs.forEach { (tab, label) ->
                Tab(
                  selected = selectedTab == tab,
                  onClick = { selectedTab = tab },
                  text = {
                    Text(
                      text = stringResource(label),
                      color = if (selectedTab == tab) Color.White else Color.LightGray
                    )
                  }
                )
              }
            }
          }
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.TopStart
        ) {
          when (selectedTab) {
            RaidTab.ATTENDANCE -> AttendanceTab(
              mainRaid = mainRaid.value,
              coRaid = coRaid.value,
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              playerFaction = playerFaction,
              raidDepartures = raidDepartures.value,
              requirePvPParticipation = filterState.participationStage > 0,
              onRequirePvPParticipationChange = {
                filterState = filterState.copy(participationStage = if (it) 1 else 0)
              }
            )

            RaidTab.BUFFS -> BuffsTab(mainRaid.value, coRaid.value, buffSelectBorder)
            RaidTab.BUFF_HISTORY -> BuffHistoryTab(mainRaid.value, coRaid.value)
            RaidTab.NEARBY -> NearbyTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              playerFaction = playerFaction,
              filterState = filterState,
              raidMemberNames = raidMemberNames.value
            )

            RaidTab.NEARBY_GEAR -> NearbyGearTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              playerFaction = playerFaction,
              filterState = filterState,
              raidMemberNames = raidMemberNames.value
            )

            RaidTab.COMPOSITION -> CompositionTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              playerFaction = playerFaction,
              filterState = filterState,
              raidMemberNames = raidMemberNames.value,
              wm = wm
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompositionTab(
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  playerFaction: Faction = Faction.UNKNOWN,
  filterState: NearbyFilterState,
  raidMemberNames: Set<String>,
  wm: WindowManager? = null,
) {
  val inRaidOnly = filterState.showInRaidOnly
  val inRaidIndicator = stringResource(Res.string.nearby_in_raid_only_indicator)
  fun filterFaction(players: List<PlayerCard>, faction: Faction): List<PlayerCard> {
    val filtered = if (inRaidOnly && faction == playerFaction) {
      players.filter { it.name in raidMemberNames }
    } else {
      players
    }
    return filterNearbyPlayers(filtered, filterState)
  }
  val filteredHaranya = filterFaction(nearbyHaranya, Faction.HARANYA)
  val filteredNuia = filterFaction(nearbyNuia, Faction.NUIA)
  val filteredPirate = filterFaction(nearbyPirate, Faction.PIRATE)
  fun factionLabelWithIndicator(label: String, faction: Faction): String {
    if (inRaidOnly && faction == playerFaction) return "$label $inRaidIndicator"
    return label
  }

  val haranyaFormat = stringResource(Res.string.raid_haranya_faction)
  val nuiaFormat = stringResource(Res.string.raid_nuian_faction)
  val pirateFormat = stringResource(Res.string.raid_pirate_faction)
  val haranyaLabel = haranyaFormat.substringBefore("(").trimEnd()
  val nuiaLabel = nuiaFormat.substringBefore("(").trimEnd()
  val pirateLabel = pirateFormat.substringBefore("(").trimEnd()
  fun chart(label: String, players: List<PlayerCard>, color: Color, faction: Faction): FactionComposition {
    val displayLabel = factionLabelWithIndicator(label, faction)
    val counts = mutableMapOf<SkillTreeType, Int>()
    players.forEach { card ->
      SpecType.fromName(card.currentBuild)?.trees?.forEach { tree -> counts[tree] = (counts[tree] ?: 0) + 1 }
    }
    val chartColor =
      playerFaction.getFactionHighlightColor(faction).takeUnless { it == Color.Transparent } ?: color
    return FactionComposition(displayLabel, players.size, counts, chartColor)
  }

  val charts = listOf(
    chart(haranyaLabel, filteredHaranya, factionHighlightColor(Faction.HARANYA), Faction.HARANYA),
    chart(nuiaLabel, filteredNuia, factionHighlightColor(Faction.NUIA), Faction.NUIA),
    chart(pirateLabel, filteredPirate, factionHighlightColor(Faction.PIRATE), Faction.PIRATE)
  )
  val factionPlayers = listOf(
    charts[0].factionLabel to filteredHaranya,
    charts[1].factionLabel to filteredNuia,
    charts[2].factionLabel to filteredPirate
  )
  val labels = SKILL_TREE_DISPLAY_ORDER.associateWith { tree ->
    stringResource(
      when (tree) {
        SkillTreeType.ARCHERY -> Res.string.skill_tree_archery
        SkillTreeType.AURAMANCY -> Res.string.skill_tree_auramancy
        SkillTreeType.BATTLERAGE -> Res.string.skill_tree_battlerage
        SkillTreeType.DEFENSE -> Res.string.skill_tree_defense
        SkillTreeType.GUNSLINGER -> Res.string.skill_tree_gunslinger
        SkillTreeType.MALEDICTION -> Res.string.skill_tree_malediction
        SkillTreeType.OCCULTISM -> Res.string.skill_tree_occultism
        SkillTreeType.SHADOWPLAY -> Res.string.skill_tree_shadowplay
        SkillTreeType.SONGCRAFT -> Res.string.skill_tree_songcraft
        SkillTreeType.SORCERY -> Res.string.skill_tree_sorcery
        SkillTreeType.SPELLDANCE -> Res.string.skill_tree_spelldance
        SkillTreeType.SWIFTBLADE -> Res.string.skill_tree_swiftblade
        SkillTreeType.VITALISM -> Res.string.skill_tree_vitalism
        SkillTreeType.WITCHCRAFT -> Res.string.skill_tree_witchcraft
      }
    )
  }
  Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val chartGap = 10.dp
      if (maxWidth >= 1040.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(chartGap)) {
          charts.forEach { chart ->
            CompositionChartComponent(chart, labels, Modifier.weight(1f))
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(chartGap)) {
          charts.forEach { chart ->
            CompositionChartComponent(chart, labels, Modifier.fillMaxWidth())
          }
        }
      }
    }
    ResponsiveFactionSections(
      factionPlayers,
      stringResource(Res.string.raid_composition_statistics),
      onEditMetaSpecs = { openMetaSpecs(wm) }) { faction, players ->
      FactionStatistics(faction, players)
    }
    ResponsiveFactionSections(
      factionPlayers,
      stringResource(Res.string.raid_composition_meta_spec_breakdown),
      onEditMetaSpecs = { openMetaSpecs(wm) }) { faction, players ->
      MetaSpecBreakdown(faction, players)
    }
  }
}

@Composable
private fun ResponsiveFactionSections(
  factions: List<Pair<String, List<PlayerCard>>>,
  title: String,
  onEditMetaSpecs: () -> Unit = {},
  content: @Composable (String, List<PlayerCard>) -> Unit
) {
  val metaSpecBreakdownTitle = stringResource(Res.string.raid_composition_meta_spec_breakdown)

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.padding(start = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(title, color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      if (title == metaSpecBreakdownTitle) {
        MetaSpecHelp()
        Text(
          text = "✎",
          color = RFColors.TextSecondary,
          fontSize = 14.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onEditMetaSpecs() }
            .padding(horizontal = 0.dp)
        )
      }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      if (maxWidth >= 1040.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          factions.forEach { (faction, players) ->
            Column(Modifier.weight(1f)) { content(faction, players) }
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          factions.forEach { (faction, players) -> content(faction, players) }
        }
      }
    }
  }
}

@Composable
private fun MetaSpecHelp() {
  var showHelp by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { showHelp = !showHelp }, modifier = Modifier.size(28.dp)) {
      Text("?", color = RFColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
    if (showHelp) {
      Popup {
        Surface(
          color = RFColors.PopupBackground.copy(alpha = 0.98f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, RFColors.CardBorder),
          elevation = 6.dp
        ) {
          Column(
            Modifier.padding(10.dp).widthIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "Meta Specs",
                color = RFColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              IconButton(
                onClick = { showHelp = false },
                modifier = Modifier.size(24.dp)
              ) {
                Text("X", color = RFColors.TextSecondary, fontSize = 11.sp)
              }
            }
            val meta = rememberMetaSpecs()
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_cc), meta.cc, MetaSpecs.STOCK.cc)
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_melee), meta.melee, MetaSpecs.STOCK.melee)
            MetaSpecHelpRow(
              stringResource(Res.string.raid_composition_meta_healer),
              meta.healer,
              MetaSpecs.STOCK.healer
            )
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_mage), meta.mage, MetaSpecs.STOCK.mage)
            MetaSpecHelpRow(
              stringResource(Res.string.raid_composition_meta_dancer),
              meta.dancer,
              MetaSpecs.STOCK.dancer
            )
            MetaSpecHelpRow(
              stringResource(Res.string.raid_composition_meta_ranged),
              meta.ranged,
              MetaSpecs.STOCK.ranged
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetaSpecHelpRow(label: String, specs: Set<SpecType>, stock: Set<SpecType>) {
  val annotatedSpecs = specs
    .sortedBy { it.name }
    .sortedBy { it !in stock }
    .map { spec ->
      val custom = spec !in stock
      buildAnnotatedString {
        if (custom) {
          withStyle(SpanStyle(color = RFColors.TestCaption, fontWeight = FontWeight.Bold)) {
            append("+${stringResource(spec.localizedDisplayNameRes)}")
          }
        } else {
          withStyle(SpanStyle(color = RFColors.TextSecondary)) {
            append(stringResource(spec.localizedDisplayNameRes))
          }
        }
      }
    }

  val combinedSpecs = buildAnnotatedString {
    annotatedSpecs.forEachIndexed { index, spec ->
      if (index > 0) append(", ")
      append(spec)
    }
  }

  val hasCustomSpecs = specs.any { it !in stock }
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(label, color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
      if (hasCustomSpecs) {
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "(customized)",
          color = RFColors.UpdateGreen,
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
    Text(
      combinedSpecs,
      color = RFColors.TextSecondary,
      fontSize = 10.sp,
      lineHeight = 12.sp
    )
  }
}

@Composable
private fun FactionStatistics(faction: String, players: List<PlayerCard>) {
  val meta = rememberMetaSpecs()
  val specs = players.mapNotNull { SpecType.fromName(it.currentBuild) }
  fun has(tree: SkillTreeType, spec: SpecType) = tree in spec.trees
  fun matching(predicate: (SpecType) -> Boolean) =
    players.filter { SpecType.fromName(it.currentBuild)?.let(predicate) == true }

  fun row(label: String, yes: Int, total: Int) = CompositionBreakdown(label, yes)
  val battlerage = specs.filter { SkillTreeType.BATTLERAGE in it.trees }
  val dps = specs.filter {
    it.trees.any { tree ->
      tree in setOf(
        SkillTreeType.ARCHERY,
        SkillTreeType.BATTLERAGE,
        SkillTreeType.GUNSLINGER,
        SkillTreeType.MALEDICTION,
        SkillTreeType.SORCERY,
        SkillTreeType.SWIFTBLADE
      )
    }
  }
  val vitalism = specs.filter { SkillTreeType.VITALISM in it.trees }
  val dancer = specs.filter { SkillTreeType.SPELLDANCE in it.trees }
  val shadowplayVitalism = matching { has(SkillTreeType.SHADOWPLAY, it) && has(SkillTreeType.VITALISM, it) }
  val shadowplayWithoutVitalism = matching { has(SkillTreeType.SHADOWPLAY, it) && !has(SkillTreeType.VITALISM, it) }
  val rowPlayers = mutableMapOf(
    stringResource(Res.string.raid_composition_shadowplay_vitalism) to shadowplayVitalism,
    stringResource(Res.string.raid_composition_shadowplay_without_vitalism) to shadowplayWithoutVitalism
  )

  fun addRow(label: String, matchingPlayers: List<PlayerCard>) {
    rowPlayers[label] = matchingPlayers
  }

  val battlerageOccultismOrWitchcraft = players.filter { card ->
    SpecType.fromName(card.currentBuild)?.let {
      has(SkillTreeType.BATTLERAGE, it) && (has(SkillTreeType.OCCULTISM, it) || has(
        SkillTreeType.WITCHCRAFT,
        it
      ))
    } == true
  }
  val battlerageWithoutEither = players.filter { card ->
    SpecType.fromName(card.currentBuild)?.let {
      has(SkillTreeType.BATTLERAGE, it) && !has(SkillTreeType.OCCULTISM, it) && !has(
        SkillTreeType.WITCHCRAFT,
        it
      )
    } == true
  }
  val battlerageOccultism = players.filter { card ->
    SpecType.fromName(card.currentBuild)
      ?.let { has(SkillTreeType.BATTLERAGE, it) && has(SkillTreeType.OCCULTISM, it) } == true
  }
  val battlerageWitchcraft = players.filter { card ->
    SpecType.fromName(card.currentBuild)
      ?.let { has(SkillTreeType.BATTLERAGE, it) && has(SkillTreeType.WITCHCRAFT, it) } == true
  }
  val dpsAuramancy = players.filter { card ->
    SpecType.fromName(card.currentBuild)?.let { it in dps && has(SkillTreeType.AURAMANCY, it) } == true
  }
  val dpsWithoutAuramancy = players.filter { card ->
    SpecType.fromName(card.currentBuild)?.let { it in dps && !has(SkillTreeType.AURAMANCY, it) } == true
  }
  addRow(stringResource(Res.string.raid_composition_battlerage_occultism_witchcraft), battlerageOccultismOrWitchcraft)
  addRow(stringResource(Res.string.raid_composition_battlerage_without_either), battlerageWithoutEither)
  addRow(stringResource(Res.string.raid_composition_battlerage_occultism), battlerageOccultism)
  addRow(stringResource(Res.string.raid_composition_battlerage_witchcraft), battlerageWitchcraft)
  addRow(stringResource(Res.string.raid_composition_dps_auramancy), dpsAuramancy)
  addRow(stringResource(Res.string.raid_composition_dps_without_auramancy), dpsWithoutAuramancy)
  addRow(stringResource(Res.string.raid_composition_vitalism_confessor), matching { it == SpecType.CONFESSOR })
  addRow(stringResource(Res.string.raid_composition_vitalism_assassin), matching { it == SpecType.ASSASSIN })
  addRow(stringResource(Res.string.raid_composition_vitalism_soothsayer), matching { it == SpecType.SOOTHSAYER })
  addRow(
    stringResource(Res.string.raid_composition_vitalism_other),
    matching { has(SkillTreeType.VITALISM, it) && it != SpecType.CONFESSOR && it != SpecType.ASSASSIN })
  addRow(stringResource(Res.string.raid_composition_dancer_comedian), matching { it == SpecType.COMEDIAN })
  addRow(stringResource(Res.string.raid_composition_dancer_seal_resolver), matching { it == SpecType.SEAL_RESOLVER })
  addRow(stringResource(Res.string.raid_composition_dancer_tough_dancer), matching { it == SpecType.TOUGH_DANCER })
  addRow(
    stringResource(Res.string.raid_composition_dancer_other),
    matching { has(SkillTreeType.SPELLDANCE, it) && it !in meta.dancer })
  val breakdownItems = listOf(
    row(stringResource(Res.string.raid_composition_shadowplay_vitalism), shadowplayVitalism.size, players.size),
    row(
      stringResource(Res.string.raid_composition_shadowplay_without_vitalism),
      shadowplayWithoutVitalism.size,
      players.size
    ),
    row(
      stringResource(Res.string.raid_composition_battlerage_occultism_witchcraft),
      battlerageOccultismOrWitchcraft.size,
      battlerage.size
    ),
    row(
      stringResource(Res.string.raid_composition_battlerage_without_either),
      battlerageWithoutEither.size,
      battlerage.size
    ),
    row(stringResource(Res.string.raid_composition_battlerage_occultism), battlerageOccultism.size, battlerage.size),
    row(stringResource(Res.string.raid_composition_battlerage_witchcraft), battlerageWitchcraft.size, battlerage.size),
    row(stringResource(Res.string.raid_composition_dps_auramancy), dpsAuramancy.size, dps.size),
    row(stringResource(Res.string.raid_composition_dps_without_auramancy), dpsWithoutAuramancy.size, dps.size),
    row(
      stringResource(Res.string.raid_composition_vitalism_confessor),
      vitalism.count { it == SpecType.CONFESSOR },
      vitalism.size
    ),
    row(
      stringResource(Res.string.raid_composition_vitalism_assassin),
      vitalism.count { it == SpecType.ASSASSIN },
      vitalism.size
    ),
    row(
      stringResource(Res.string.raid_composition_vitalism_soothsayer),
      vitalism.count { it == SpecType.SOOTHSAYER },
      vitalism.size
    ),
    row(
      stringResource(Res.string.raid_composition_vitalism_other),
      vitalism.count { it != SpecType.CONFESSOR && it != SpecType.ASSASSIN },
      vitalism.size
    ),
    row(
      stringResource(Res.string.raid_composition_dancer_comedian),
      dancer.count { it == SpecType.COMEDIAN },
      dancer.size
    ),
    row(
      stringResource(Res.string.raid_composition_dancer_seal_resolver),
      dancer.count { it == SpecType.SEAL_RESOLVER },
      dancer.size
    ),
    row(
      stringResource(Res.string.raid_composition_dancer_tough_dancer),
      dancer.count { it == SpecType.TOUGH_DANCER },
      dancer.size
    ),
    row(stringResource(Res.string.raid_composition_dancer_other), dancer.count { it !in meta.dancer }, dancer.size)
  )
  Surface(
    color = RFColors.CardBackground.copy(alpha = 0.78f),
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, RFColors.CardBorder),
    modifier = Modifier.fillMaxWidth().padding(12.dp)
  ) {
    CompositionBreakdownListComponent(
      title = faction,
      total = players.size,
      items = breakdownItems,
      itemPlayers = rowPlayers,
      headingColor = factionHeadingColor(faction),
      rowHoverColor = factionHeadingColor(faction),
      sortByCount = false
    )
  }
}

@Composable
private fun MetaSpecBreakdown(faction: String, players: List<PlayerCard>) {
  val meta = rememberMetaSpecs()
  val specs = players.mapNotNull { card -> SpecType.fromName(card.currentBuild)?.let { it to card } }
  val groups = listOf(
    stringResource(Res.string.raid_composition_meta_cc) to meta.cc,
    stringResource(Res.string.raid_composition_meta_melee) to meta.melee,
    stringResource(Res.string.raid_composition_meta_healer) to meta.healer,
    stringResource(Res.string.raid_composition_meta_mage) to meta.mage,
    stringResource(Res.string.raid_composition_meta_dancer) to meta.dancer,
    stringResource(Res.string.raid_composition_meta_ranged) to meta.ranged
  )
  val known = groups.flatMap { it.second }.toSet()
  val other = specs.filter { it.first !in known }
  val otherExamples = other.map { it.first.name.lowercase().replace('_', ' ') }
    .ifEmpty { listOf(stringResource(Res.string.raid_composition_none)) }
    .joinToString(", ")
  val itemPlayers = groups.associate { (name, set) ->
    name to specs.filter { it.first in set }.map { it.second }
  } + (stringResource(Res.string.raid_composition_other) to other.map { it.second })
  Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
    Surface(
      color = RFColors.CardBackground.copy(alpha = 0.78f),
      shape = RoundedCornerShape(8.dp),
      border = BorderStroke(1.dp, RFColors.CardBorder),
      modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
      CompositionBreakdownListComponent(
        title = faction,
        total = players.size,
        items = groups.map { (name, set) -> CompositionBreakdown(name, specs.count { it.first in set }) } +
            CompositionBreakdown(stringResource(Res.string.raid_composition_other), other.size),
        itemPlayers = itemPlayers,
        headingColor = factionHeadingColor(faction),
        rowHoverColor = factionHeadingColor(faction)
      )
    }
    Text(
      text = stringResource(Res.string.raid_composition_other_examples, otherExamples),
      color = RFColors.TextTertiary,
      fontWeight = FontWeight.Medium,
      fontSize = 9.sp,
      lineHeight = 11.sp
    )
  }
}

private fun factionHeadingColor(faction: String): Color {
  val target = when {
    faction.contains("Haranya", ignoreCase = true) -> Faction.HARANYA
    faction.contains("Nuia", ignoreCase = true) -> Faction.NUIA
    faction.contains("Pirate", ignoreCase = true) -> Faction.PIRATE
    else -> Faction.UNKNOWN
  }
  return Faction.fromString(RFConfig.state.value.playerFaction).getFactionHighlightColor(target)
    .takeUnless { it == Color.Transparent } ?: RFColors.TextPrimary
}

@Composable
private fun RaidHeaderStrip() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF161616).copy(alpha = 0.80f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = stringResource(Res.string.raid_control_deck_title),
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp
    )
    Text(
      text = stringResource(Res.string.raid_control_deck_subtitle),
      color = Color.LightGray,
      fontSize = 12.sp
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      RaidHeaderChip("Roster left")
      RaidHeaderChip("Controls right")
      RaidHeaderChip("Tabs below")
    }
  }
}

@Composable
private fun RaidHeaderChip(label: String) {
  Box(
    modifier = Modifier
      .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun AttendanceTab(
  mainRaid: List<List<RaidFramePayload>>,
  coRaid: List<List<RaidFramePayload>>,
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  playerFaction: Faction,
  raidDepartures: Map<Int, Set<String>>,
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()
  var includeMain by rememberSaveable { mutableStateOf(true) }
  var includeCo by rememberSaveable { mutableStateOf(true) }
  var includeNearbySameFaction by rememberSaveable { mutableStateOf(false) }
  var includeNearbyOppositeFaction by rememberSaveable { mutableStateOf(false) }
  var includePlayersThatLeftRaid by rememberSaveable { mutableStateOf(false) }
  fun String.meetsPvP(): Boolean =
    if (!requirePvPParticipation) true
    else PlayerCacheInteractor.getCard(this)?.hasPvPParticipation() ?: false

  val attendanceNames = run {
    val names = mutableListOf<String>()
    if (includeMain) {
      names += mainRaid.flatten().mapNotNull { frame -> frame.playerName.takeIf { it.isNotBlank() } }
        .filter { it.meetsPvP() }
    }
    if (includeCo) {
      names += coRaid.flatten().mapNotNull { frame -> frame.playerName.takeIf { it.isNotBlank() } }
        .filter { it.meetsPvP() }
    }
    if (includeNearbySameFaction) {
      val sameFactionCards: List<PlayerCard> = when (playerFaction) {
        Faction.HARANYA -> nearbyHaranya
        Faction.NUIA -> nearbyNuia
        Faction.PIRATE -> nearbyPirate
        else -> emptyList()
      }
      names += sameFactionCards
        .let { if (requirePvPParticipation) it.filter { card -> card.hasPvPParticipation() } else it }
        .map { it.name }
    }
    if (includeNearbyOppositeFaction) {
      val oppCards: List<PlayerCard> = when (playerFaction) {
        Faction.HARANYA -> nearbyNuia + nearbyPirate
        Faction.NUIA -> nearbyHaranya + nearbyPirate
        Faction.PIRATE -> nearbyHaranya + nearbyNuia
        else -> emptyList()
      }
      names += oppCards
        .let { if (requirePvPParticipation) it.filter { card -> card.hasPvPParticipation() } else it }
        .map { it.name }
    }
    if (includePlayersThatLeftRaid) {
      val departed: Set<String> =
        (raidDepartures[0] ?: emptySet()) +
            (raidDepartures[1] ?: emptySet())
      names += departed.filter { it.meetsPvP() }
    }
    names.filter { it.isNotBlank() }.distinct().joinToString(", ")
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val wideEnoughForTwoColumns = maxWidth >= 760.dp
      if (wideEnoughForTwoColumns) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top
        ) {
          AttendanceRaidPane(
            mainRaid = mainRaid,
            coRaid = coRaid,
            modifier = Modifier.weight(1f)
          )
          AttendanceControlsPane(
            includeMain = includeMain,
            onIncludeMainChange = { includeMain = it },
            includeCo = includeCo,
            onIncludeCoChange = { includeCo = it },
            includeNearbySameFaction = includeNearbySameFaction,
            onIncludeNearbySameFactionChange = { includeNearbySameFaction = it },
            includeNearbyOppositeFaction = includeNearbyOppositeFaction,
            onIncludeNearbyOppositeFactionChange = { includeNearbyOppositeFaction = it },
            requirePvPParticipation = requirePvPParticipation,
            onRequirePvPParticipationChange = onRequirePvPParticipationChange,
            includePlayersThatLeftRaid = includePlayersThatLeftRaid,
            onIncludePlayersThatLeftRaidChange = { includePlayersThatLeftRaid = it },
            attendanceNames = attendanceNames,
            modifier = Modifier.widthIn(min = 320.dp, max = 390.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          AttendanceRaidPane(
            mainRaid = mainRaid,
            coRaid = coRaid,
            modifier = Modifier.fillMaxWidth()
          )
          AttendanceControlsPane(
            includeMain = includeMain,
            onIncludeMainChange = { includeMain = it },
            includeCo = includeCo,
            onIncludeCoChange = { includeCo = it },
            includeNearbySameFaction = includeNearbySameFaction,
            onIncludeNearbySameFactionChange = { includeNearbySameFaction = it },
            includeNearbyOppositeFaction = includeNearbyOppositeFaction,
            onIncludeNearbyOppositeFactionChange = { includeNearbyOppositeFaction = it },
            requirePvPParticipation = requirePvPParticipation,
            onRequirePvPParticipationChange = onRequirePvPParticipationChange,
            includePlayersThatLeftRaid = includePlayersThatLeftRaid,
            onIncludePlayersThatLeftRaidChange = { includePlayersThatLeftRaid = it },
            attendanceNames = attendanceNames,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
  }
}

data class BuffPreset(val label: String, val requirements: RaidBuffRequirements)

val BUFF_PRESETS = listOf(
  BuffPreset("No Buffs", RaidBuffRequirements()),
  BuffPreset(
    "Light PvP",
    RaidBuffRequirements(
      selected = setOf(
        RaidBuffKey.STATUE_BUFF,
        RaidBuffKey.GOBLET,
        RaidBuffKey.WAR_DRUM,
        RaidBuffKey.FEAST_RIBS
      )
    )
  ),
  BuffPreset(
    "Serious PvP",
    RaidBuffRequirements(
      selected = setOf(
        RaidBuffKey.STATUE_BUFF,
        RaidBuffKey.GOBLET,
        RaidBuffKey.WAR_DRUM,
        RaidBuffKey.SECRET_GIFT,
        RaidBuffKey.FEAST_RIBS,
        RaidBuffKey.JINHUI_WISH,
        RaidBuffKey.LONGING
      )
    )
  ),
  BuffPreset(
    "Full Buffed",
    RaidBuffRequirements(
      selected = setOf(
        RaidBuffKey.STATUE_BUFF,
        RaidBuffKey.GOBLET,
        RaidBuffKey.WAR_DRUM,
        RaidBuffKey.SECRET_GIFT,
        RaidBuffKey.FEAST_RIBS,
        RaidBuffKey.JINHUI_WISH,
        RaidBuffKey.LONGING,
        RaidBuffKey.WHISPER
      ), requireEnhancedLonging = true
    )
  ),
  BuffPreset(
    "Full-Buff BD PvP",
    RaidBuffRequirements(
      selected = setOf(
        RaidBuffKey.STATUE_BUFF,
        RaidBuffKey.GOBLET,
        RaidBuffKey.WAR_DRUM,
        RaidBuffKey.SECRET_GIFT,
        RaidBuffKey.FEAST_RIBS,
        RaidBuffKey.JINHUI_WISH,
        RaidBuffKey.LONGING,
        RaidBuffKey.WHISPER,
        RaidBuffKey.FACTION_WAR_TIME,
        RaidBuffKey.MONSTER_HUNTERS_DREAM
      ), requireEnhancedLonging = true
    )
  ),
  BuffPreset(
    "Full-Buff Kraken PvP",
    RaidBuffRequirements(
      selected = setOf(
        RaidBuffKey.STATUE_BUFF,
        RaidBuffKey.GOBLET,
        RaidBuffKey.WAR_DRUM,
        RaidBuffKey.SECRET_GIFT,
        RaidBuffKey.FEAST_RIBS,
        RaidBuffKey.JINHUI_WISH,
        RaidBuffKey.LONGING,
        RaidBuffKey.WHISPER,
        RaidBuffKey.DAHUTAS_BUBBLE
      ), requireEnhancedLonging = true
    )
  ),
  BuffPreset(
    "Uncontested Boss Kill",
    RaidBuffRequirements(
      selected = setOf(
        RaidBuffKey.STATUE_BUFF,
        RaidBuffKey.GOBLET,
        RaidBuffKey.WAR_DRUM,
        RaidBuffKey.SECRET_GIFT,
        RaidBuffKey.FEAST_RIBS,
        RaidBuffKey.JINHUI_WISH,
        RaidBuffKey.LONGING,
        RaidBuffKey.FACTION_WAR_TIME
      ), lootThreshold = 100
    )
  )
)

@Composable
private fun BuffsTab(
  mainRaid: List<List<RaidFramePayload>>,
  coRaid: List<List<RaidFramePayload>>,
  buffSelectBorder: Color = Color.White.copy(alpha = 0.06f)
) {
  val config by RFConfig.state.collectAsState()
  val lootEnabled by com.reoky.raidframer.RaidCallerSync.lootBuffEnabled.collectAsState()
  val lootThreshold = config.raidCallerLootBuffThreshold.coerceIn(100, 600)
  val gracePeriod = RaidBuffGracePeriod.entries.firstOrNull { it.name == config.raidCallerBuffGracePeriod }
    ?: RaidBuffGracePeriod.FIFTEEN_MINUTES
  // Effective requirements used for matching: buff selection comes from config, and the loot
  // threshold is only applied when the (in-memory) "check for loot buffs?" box is checked.
  val baseRequirements = remember(config.raidCallerBuffRequirements) {
    parseRaidBuffRequirements(config.raidCallerBuffRequirements)
  }
  val requirements = remember(baseRequirements, lootEnabled, lootThreshold) {
    baseRequirements.copy(lootThreshold = if (lootEnabled) lootThreshold else 0)
  }

  fun persistRequirements(newReq: RaidBuffRequirements) {
    // Persist the buff selection (ignoring loot threshold, which is derived from the in-memory box).
    RFConfig.update { it.copy(raidCallerBuffRequirements = newReq.copy(lootThreshold = 0).serialize()) }
    com.reoky.raidframer.RaidCallerSync.setLootBuffEnabled(newReq.lootThreshold > 0)
    RFConfig.update { it.copy(raidCallerLootBuffThreshold = newReq.lootThreshold.coerceIn(100, 600)) }
  }

  var selectedPlayer by remember { mutableStateOf<RaidFramePayload?>(null) }
  var selectedPlayerPopupOffset by remember { mutableStateOf(IntOffset.Zero) }
  val customLabel = stringResource(Res.string.raid_preset_custom)
  val selectedPreset = remember(baseRequirements, customLabel) {
    BUFF_PRESETS.firstOrNull { it.requirements.serialize() == baseRequirements.serialize() }
      ?: BuffPreset(customLabel, baseRequirements)
  }
  var presetExpanded by remember { mutableStateOf(false) }
  val allMembers = (mainRaid.flatten() + coRaid.flatten()).filter { it.playerName.isNotBlank() }
  val observations = allMembers.associateWith { PlayerCacheInteractor.resolveRaidBuffObservation(it, gracePeriod) }
  val buffed = allMembers.filter { member ->
    val observation = observations.getValue(member)
    val snapshot = observation.snapshot
    snapshot != null && snapshot.confirmed && requirements.matches(member.copy(buffs = snapshot.buffIds.map { id ->
      BuffPayload(buff_id = id)
    }), config.raidCallerAllowGuildBuff)
  }.joinToString(", ") { it.playerName }
  val notBuffed = allMembers.filter { member ->
    val observation = observations.getValue(member)
    val snapshot = observation.snapshot
    snapshot != null && snapshot.confirmed && !requirements.matches(member.copy(buffs = snapshot.buffIds.map { id ->
      BuffPayload(buff_id = id)
    }), config.raidCallerAllowGuildBuff)
  }.joinToString(", ") { it.playerName }
  val notScannable = allMembers.filter { observations.getValue(it).snapshot == null }
    .joinToString(", ") { it.playerName }
  val buffLabels = localizedBuffLabels()
  Box(Modifier.fillMaxSize()) {
    Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Performance banner when buff scanning is disabled
      if (!RFConfig.state.collectAsState().value.performanceRaidBuffScanning) {
        PerformanceDisabledBanner(stringResource(Res.string.performance_buff_scanning_disabled_banner))
      }
      BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 760.dp) {
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              BuffRaidPane(
                mainRaid,
                coRaid,
                selectedPlayer,
                requirements,
                gracePeriod,
                config.raidCallerAllowGuildBuff,
                { selectedPlayer = it },
                { player, offset ->
                  if (player.playerName.isNotBlank()) {
                    selectedPlayer = player; selectedPlayerPopupOffset = offset
                  }
                },
                Modifier.fillMaxWidth()
              )
              BuffCopyPane(notBuffed, buffed, notScannable, Modifier.fillMaxWidth())
              LootBuffRankList(allMembers, observations, Modifier.fillMaxWidth())
            }
            BuffControlsPane(
              requirements,
              { persistRequirements(it) },
              selectedPreset,
              { persistRequirements(it.requirements) },
              presetExpanded,
              { presetExpanded = !presetExpanded },
              gracePeriod,
              { gp -> RFConfig.update { cfg -> cfg.copy(raidCallerBuffGracePeriod = gp.name) } },
              lootThreshold,
              { v -> RFConfig.update { cfg -> cfg.copy(raidCallerLootBuffThreshold = v) } },
              lootEnabled,
              { com.reoky.raidframer.RaidCallerSync.setLootBuffEnabled(it) },
              buffLabels,
              config,
              Modifier.widthIn(min = 320.dp, max = 390.dp),
              buffSelectBorder
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BuffRaidPane(
              mainRaid,
              coRaid,
              selectedPlayer,
              requirements,
              gracePeriod,
              config.raidCallerAllowGuildBuff,
              { selectedPlayer = it },
              { player, offset ->
                if (player.playerName.isNotBlank()) {
                  selectedPlayer = player; selectedPlayerPopupOffset = offset
                }
              },
              Modifier.fillMaxWidth()
            )
            BuffControlsPane(
              requirements,
              { persistRequirements(it) },
              selectedPreset,
              { persistRequirements(it.requirements) },
              presetExpanded,
              { presetExpanded = !presetExpanded },
              gracePeriod,
              { gp -> RFConfig.update { cfg -> cfg.copy(raidCallerBuffGracePeriod = gp.name) } },
              lootThreshold,
              { v -> RFConfig.update { cfg -> cfg.copy(raidCallerLootBuffThreshold = v) } },
              lootEnabled,
              { com.reoky.raidframer.RaidCallerSync.setLootBuffEnabled(it) },
              buffLabels,
              config,
              Modifier.fillMaxWidth(),
              buffSelectBorder
            )
            BuffCopyPane(notBuffed, buffed, notScannable, Modifier.fillMaxWidth())
            LootBuffRankList(allMembers, observations, Modifier.fillMaxWidth())
          }
        }
      }
    }
    selectedPlayer?.let { player ->
      val selectedObservation = PlayerCacheInteractor.resolveRaidBuffObservation(player, gracePeriod)
      val observationSnapshot = selectedObservation.snapshot
      val resolvedPlayer =
        observationSnapshot?.let { player.copy(buffs = it.buffIds.map { id -> BuffPayload(buff_id = id) }) } ?: player
      val observationText = selectedObservation.observedAt?.let { timestamp ->
        if (selectedObservation.isCurrent) {
          stringResource(Res.string.raid_buff_current_scan)
        } else {
          val timeAgoResult = timestamp.timeAgo()
          stringResource(Res.string.raid_buff_last_observed, timeAgoResult.resolveLocalizedString())
        }
      } ?: stringResource(Res.string.raid_buff_none_found)
      Popup(
        popupPositionProvider = buffDetailPopupPositionProvider(selectedPlayerPopupOffset),
        onDismissRequest = { selectedPlayer = null },
        properties = PopupProperties(focusable = false)
      ) {
        Surface(
          color = RFColors.PopupBackground.copy(alpha = 0.98f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, RFColors.CardBorder),
          elevation = 6.dp
        ) {
          BuffPlayerDetailPane(
            playerName = player.playerName,
            buffIds = resolvedPlayer.buffs.mapTo(mutableSetOf()) { it.buff_id },
            requirements = requirements,
            allowGuildBuff = config.raidCallerAllowGuildBuff,
            footerText = observationText,
            onClose = { selectedPlayer = null },
            modifier = Modifier.padding(10.dp).widthIn(max = 300.dp)
          )
        }
      }
    }
  }
}

fun RaidBuffGracePeriod.label(): String = when (this) {
  RaidBuffGracePeriod.IMMEDIATE -> "Immediate"
  RaidBuffGracePeriod.FIFTEEN_MINUTES -> "15 Minutes"
  RaidBuffGracePeriod.THIRTY_MINUTES -> "30 Minutes"
  RaidBuffGracePeriod.ONE_HOUR -> "1 Hour"
  RaidBuffGracePeriod.SIX_HOURS -> "6 Hours"
}

@Composable
private fun BuffRaidPane(
  mainRaid: List<List<RaidFramePayload>>,
  coRaid: List<List<RaidFramePayload>>,
  selected: RaidFramePayload?,
  requirements: RaidBuffRequirements,
  gracePeriod: RaidBuffGracePeriod,
  allowGuildBuff: Boolean,
  onSelect: (RaidFramePayload) -> Unit,
  onSelectAt: (RaidFramePayload, IntOffset) -> Unit,
  modifier: Modifier
) {
  Column(
    modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          stringResource(Res.string.raid_main_raid_label),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          mainRaid,
          selectedPlayerName = selected?.playerName,
          onPlayerClick = onSelect,
          onPlayerClickAt = onSelectAt,
          isBuffed = { requirements.matchesResolved(it, gracePeriod, allowGuildBuff) },
          isOutOfRange = { it.distance > 115 },
           isObservationKnown = { PlayerCacheInteractor.resolveRaidBuffObservation(it, gracePeriod).isKnown })
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          stringResource(Res.string.raid_coraid_label),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          coRaid,
          selectedPlayerName = selected?.playerName,
          onPlayerClick = onSelect,
          onPlayerClickAt = onSelectAt,
          isBuffed = { requirements.matchesResolved(it, gracePeriod, allowGuildBuff) },
          isOutOfRange = { it.distance > 115 },
           isObservationKnown = { PlayerCacheInteractor.resolveRaidBuffObservation(it, gracePeriod).isKnown })
      }
    }
  }
}

@Composable
private fun BuffControlsPane(
  requirements: RaidBuffRequirements,
  onRequirements: (RaidBuffRequirements) -> Unit,
  preset: BuffPreset,
  onPreset: (BuffPreset) -> Unit,
  expanded: Boolean,
  onExpanded: () -> Unit,
  gracePeriod: RaidBuffGracePeriod,
  onGracePeriod: (RaidBuffGracePeriod) -> Unit,
  lootThreshold: Int,
  onLootThreshold: (Int) -> Unit,
  lootEnabled: Boolean,
  onLootEnabled: (Boolean) -> Unit,
  localizedBuffLabels: Map<RaidBuffKey, String>,
  config: ConfigEntity,
  modifier: Modifier,
  borderColor: Color = Color.White.copy(alpha = 0.06f)
) {
  Column(
    modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp))
      .border(1.dp, borderColor, RoundedCornerShape(14.dp)).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    var graceExpanded by remember { mutableStateOf(false) }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(Res.string.raid_buff_preset), color = Color.White, fontWeight = FontWeight.Bold)
        Box {
          Button(
            onClick = onExpanded,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            modifier = Modifier.fillMaxWidth()
          ) { Text(preset.label, color = Color.Black, maxLines = 1) }
          DropdownMenu(
            expanded = expanded,
            onDismissRequest = onExpanded
          ) {
            BUFF_PRESETS.forEach { option ->
              DropdownMenuItem(onClick = { onPreset(option); onExpanded() }) {
                Text(
                  option.label
                )
              }
            }
          }
        }
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(Res.string.raid_buff_grace_period), color = Color.White, fontWeight = FontWeight.Bold)
        Box {
          Button(
            onClick = { graceExpanded = !graceExpanded },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            modifier = Modifier.fillMaxWidth()
          ) { Text(gracePeriod.label(), color = Color.Black, maxLines = 1) }
          DropdownMenu(
            expanded = graceExpanded,
            onDismissRequest = { graceExpanded = false }) {
            RaidBuffGracePeriod.entries.forEach { option ->
              DropdownMenuItem(onClick = {
                onGracePeriod(option); graceExpanded = false
              }) { Text(option.label()) }
            }
          }
        }
      }
    }
    Text(stringResource(Res.string.raid_buff_requirements), color = Color.White, fontWeight = FontWeight.Bold)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val columns = if (maxWidth >= 360.dp) 2 else 1
      if (columns == 2) {
        FlowRow(
          maxItemsInEachRow = 2,
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
          BuffRequirementCheckboxes(requirements, onRequirements, localizedBuffLabels, config)
        }
      } else {
        Column { BuffRequirementCheckboxes(requirements, onRequirements, localizedBuffLabels, config) }
      }
    }
    Text(
      stringResource(Res.string.raid_buff_loot_section),
      color = Color.White,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 6.dp)
    )
    // "Check for loot buffs?" + a 100-600% threshold slider (100% is the in-game baseline).
    // The enabled state is held in-memory only (not persisted), while the threshold value is
    // persisted and kept in sync with the Raid Caller overlay settings.
    var lootSliderValue by remember(requirements) { mutableStateOf(lootThreshold) }
    LaunchedEffect(lootThreshold) { lootSliderValue = lootThreshold }
    ControlledCheckbox(stringResource(Res.string.raid_buff_check_loot), lootEnabled) { checked ->
      onLootEnabled(checked)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(Res.string.raid_buff_loot_threshold_label), color = Color.White, fontSize = 11.sp)
      DragLockedSlider(
        value = lootSliderValue.toFloat(),
        onValueChange = {
          lootSliderValue = it.toInt().coerceIn(100, 600)
          onLootThreshold(lootSliderValue)
        },
        modifier = Modifier.weight(1f).height(28.dp),
        valueRange = 100f..600f
      )
      Text(
        text = "$lootSliderValue%",
        color = if (lootEnabled) RFColors.lootBuffColor else Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        textAlign = TextAlign.End,
        modifier = Modifier.width(42.dp)
      )
    }
    Text(
      text = stringResource(Res.string.raid_buff_loot_baseline_note),
      color = RFColors.TextTertiary,
      fontSize = 10.sp,
      lineHeight = 12.sp,
      modifier = Modifier.padding(top = 2.dp)
    )
  }
}

@Composable
private fun BuffCopyPane(notBuffed: String, buffed: String, notScannable: String, modifier: Modifier) {
  var notScannableExpanded by remember { mutableStateOf(false) }
  Column(
    modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        stringResource(Res.string.raid_buff_categories_help_title),
        color = Color.White,
        fontWeight = FontWeight.Bold
      )
      BuffCategoriesHelp()
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        stringResource(Res.string.raid_copy_not_buffed_title),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f)
      )
      Text(
        stringResource(Res.string.raid_copy_buffed_title),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f)
      )
    }
    Row(
      Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 150.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      SelectableTextField(value = notBuffed, modifier = Modifier.weight(1f).fillMaxHeight(), minHeight = 0.dp)
      SelectableTextField(value = buffed, modifier = Modifier.weight(1f).fillMaxHeight(), minHeight = 0.dp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(notBuffed), null) },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
        modifier = Modifier.weight(1f)
      ) { Text(stringResource(Res.string.raid_copy_not_buffed), color = Color.Black) }
      Button(
        onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(buffed), null) },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
        modifier = Modifier.weight(1f)
      ) { Text(stringResource(Res.string.raid_copy_buffed), color = Color.Black) }
    }
    // Collapsible "Not Scannable" section — players we have no in-grace observation for.
    // Collapses to a single header row to avoid taking up screen real estate.
    Row(
      Modifier.fillMaxWidth().clickable { notScannableExpanded = !notScannableExpanded },
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        stringResource(Res.string.raid_copy_not_scannable_title),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f)
      )
      Text(
        if (notScannableExpanded) "\u25B2" else "\u25BC",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
    if (notScannableExpanded) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableTextField(
          value = notScannable,
          modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 150.dp),
          minHeight = 0.dp
        )
        Button(
          onClick = {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
              StringSelection(notScannable),
              null
            )
          },
          colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(Res.string.raid_copy_not_scannable), color = Color.Black) }
      }
    }
  }
}

@Composable
private fun BuffCategoriesHelp() {
  var showHelp by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { showHelp = !showHelp }, modifier = Modifier.size(24.dp)) {
      Text("?", color = RFColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    if (showHelp) {
      Popup {
        Surface(
          color = RFColors.PopupBackground.copy(alpha = 0.98f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, RFColors.CardBorder),
          elevation = 6.dp
        ) {
          Column(
            Modifier.padding(10.dp).widthIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                stringResource(Res.string.raid_buff_categories_help_title),
                color = RFColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              IconButton(
                onClick = { showHelp = false },
                modifier = Modifier.size(24.dp)
              ) {
                Text("X", color = RFColors.TextSecondary, fontSize = 11.sp)
              }
            }
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            BuffCategoryHelpRow(
              stringResource(Res.string.raid_copy_buffed_title),
              stringResource(Res.string.raid_buff_categories_buffed)
            )
            BuffCategoryHelpRow(
              stringResource(Res.string.raid_copy_not_buffed_title),
              stringResource(Res.string.raid_buff_categories_not_buffed)
            )
            BuffCategoryHelpRow(
              stringResource(Res.string.raid_copy_not_scannable_title),
              stringResource(Res.string.raid_buff_categories_not_scannable)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BuffCategoryHelpRow(title: String, explanation: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    Text(explanation, color = RFColors.TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
  }
}

@Composable
private fun LootBuffRankList(
  members: List<RaidFramePayload>,
  observations: Map<RaidFramePayload, RaidBuffObservation>,
  modifier: Modifier
) {
  // Scrollable, max-height breakdown of players sorted by their current summed loot buff %,
  // most → least. Uses the grace-period cached snapshots so players who loot buffed recently
  // (but are now out-of-range / empty) still appear. Rows flow across multiple columns so
  // more players are visible at once. Uses the buff color (gold) from ColorsHelper.
  val ranked = members
    .map { member ->
      val snapshot = observations[member]?.snapshot
      val amount = if (snapshot != null) lootBuffTotalForIds(snapshot.buffIds) else 0
      member to amount
    }
    .filter { it.second > 0 }
    .sortedByDescending { it.second }
  Column(
    modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(stringResource(Res.string.raid_loot_breakdown_title), color = Color.White, fontWeight = FontWeight.Bold)
    if (ranked.isEmpty()) {
      Text(stringResource(Res.string.raid_buff_none_found), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
    } else {
      val maxAmount = ranked.first().second.coerceAtLeast(1)
      BoxWithConstraints(
        Modifier.fillMaxWidth().heightIn(max = 224.dp).verticalScroll(rememberScrollState())
      ) {
        // Two columns when there's room; otherwise a single column.
        val columns = if (maxWidth >= 520.dp) 2 else 1
        val chunkSize = if (ranked.size % columns == 0) ranked.size / columns else (ranked.size / columns) + 1
        // Bottom padding so the last row's text doesn't clip against the scroll container.
        if (columns == 2) {
          Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ranked.take(100).chunked(chunkSize).forEach { chunk ->
              Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                chunk.forEach { (member, amount) ->
                  LootBuffRow(member.playerName, amount, maxAmount)
                }
              }
            }
          }
        } else {
          Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ranked.take(100).forEach { (member, amount) ->
              LootBuffRow(member.playerName, amount, maxAmount)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LootBuffRow(name: String, amount: Int, maxAmount: Int) {
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(name, color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
    // Mini bar — visual comparison of this player's loot % against the raid max.
    val fraction = (amount.toFloat() / maxAmount).coerceIn(0f, 1f)
    Box(
      modifier = Modifier
        .width(56.dp)
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(Color.White.copy(alpha = 0.1f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(fraction)
          .clip(RoundedCornerShape(3.dp))
          .background(RFColors.lootBuffColor)
      )
    }
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      "${amount}%",
      color = RFColors.lootBuffColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.width(42.dp)
    )
  }
}

@Composable
private fun BuffRequirementCheckboxes(
  requirements: RaidBuffRequirements,
  onRequirements: (RaidBuffRequirements) -> Unit,
  localizedBuffLabels: Map<RaidBuffKey, String>,
  config: ConfigEntity
) {
  RAID_BUFF_DEFINITIONS.filter { it.section == RaidBuffSection.MAIN && it.key != RaidBuffKey.GALLANTS_BLESSING }.forEach { definition ->
    ControlledCheckbox(
      localizedBuffLabels[definition.key].orEmpty(),
      definition.key in requirements.selected
    ) { checked -> onRequirements(requirements.copy(selected = if (checked) requirements.selected + definition.key else requirements.selected - definition.key)) }
  }
  ControlledCheckbox(
    stringResource(Res.string.raid_buff_allow_guild_buff),
    config.raidCallerAllowGuildBuff
  ) { RFConfig.update { cfg -> cfg.copy(raidCallerAllowGuildBuff = it) } }
  ControlledCheckbox(
    stringResource(Res.string.raid_buff_orange_goblet),
    requirements.requireOrangeGoblet
  ) {
    onRequirements(
      requirements.copy(
        requireOrangeGoblet = it,
        selected = requirements.selected + RaidBuffKey.GOBLET
      )
    )
  }
  ControlledCheckbox(
    stringResource(Res.string.raid_buff_allow_meatballs),
    requirements.allowMeatballs
  ) { onRequirements(requirements.copy(allowMeatballs = it)) }
  ControlledCheckbox(
    stringResource(Res.string.raid_buff_require_enhanced),
    requirements.requireEnhancedLonging
  ) {
    onRequirements(
      requirements.copy(
        requireEnhancedLonging = it,
        selected = requirements.selected + RaidBuffKey.LONGING
      )
    )
  }
}

@Composable
private fun ControlledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = Color.White)
    )
    Text(label, color = Color.White)
  }
}

@Composable
private fun AttendanceRaidPane(
  mainRaid: List<List<RaidFramePayload>>,
  coRaid: List<List<RaidFramePayload>>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(Color(0xFF1A1A1A).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    FlowRow(
      modifier = Modifier.wrapContentWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = stringResource(Res.string.raid_main_raid_label),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = mainRaid,
          modifier = Modifier.wrapContentSize()
        )
      }
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = stringResource(Res.string.raid_coraid_label),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = coRaid,
          modifier = Modifier.wrapContentSize()
        )
      }
    }
  }
}

@Composable
private fun AttendanceControlsPane(
  includeMain: Boolean,
  onIncludeMainChange: (Boolean) -> Unit,
  includeCo: Boolean,
  onIncludeCoChange: (Boolean) -> Unit,
  includeNearbySameFaction: Boolean,
  onIncludeNearbySameFactionChange: (Boolean) -> Unit,
  includeNearbyOppositeFaction: Boolean,
  onIncludeNearbyOppositeFactionChange: (Boolean) -> Unit,
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit,
  includePlayersThatLeftRaid: Boolean,
  onIncludePlayersThatLeftRaidChange: (Boolean) -> Unit,
  attendanceNames: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_main_raid),
      initialChecked = includeMain,
      onCheckedChange = onIncludeMainChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_coraid),
      initialChecked = includeCo,
      onCheckedChange = onIncludeCoChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_nearby_same_faction),
      initialChecked = includeNearbySameFaction,
      onCheckedChange = onIncludeNearbySameFactionChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_nearby_opposite_faction),
      initialChecked = includeNearbyOppositeFaction,
      onCheckedChange = onIncludeNearbyOppositeFactionChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_require_pvp_filter),
      initialChecked = requirePvPParticipation,
      onCheckedChange = onRequirePvPParticipationChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_departed),
      initialChecked = includePlayersThatLeftRaid,
      onCheckedChange = onIncludePlayersThatLeftRaidChange,
      textColor = Color.White
    )
    SelectableTextField(
      value = attendanceNames,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
    )
    Button(
      onClick = {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(attendanceNames), null)
      },
      colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
      modifier = Modifier.padding(top = 2.dp)
    ) {
      Text(text = stringResource(Res.string.raid_copy_attendance), color = Color.Black)
    }
  }
}

@Composable
private fun NearbyTab(
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  playerFaction: Faction = Faction.UNKNOWN,
  filterState: NearbyFilterState,
  raidMemberNames: Set<String>
) {
  val scrollState = rememberScrollState()
  val inRaidOnly = filterState.showInRaidOnly
  val inRaidIndicator = stringResource(Res.string.nearby_in_raid_only_indicator)
  fun filterFaction(players: List<PlayerCard>, faction: Faction): List<PlayerCard> {
    val filtered = if (inRaidOnly && faction == playerFaction) {
      players.filter { it.name in raidMemberNames }
    } else {
      players
    }
    return filterNearbyPlayers(filtered, filterState)
  }
  fun factionLabelWithIndicator(faction: Faction): String {
    if (inRaidOnly && faction == playerFaction) return inRaidIndicator
    return ""
  }
  val filteredHaranya = filterFaction(nearbyHaranya, Faction.HARANYA)
  val filteredNuia = filterFaction(nearbyNuia, Faction.NUIA)
  val filteredPirate = filterFaction(nearbyPirate, Faction.PIRATE)

  @Composable
  fun factionHeader(faction: Faction, count: Int, formatRes: org.jetbrains.compose.resources.StringResource) {
    val indicator = factionLabelWithIndicator(faction)
    val base = String.format(stringResource(formatRes), count)
    val fullText = if (indicator.isNotBlank()) "$base $indicator" else base
    val indicatorColor = RFColors.IndicatorAmber
    if (indicator.isNotBlank()) {
      val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
          append(base)
        }
        withStyle(SpanStyle(color = indicatorColor, fontWeight = FontWeight.Bold)) {
          append(" $indicator")
        }
      }
      Text(text = annotated, fontSize = 13.sp)
    } else {
      Text(text = fullText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        factionHeader(Faction.HARANYA, filteredHaranya.size, Res.string.raid_haranya_faction)
        RaidComponent(
          parties = filteredHaranya.mapIndexed { index, card ->
            RaidFramePayload(
              slot = index,
              playerName = card.name,
              role = card.currentRole,
              gearScore = card.lastKnownGearScore
            )
          }.chunked(5),
          modifier = Modifier.wrapContentSize()
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        factionHeader(Faction.NUIA, filteredNuia.size, Res.string.raid_nuian_faction)
        RaidComponent(
          parties = filteredNuia.mapIndexed { index, card ->
            RaidFramePayload(
              slot = index,
              playerName = card.name,
              role = card.currentRole,
              gearScore = card.lastKnownGearScore
            )
          }.chunked(5),
          modifier = Modifier.wrapContentSize()
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        factionHeader(Faction.PIRATE, filteredPirate.size, Res.string.raid_pirate_faction)
        RaidComponent(
          parties = filteredPirate.mapIndexed { index, card ->
            RaidFramePayload(
              slot = index,
              playerName = card.name,
              role = card.currentRole,
              gearScore = card.lastKnownGearScore
            )
          }.chunked(5),
          modifier = Modifier.wrapContentSize()
        )
      }
    }
    Text(
      text = stringResource(Res.string.raid_nearby_disclaimer),
      color = Color.LightGray,
      fontSize = 11.sp,
      fontStyle = FontStyle.Italic,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

private fun averageGearScore(players: List<PlayerCard>): Int {
  val known = players.filter { it.lastKnownGearScore > 0 }
  return if (known.isEmpty()) 0 else known.map { it.lastKnownGearScore }.average().toInt()
}

/*
 * Buff History surfaces: dark and deliberately translucent so the game world stays faintly
 * visible behind them, with hairline borders instead of heavy frames.
 */
internal val BuffHistoryPanelColor = Color(0xFF0A0F11).copy(alpha = 0.45f)
internal val BuffHistoryCardColor = Color(0xFF0E1315).copy(alpha = 0.72f)
internal val BuffHistoryHeaderTint = Color.White.copy(alpha = 0.03f)
internal val BuffHistoryRowTint = Color.White.copy(alpha = 0.025f)
internal val BuffHistoryHairline = Color.White.copy(alpha = 0.07f)
internal val BuffHistoryMenuColor = Color(0xFF14181A)
internal val BuffHistoryUptimeColor = Color(0xFF7CFF8A)
/** Ideal snapshot-card width for the masonry; columns range from 1 up to [BuffHistoryCardMaxColumns]. */
internal val BuffHistoryCardIdealWidth = 340.dp
private const val BuffHistoryCardMaxColumns = 3

/**
 * Compact dark select used by the Buff History filter bar. [active] marks a filter that is
 * currently narrowing the view and [expanded] an open menu; both light the control up in teal so
 * the engaged filter reads at a glance.
 */
@Composable
internal fun BuffHistorySelectButton(
  value: String,
  active: Boolean,
  expanded: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val hovered by interactionSource.collectIsHoveredAsState()
  val engaged = active || expanded
  val shape = RoundedCornerShape(3.dp)
  val background = when {
    engaged -> Color(0xFF12312C).copy(alpha = 0.85f)
    hovered -> Color.White.copy(alpha = 0.10f)
    else -> Color(0xFF0D1112).copy(alpha = 0.85f)
  }
  val border = when {
    engaged -> RFColors.buffHistoryBuffed.copy(alpha = 0.45f)
    hovered -> Color.White.copy(alpha = 0.18f)
    else -> Color.White.copy(alpha = 0.10f)
  }

  Row(
    modifier = modifier
      .height(26.dp)
      .clip(shape)
      .background(background, shape)
      .border(1.dp, border, shape)
      .hoverable(interactionSource)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = value,
      color = when {
        engaged -> Color(0xFFBFF4E9)
        hovered -> RFColors.TextPrimary
        else -> RFColors.TextSecondary
      },
      fontSize = 11.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = "\u25BE",
      color = if (engaged) RFColors.buffHistoryBuffed.copy(alpha = 0.85f) else RFColors.TextTertiary,
      fontSize = 8.sp
    )
  }
}

/** Dark dropdown surface shared by the Buff History filter selects; marks the current choice. */
@Composable
internal fun BuffHistoryMenu(
  expanded: Boolean,
  onDismiss: () -> Unit,
  content: @Composable ColumnScope.() -> Unit
) {
  MaterialTheme(colors = MaterialTheme.colors.copy(surface = BuffHistoryMenuColor)) {
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = onDismiss,
      modifier = Modifier
        .background(BuffHistoryMenuColor, RoundedCornerShape(4.dp))
        .border(1.dp, RFColors.CardBorder, RoundedCornerShape(4.dp))
    ) {
      content()
    }
  }
}

/** Compact count + label pair used for the secondary buff statistics in a snapshot header. */
@Composable
internal fun BuffHistoryStat(color: Color, value: Int, label: String) {
  Row(
    modifier = Modifier.height(12.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(1.dp)))
    Text(
      text = "$value",
      color = color,
      fontSize = 10.sp,
      lineHeight = 10.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1
    )
    Text(
      text = label,
      color = RFColors.TextTertiary,
      fontSize = 10.sp,
      lineHeight = 10.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun BuffHistoryTab(
  mainRaid: List<List<RaidFramePayload>>,
  coRaid: List<List<RaidFramePayload>>
) {
  val config by RFConfig.state.collectAsState()
  val sessionStart = config.lastSessionStart

  val allMembers = (mainRaid.flatten() + coRaid.flatten()).filter { it.playerName.isNotBlank() }
  val allPlayerNames = allMembers.map { it.playerName }

  // Depends on raid data so the timeline recomputes when new snapshots arrive (every scan interval)
  val timeline = remember(config.raidCallerBuffRequirements, mainRaid, coRaid) {
    PlayerCacheInteractor.getRaidBuffTimeline()
  }
  // Recorded raid rosters, so each snapshot bucket can use the roster that was actually present
  // instead of the current one: totals follow the raid as players join and leave.
  val rosterLog = remember(config.raidCallerBuffRequirements, mainRaid, coRaid) {
    PlayerCacheInteractor.getRaidRosterTimeline()
  }

  val baseRequirements = remember(config.raidCallerBuffRequirements) {
    parseRaidBuffRequirements(config.raidCallerBuffRequirements)
  }
  var localRequirements by remember { mutableStateOf(baseRequirements) }
  var mode by remember { mutableStateOf(BuffHistoryMode.ALL_BUFFS) }
  var selectedBuffKey by remember { mutableStateOf<RaidBuffKey?>(null) }

  val snapshotCards = remember(
    timeline, rosterLog, localRequirements, selectedBuffKey, mode, allPlayerNames, sessionStart, config.raidCallerAllowGuildBuff
  ) {
    computeSnapshotCards(
      timeline = timeline,
      sessionStart = sessionStart,
      allPlayerNames = allPlayerNames,
      requirements = localRequirements,
      individualBuffKey = selectedBuffKey,
      mode = mode,
      allowGuildBuff = config.raidCallerAllowGuildBuff,
      rosterTimeline = rosterLog
    )
  }
  val dataPoints = remember(snapshotCards) { computeBuffHistoryDataPoints(snapshotCards) }
  val buffLabels = localizedBuffLabels()

  val playerCards = PlayerCacheInteractor.getGroupCards { true }.associateBy { it.name }
  val showSpecIcons = config.combatShowSpecIcons
  val scrollState = rememberScrollState()
  var selectedDetail by remember { mutableStateOf<BuffHistoryPlayerDetail?>(null) }

  var modeExpanded by remember { mutableStateOf(false) }
  var presetExpanded by remember { mutableStateOf(false) }

  if (dataPoints.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = stringResource(Res.string.raid_buff_history_no_data),
        color = RFColors.TextSecondary,
        fontSize = 12.sp
      )
    }
  } else {
    Column(modifier = Modifier.fillMaxSize()) {
      // Filter bar — understated dark selects; an engaged filter lights up in teal.
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box {
          BuffHistorySelectButton(
            value = when (mode) {
              BuffHistoryMode.ALL_BUFFS -> stringResource(Res.string.raid_buff_history_all_buffs)
              BuffHistoryMode.INDIVIDUAL_BUFF -> stringResource(Res.string.raid_buff_history_individual_buff)
              BuffHistoryMode.LOOT_PERCENTAGE -> stringResource(Res.string.raid_buff_history_loot_percentage)
            },
            active = mode != BuffHistoryMode.ALL_BUFFS,
            expanded = modeExpanded,
            onClick = { modeExpanded = !modeExpanded },
            modifier = Modifier.widthIn(max = 170.dp)
          )
          BuffHistoryMenu(expanded = modeExpanded, onDismiss = { modeExpanded = false }) {
            BuffHistoryMode.entries.forEach { m ->
              DropdownMenuItem(onClick = { mode = m; modeExpanded = false }) {
                Text(
                  text = stringResource(m.labelRes),
                  color = if (m == mode) RFColors.buffHistoryBuffed else RFColors.TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = if (m == mode) FontWeight.Medium else FontWeight.Normal
                )
              }
            }
          }
        }

        Box {
          val currentPreset = BUFF_PRESETS.firstOrNull { it.requirements.serialize() == localRequirements.serialize() }
            ?: BuffPreset(stringResource(Res.string.raid_preset_custom), localRequirements)
          BuffHistorySelectButton(
            value = currentPreset.label,
            active = localRequirements.selected.isNotEmpty(),
            expanded = presetExpanded,
            onClick = { presetExpanded = !presetExpanded },
            modifier = Modifier.widthIn(max = 160.dp)
          )
          BuffHistoryMenu(expanded = presetExpanded, onDismiss = { presetExpanded = false }) {
            BUFF_PRESETS.forEach { preset ->
              val isCurrent = preset.requirements.serialize() == localRequirements.serialize()
              DropdownMenuItem(onClick = {
                localRequirements = preset.requirements
                presetExpanded = false
              }) {
                Text(
                  text = preset.label,
                  color = if (isCurrent) RFColors.buffHistoryBuffed else RFColors.TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                )
              }
            }
          }
        }

        // Individual buff selector (only for INDIVIDUAL_BUFF mode)
        if (mode == BuffHistoryMode.INDIVIDUAL_BUFF) {
          var buffExpanded by remember { mutableStateOf(false) }
          Box {
            BuffHistorySelectButton(
              value = selectedBuffKey?.let { buffLabels[it] }
                ?: stringResource(Res.string.raid_buff_history_select_criteria),
              active = selectedBuffKey != null,
              expanded = buffExpanded,
              onClick = { buffExpanded = !buffExpanded },
              modifier = Modifier.widthIn(max = 170.dp)
            )
            BuffHistoryMenu(expanded = buffExpanded, onDismiss = { buffExpanded = false }) {
              RAID_BUFF_DEFINITIONS.filter { it.section == RaidBuffSection.MAIN }
                .forEach { def ->
                  val isCurrent = def.key == selectedBuffKey
                  DropdownMenuItem(onClick = {
                    selectedBuffKey = def.key
                    buffExpanded = false
                  }) {
                    Text(
                      text = buffLabels[def.key] ?: def.labelKey,
                      color = if (isCurrent) RFColors.buffHistoryBuffed else RFColors.TextPrimary,
                      fontSize = 11.sp,
                      fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                    )
                  }
                }
            }
          }
        }
      }

      // Graph surface: one dark, translucent panel carrying its own title and legend.
      Box(modifier = Modifier.fillMaxWidth().weight(3f)) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(BuffHistoryPanelColor, RoundedCornerShape(4.dp))
            .border(1.dp, BuffHistoryHairline, RoundedCornerShape(4.dp))
        ) {
          BuffHistoryGraph(
            dataPoints = dataPoints,
            mode = mode,
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      // Snapshot history section label — pinned above the scrolling list.
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(Res.string.raid_buff_history_section_snapshots).uppercase(),
          color = RFColors.TextTertiary,
          fontSize = 9.sp,
          letterSpacing = 1.sp,
          maxLines = 1
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(BuffHistoryHairline))
      }

      // Snapshot history — newest first, flowing into a responsive masonry of cards.
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(4f)
          .verticalScroll(scrollState)
          .padding(bottom = 4.dp)
      ) {
        // Newest first; the first card doubles as the graph origin, so it is the time reference
        val refStart = snapshotCards.firstOrNull()?.timestamp ?: sessionStart
        BuffHistorySnapshotList(
          cards = snapshotCards,
          refStart = refStart,
          playerCards = playerCards,
          showSpecIcons = showSpecIcons,
          onPlayerClick = { detail -> selectedDetail = detail },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }

  selectedDetail?.let { detail ->
    Popup(
      popupPositionProvider = buffDetailPopupPositionProvider(detail.anchor),
      onDismissRequest = { selectedDetail = null },
      properties = PopupProperties(focusable = false)
    ) {
      Surface(
        color = RFColors.PopupBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RFColors.CardBorder),
        elevation = 6.dp
      ) {
        BuffPlayerDetailPane(
          playerName = detail.playerName,
          buffIds = detail.buffIds,
          requirements = localRequirements,
          allowGuildBuff = config.raidCallerAllowGuildBuff,
          notScannable = detail.status == BuffHistoryPlayerStatus.NOT_SCANNABLE,
          footerText = stringResource(Res.string.raid_buff_history_snapshot, detail.timeLabel),
          onClose = { selectedDetail = null },
          modifier = Modifier.padding(10.dp).widthIn(max = 300.dp)
        )
      }
    }
  }
}

/**
 * Snapshot cards in a responsive masonry: a single column on narrow windows, up to
 * [BuffHistoryCardMaxColumns] side by side when there is room. Cards flow newest-first so the
 * first column always starts with the most recent snapshot; column heights may stagger.
 */
@Composable
internal fun BuffHistorySnapshotList(
  cards: List<BuffHistorySnapshotCard>,
  refStart: Long,
  playerCards: Map<String, PlayerCard>,
  showSpecIcons: Boolean,
  onPlayerClick: (BuffHistoryPlayerDetail) -> Unit,
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(modifier) {
    val columnCount = (maxWidth / BuffHistoryCardIdealWidth).toInt().coerceIn(1, BuffHistoryCardMaxColumns)
    val columns = List(columnCount) { mutableListOf<BuffHistorySnapshotCard>() }
    // Newest first, round-robin: the overall-newest card always lands on top of the first column.
    cards.asReversed().forEachIndexed { index, card ->
      columns[index % columnCount].add(card)
    }
    val newest = cards.lastOrNull()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      columns.forEach { column ->
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          column.forEach { card ->
            BuffHistorySnapshotCardView(
              card = card,
              timeLabel = formatBuffHistoryTime((card.timestamp - refStart) / 1000L),
              isNewest = newest != null && card === newest,
              playerCards = playerCards,
              showSpecIcons = showSpecIcons,
              onPlayerClick = onPlayerClick,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }
  }
}

/**
 * One snapshot of the roster: the timestamp heading, the compact buff counts, the buffed-share
 * badge and the aligned player rows - one cohesive surface rather than a card per player.
 */
@Composable
internal fun BuffHistorySnapshotCardView(
  card: BuffHistorySnapshotCard,
  timeLabel: String,
  isNewest: Boolean,
  playerCards: Map<String, PlayerCard>,
  showSpecIcons: Boolean,
  onPlayerClick: (BuffHistoryPlayerDetail) -> Unit,
  modifier: Modifier = Modifier
) {
  val buffedCount = card.playerNameToStatus.values.count { it == BuffHistoryPlayerStatus.BUFFED }
  val notBuffedCount = card.playerNameToStatus.values.count { it == BuffHistoryPlayerStatus.NOT_BUFFED }
  val notScannableCount = card.playerNameToStatus.values.count { it == BuffHistoryPlayerStatus.NOT_SCANNABLE }
  val totalScanned = buffedCount + notBuffedCount
  val uptimePercent = if (totalScanned > 0) (buffedCount * 100f / totalScanned) else 0f

  Surface(
    color = BuffHistoryCardColor,
    shape = RoundedCornerShape(4.dp),
    border = BorderStroke(1.dp, BuffHistoryHairline),
    modifier = modifier
  ) {
    Column {
      // Header: the timestamp is the heading, the buff counts sit below it, and the buffed
      // share sits right. Stats flow to a second line on narrow cards instead of clipping.
      Column(modifier = Modifier.fillMaxWidth().background(BuffHistoryHeaderTint)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = stringResource(Res.string.raid_buff_history_snapshot, "").trim().uppercase(),
                color = RFColors.TextTertiary,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignBy(FirstBaseline)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = timeLabel,
                color = RFColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignBy(FirstBaseline)
              )
              if (isNewest) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "(${stringResource(Res.string.raid_buff_history_most_recent)})",
                  color = RFColors.TextTertiary,
                  fontSize = 9.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.alignBy(FirstBaseline)
                )
              }
            }
            Text(
              text = String.format(stringResource(Res.string.raid_buff_history_buffed_share), uptimePercent),
              color = BuffHistoryUptimeColor,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              maxLines = 1,
              modifier = Modifier
                .background(BuffHistoryUptimeColor.copy(alpha = 0.10f), RoundedCornerShape(3.dp))
                .border(1.dp, BuffHistoryUptimeColor.copy(alpha = 0.28f), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.Center
          ) {
            BuffHistoryStat(
              color = Color(0xFF7CFF8A),
              value = buffedCount,
              label = stringResource(Res.string.raid_buff_history_buffed)
            )
            BuffHistoryStat(
              color = RFColors.buffHistoryUnbuffed,
              value = notBuffedCount,
              label = stringResource(Res.string.raid_buff_history_unbuffed)
            )
            BuffHistoryStat(
              color = RFColors.buffHistoryNotScannable,
              value = notScannableCount,
              label = stringResource(Res.string.raid_buff_history_not_scannable)
            )
          }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BuffHistoryHairline))
      }

      // Roster: aligned columns and fixed-height rows instead of a card per player. The
      // roster is whatever the card's bucket recorded, so departed players never linger here.
      BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth.value / 155f).toInt().coerceIn(2, 8)
        val players = card.playerNameToStatus.keys.sorted()
        Column(modifier = Modifier.fillMaxWidth()) {
          players.chunked(columns).forEachIndexed { rowIndex, rowPlayers ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(if (rowIndex % 2 == 1) BuffHistoryRowTint else Color.Transparent),
              verticalAlignment = Alignment.CenterVertically
            ) {
              rowPlayers.forEachIndexed { columnIndex, playerName ->
                val status = card.playerNameToStatus[playerName] ?: BuffHistoryPlayerStatus.NOT_SCANNABLE
                BuffHistoryPlayerCell(
                  playerName = playerName,
                  status = status,
                  playerCard = playerCards[playerName],
                  showSpecIcons = showSpecIcons,
                  showColumnDivider = columnIndex > 0,
                  onClick = { anchor ->
                    onPlayerClick(BuffHistoryPlayerDetail(
                      playerName = playerName,
                      status = status,
                      buffIds = card.playerNameToBuffIds[playerName].orEmpty(),
                      timeLabel = timeLabel,
                      anchor = anchor
                    ))
                  },
                  modifier = Modifier.weight(1f).fillMaxHeight()
                )
              }
              // Pad out the last row so its columns line up with the rows above
              repeat(columns - rowPlayers.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

/** A clicked player in a Buff History snapshot, plus the popup anchor in window coordinates. */
internal data class BuffHistoryPlayerDetail(
  val playerName: String,
  val status: BuffHistoryPlayerStatus,
  val buffIds: Set<Int>,
  val timeLabel: String,
  val anchor: IntOffset
)

/**
 * One player line inside a snapshot roster: fixed-width status mark, spec icons, name (coloured by
 * buff state, as before) and the faction dot pinned to the right edge so columns stay aligned.
 */
@Composable
internal fun BuffHistoryPlayerCell(
  playerName: String,
  status: BuffHistoryPlayerStatus,
  playerCard: PlayerCard?,
  showSpecIcons: Boolean,
  showColumnDivider: Boolean,
  onClick: (IntOffset) -> Unit,
  modifier: Modifier = Modifier
) {
  val spec = remember(playerCard?.currentBuild) { playerCard?.currentBuild?.let { SpecType.fromName(it) } }
  val statusColor = when (status) {
    BuffHistoryPlayerStatus.BUFFED -> Color(0xFF7CFF8A)
    BuffHistoryPlayerStatus.NOT_BUFFED -> Color(0xFFFF7777)
    BuffHistoryPlayerStatus.NOT_SCANNABLE -> RFColors.TextTertiary
  }
  val statusMark = when (status) {
    BuffHistoryPlayerStatus.BUFFED -> "✓"
    BuffHistoryPlayerStatus.NOT_BUFFED -> "✗"
    BuffHistoryPlayerStatus.NOT_SCANNABLE -> "?"
  }
  val faction = playerCard?.let { Faction.fromString(it.lastKnownFaction) } ?: Faction.UNKNOWN
  var anchor by remember { mutableStateOf(IntOffset.Zero) }
  val interactionSource = remember { MutableInteractionSource() }
  val hovered by interactionSource.collectIsHoveredAsState()

  Row(
    modifier = modifier
      .onGloballyPositioned { coords ->
        val pos = coords.positionInWindow()
        anchor = IntOffset((pos.x + coords.size.width).roundToInt(), pos.y.roundToInt())
      }
      .hoverable(interactionSource)
      .clickable { onClick(anchor) }
      .background(if (hovered) Color.White.copy(alpha = 0.07f) else Color.Transparent, RoundedCornerShape(2.dp))
      .then(
        if (showColumnDivider) {
          Modifier.drawBehind {
            val stroke = 1.dp.toPx()
            val x = stroke / 2f
            drawLine(
              color = BuffHistoryHairline,
              start = Offset(x, 0f),
              end = Offset(x, size.height),
              strokeWidth = stroke
            )
          }
        } else {
          Modifier
        }
      )
      .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.width(9.dp), contentAlignment = Alignment.Center) {
      Text(statusMark, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
    if (showSpecIcons && spec != null && spec != SpecType.UNKNOWN) {
      spec.trees.sortedByDisplayOrder().forEach { tree ->
        Image(
          painter = skillTreeIconPainterFor(tree),
          contentDescription = null,
          modifier = Modifier.size(11.dp)
        )
      }
    }
    Text(
      text = playerName,
      color = statusColor,
      fontSize = 10.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    Box(
      modifier = Modifier
        .size(4.dp)
        .background(factionHighlightColor(faction), RoundedCornerShape(2.dp))
    )
  }
}

@Composable
private fun NearbyGearTab(
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  playerFaction: Faction = Faction.UNKNOWN,
  filterState: NearbyFilterState,
  raidMemberNames: Set<String>
) {
  val scrollState = rememberScrollState()
  val inRaidOnly = filterState.showInRaidOnly
  val inRaidIndicator = stringResource(Res.string.nearby_in_raid_only_indicator)
  fun filterFaction(players: List<PlayerCard>, faction: Faction): List<PlayerCard> {
    val filtered = if (inRaidOnly && faction == playerFaction) {
      players.filter { it.name in raidMemberNames }
    } else {
      players
    }
    return filterNearbyPlayers(filtered, filterState)
  }
  fun factionLabelWithIndicator(faction: Faction): String {
    if (inRaidOnly && faction == playerFaction) return inRaidIndicator
    return ""
  }
  val filteredHaranya = filterFaction(nearbyHaranya, Faction.HARANYA)
  val filteredNuia = filterFaction(nearbyNuia, Faction.NUIA)
  val filteredPirate = filterFaction(nearbyPirate, Faction.PIRATE)

  val avgHaranya = averageGearScore(filteredHaranya)
  val avgNuia = averageGearScore(filteredNuia)
  val avgPirate = averageGearScore(filteredPirate)
  val haranyaLabel = stringResource(Res.string.raid_haranya_faction).substringBefore("(").trimEnd()
  val nuiaLabel = stringResource(Res.string.raid_nuian_faction).substringBefore("(").trimEnd()
  val pirateLabel = stringResource(Res.string.raid_pirate_faction).substringBefore("(").trimEnd()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    @Composable
    fun gearFactionHeader(faction: Faction, count: Int, formatRes: org.jetbrains.compose.resources.StringResource) {
      val indicator = factionLabelWithIndicator(faction)
      val base = String.format(stringResource(formatRes), count)
      val indicatorColor = Color(0xFFE8B868)
      if (indicator.isNotBlank()) {
        val annotated = buildAnnotatedString {
          withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
            append(base)
          }
          withStyle(SpanStyle(color = indicatorColor, fontWeight = FontWeight.Bold)) {
            append(" $indicator")
          }
        }
        Text(text = annotated, fontSize = 13.sp)
      } else {
        Text(text = base, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        gearFactionHeader(Faction.HARANYA, filteredHaranya.size, Res.string.raid_haranya_faction)
        Text(
          text = String.format(
            stringResource(Res.string.nearby_avg_gs_format),
            avgHaranya,
            filteredHaranya.count { it.lastKnownGearScore > 0 }),
          color = Color.LightGray,
          fontSize = 11.sp
        )
        GearScoreHistogram(
          players = filteredHaranya,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        gearFactionHeader(Faction.NUIA, filteredNuia.size, Res.string.raid_nuian_faction)
        Text(
          text = String.format(
            stringResource(Res.string.nearby_avg_gs_format),
            avgNuia,
            filteredNuia.count { it.lastKnownGearScore > 0 }),
          color = Color.LightGray,
          fontSize = 11.sp
        )
        GearScoreHistogram(
          players = filteredNuia,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        gearFactionHeader(Faction.PIRATE, filteredPirate.size, Res.string.raid_pirate_faction)
        Text(
          text = String.format(
            stringResource(Res.string.nearby_avg_gs_format),
            avgPirate,
            filteredPirate.count { it.lastKnownGearScore > 0 }),
          color = Color.LightGray,
          fontSize = 11.sp
        )
        GearScoreHistogram(
          players = filteredPirate,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
    OverlaidGearScoreChart(
      series = listOf(
        GearFactionSeries(haranyaLabel, filteredHaranya, RFColors.graphNodeAllied),
        GearFactionSeries(nuiaLabel, filteredNuia, RFColors.graphNodeEnemy),
        GearFactionSeries(pirateLabel, filteredPirate, RFColors.graphNodePirate)
      ),
      gearScoreLabel = "Gear Score",
      playerCountLabel = "Number of Players",
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
  }
}
