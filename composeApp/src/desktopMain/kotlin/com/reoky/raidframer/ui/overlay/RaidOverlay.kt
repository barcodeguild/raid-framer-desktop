package com.reoky.raidframer.ui.overlay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.ui.component.PerformanceDisabledBanner
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.definitions.RaidBuffKey
import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.definitions.RAID_BUFF_DEFINITIONS
import com.reoky.raidframer.core.definitions.RaidBuffSection
import com.reoky.raidframer.core.definitions.lootBuffById
import com.reoky.raidframer.core.definitions.lootBuffAmountForIds
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.definitions.matchesResolved
import com.reoky.raidframer.core.definitions.matchedDefinitions
import com.reoky.raidframer.core.definitions.missingKeys
import com.reoky.raidframer.core.definitions.parseRaidBuffRequirements
import com.reoky.raidframer.core.definitions.serialize
import com.reoky.raidframer.core.model.hasPvPParticipation
import com.reoky.raidframer.core.model.RaidBuffGracePeriod
import com.reoky.raidframer.core.model.RaidBuffObservation
import com.reoky.raidframer.core.definitions.SKILL_TREE_DISPLAY_ORDER
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.META_CC_SPECS
import com.reoky.raidframer.core.definitions.META_DANCER_SPECS
import com.reoky.raidframer.core.definitions.META_HEALER_SPECS
import com.reoky.raidframer.core.definitions.META_MAGE_SPECS
import com.reoky.raidframer.core.definitions.META_MELEE_SPECS
import com.reoky.raidframer.core.definitions.META_RANGED_SPEC
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.factionHighlightColor
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.helpers.rememberSectionPulse
import com.reoky.raidframer.core.helpers.timeAgo
import com.reoky.raidframer.core.helpers.resolveLocalizedString
import com.reoky.raidframer.OverlayNav
import com.reoky.raidframer.core.serialization.BuffPayload
import com.reoky.raidframer.core.serialization.IPCMessagePayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CheckBoxComponent
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
import raid_framer_desktop.composeapp.generated.resources.raid_composition_require_pvp
import raid_framer_desktop.composeapp.generated.resources.*
import raid_framer_desktop.composeapp.generated.resources.raid_control_deck_subtitle
import raid_framer_desktop.composeapp.generated.resources.raid_control_deck_title
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.time.Duration.Companion.milliseconds

enum class RaidTab { ATTENDANCE, BUFFS, NEARBY, NEARBY_GEAR, COMPOSITION }
@Composable
fun RaidOverlay(wm: WindowManager? = null) {
  val playerFaction = Faction.fromString(RFConfig.state.collectAsState().value.playerFaction)
  val mainRaid = PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid = PlayerCacheInteractor.getRaidById(1).collectAsState()
  val nearbyNuia = PlayerCacheInteractor.nearbyNuianRaidParties.collectAsState()
  val nearbyHaranya = PlayerCacheInteractor.nearbyHaraniRaidParties.collectAsState()
  val nearbyPirate = PlayerCacheInteractor.nearbyPirateRaidParties.collectAsState()
  val raidDepartures = PlayerCacheInteractor.raidDeparturesFlow.collectAsState()
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
  var requirePvPParticipation by rememberSaveable { mutableStateOf(false) }
  var raidWasDetected by remember { mutableStateOf(false) }
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
          onClose = { wm?.closeWindow(OverlayType.RAID) }
        )
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // RaidHeaderStrip() I might want something like this in the future
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
                RaidTab.NEARBY to Res.string.raid_tab_nearby,
                RaidTab.NEARBY_GEAR to Res.string.raid_tab_nearby_gear
                ,RaidTab.COMPOSITION to Res.string.raid_tab_composition
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
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
            )
            RaidTab.BUFFS -> BuffsTab(mainRaid.value, coRaid.value, buffSelectBorder)
            RaidTab.NEARBY -> NearbyTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
            )
            RaidTab.NEARBY_GEAR -> NearbyGearTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              raidDepartures = raidDepartures.value,
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
            )
            RaidTab.COMPOSITION -> CompositionTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              playerFaction = playerFaction,
              raidDepartures = raidDepartures.value,
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
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
  raidDepartures: Map<Int, Set<String>> = emptyMap(),
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  var requireGearOver15k by rememberSaveable { mutableStateOf(false) }
  var includePlayersThatLeftRaid by rememberSaveable { mutableStateOf(false) }
  val departedNames = remember(raidDepartures) {
    (raidDepartures[0] ?: emptySet()) + (raidDepartures[1] ?: emptySet())
  }
  fun sourcePlayers(players: List<PlayerCard>, faction: Faction): List<PlayerCard> = if (!includePlayersThatLeftRaid) {
    players
  } else {
    val currentNames = players.mapTo(mutableSetOf()) { it.name }
    val departedPlayers = departedNames.mapNotNull { PlayerCacheInteractor.getCard(it) }
      .filter { it.name !in currentNames && Faction.fromString(it.lastKnownFaction) == faction }
    (players + departedPlayers).distinctBy { it.name }
  }
  val filter: (PlayerCard) -> Boolean = { card ->
    (!requirePvPParticipation || card.hasPvPParticipation()) &&
      (!requireGearOver15k || card.lastKnownGearScore > 15000)
  }
  val haranyaFormat = stringResource(Res.string.raid_haranya_faction)
  val nuiaFormat = stringResource(Res.string.raid_nuian_faction)
  val pirateFormat = stringResource(Res.string.raid_pirate_faction)
  val haranyaLabel = haranyaFormat.substringBefore("(").trimEnd()
  val nuiaLabel = nuiaFormat.substringBefore("(").trimEnd()
  val pirateLabel = pirateFormat.substringBefore("(").trimEnd()
  fun chart(label: String, players: List<PlayerCard>, color: Color): FactionComposition {
    val chartFaction = when (label) {
      haranyaLabel -> Faction.HARANYA
      nuiaLabel -> Faction.NUIA
      pirateLabel -> Faction.PIRATE
      else -> Faction.UNKNOWN
    }
    val filtered = sourcePlayers(players, chartFaction).filter(filter)
    val counts = mutableMapOf<SkillTreeType, Int>()
    filtered.forEach { card ->
      SpecType.fromName(card.currentBuild)?.trees?.forEach { tree -> counts[tree] = (counts[tree] ?: 0) + 1 }
    }
    val chartColor = playerFaction.getFactionHighlightColor(chartFaction).takeUnless { it == Color.Transparent } ?: color
    return FactionComposition(label, filtered.size, counts, chartColor)
  }
  val charts = listOf(
    chart(haranyaLabel, nearbyHaranya, factionHighlightColor(Faction.HARANYA)),
    chart(nuiaLabel, nearbyNuia, factionHighlightColor(Faction.NUIA)),
    chart(pirateLabel, nearbyPirate, factionHighlightColor(Faction.PIRATE))
  )
  val factionPlayers = listOf(
    charts[0].factionLabel to sourcePlayers(nearbyHaranya, Faction.HARANYA).filter(filter),
    charts[1].factionLabel to sourcePlayers(nearbyNuia, Faction.NUIA).filter(filter),
    charts[2].factionLabel to sourcePlayers(nearbyPirate, Faction.PIRATE).filter(filter)
  )
  val labels = SKILL_TREE_DISPLAY_ORDER.associateWith { tree ->
    stringResource(when (tree) {
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
    })
  }
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
     Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
       CheckBoxComponent(
         label = stringResource(Res.string.raid_composition_require_pvp),
         initialChecked = requirePvPParticipation,
         onCheckedChange = onRequirePvPParticipationChange,
         textColor = RFColors.TextPrimary
       )
        CheckBoxComponent(
          label = stringResource(Res.string.raid_composition_gear_over_15k),
         initialChecked = requireGearOver15k,
         onCheckedChange = { requireGearOver15k = it },
          textColor = RFColors.TextPrimary
        )
        CheckBoxComponent(
           label = stringResource(Res.string.raid_include_departed),
          initialChecked = includePlayersThatLeftRaid,
          onCheckedChange = { includePlayersThatLeftRaid = it },
          textColor = RFColors.TextPrimary
        )
     }
      ResponsiveFactionSections(factionPlayers, stringResource(Res.string.raid_composition_statistics)) { faction, players ->
       FactionStatistics(faction, players)
     }
      ResponsiveFactionSections(factionPlayers, stringResource(Res.string.raid_composition_meta_spec_breakdown)) { faction, players ->
       MetaSpecBreakdown(faction, players)
     }
   }
}

@Composable
private fun ResponsiveFactionSections(
  factions: List<Pair<String, List<PlayerCard>>>,
  title: String,
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
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_cc), META_CC_SPECS)
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_melee), META_MELEE_SPECS)
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_healer), META_HEALER_SPECS)
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_mage), META_MAGE_SPECS)
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_dancer), META_DANCER_SPECS)
            MetaSpecHelpRow(stringResource(Res.string.raid_composition_meta_ranged), META_RANGED_SPEC)
          }
        }
      }
    }
  }
}

@Composable
private fun MetaSpecHelpRow(label: String, specs: Set<SpecType>) {
  val localizedSpecs = specs.map { stringResource(it.localizedDisplayNameRes) }

  Column {
    Text(label, color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    Text(
      localizedSpecs.joinToString(", "),
      color = RFColors.TextSecondary,
      fontSize = 10.sp,
      lineHeight = 12.sp
    )
  }
}

@Composable
private fun FactionStatistics(faction: String, players: List<PlayerCard>) {
      val specs = players.mapNotNull { SpecType.fromName(it.currentBuild) }
      fun has(tree: SkillTreeType, spec: SpecType) = tree in spec.trees
      fun matching(predicate: (SpecType) -> Boolean) = players.filter { SpecType.fromName(it.currentBuild)?.let(predicate) == true }
      fun row(label: String, yes: Int, total: Int) = CompositionBreakdown(label, yes)
      val battlerage = specs.filter { SkillTreeType.BATTLERAGE in it.trees }
      val dps = specs.filter { it.trees.any { tree -> tree in setOf(SkillTreeType.ARCHERY, SkillTreeType.BATTLERAGE, SkillTreeType.GUNSLINGER, SkillTreeType.MALEDICTION, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE) } }
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
         SpecType.fromName(card.currentBuild)?.let { has(SkillTreeType.BATTLERAGE, it) && (has(SkillTreeType.OCCULTISM, it) || has(SkillTreeType.WITCHCRAFT, it)) } == true
       }
       val battlerageWithoutEither = players.filter { card ->
         SpecType.fromName(card.currentBuild)?.let { has(SkillTreeType.BATTLERAGE, it) && !has(SkillTreeType.OCCULTISM, it) && !has(SkillTreeType.WITCHCRAFT, it) } == true
       }
       val battlerageOccultism = players.filter { card -> SpecType.fromName(card.currentBuild)?.let { has(SkillTreeType.BATTLERAGE, it) && has(SkillTreeType.OCCULTISM, it) } == true }
       val battlerageWitchcraft = players.filter { card -> SpecType.fromName(card.currentBuild)?.let { has(SkillTreeType.BATTLERAGE, it) && has(SkillTreeType.WITCHCRAFT, it) } == true }
       val dpsAuramancy = players.filter { card -> SpecType.fromName(card.currentBuild)?.let { it in dps && has(SkillTreeType.AURAMANCY, it) } == true }
       val dpsWithoutAuramancy = players.filter { card -> SpecType.fromName(card.currentBuild)?.let { it in dps && !has(SkillTreeType.AURAMANCY, it) } == true }
        addRow(stringResource(Res.string.raid_composition_battlerage_occultism_witchcraft), battlerageOccultismOrWitchcraft)
        addRow(stringResource(Res.string.raid_composition_battlerage_without_either), battlerageWithoutEither)
        addRow(stringResource(Res.string.raid_composition_battlerage_occultism), battlerageOccultism)
        addRow(stringResource(Res.string.raid_composition_battlerage_witchcraft), battlerageWitchcraft)
        addRow(stringResource(Res.string.raid_composition_dps_auramancy), dpsAuramancy)
        addRow(stringResource(Res.string.raid_composition_dps_without_auramancy), dpsWithoutAuramancy)
        addRow(stringResource(Res.string.raid_composition_vitalism_confessor), matching { it == SpecType.CONFESSOR })
        addRow(stringResource(Res.string.raid_composition_vitalism_assassin), matching { it == SpecType.ASSASSIN })
        addRow(stringResource(Res.string.raid_composition_vitalism_soothsayer), matching { it == SpecType.SOOTHSAYER })
         addRow(stringResource(Res.string.raid_composition_vitalism_other), matching { has(SkillTreeType.VITALISM, it) && it != SpecType.CONFESSOR && it != SpecType.ASSASSIN })
        addRow(stringResource(Res.string.raid_composition_dancer_comedian), matching { it == SpecType.COMEDIAN })
        addRow(stringResource(Res.string.raid_composition_dancer_seal_resolver), matching { it == SpecType.SEAL_RESOLVER })
        addRow(stringResource(Res.string.raid_composition_dancer_tough_dancer), matching { it == SpecType.TOUGH_DANCER })
         addRow(stringResource(Res.string.raid_composition_dancer_other), matching { has(SkillTreeType.SPELLDANCE, it) && it !in META_DANCER_SPECS })
       val breakdownItems = listOf(
         row(stringResource(Res.string.raid_composition_shadowplay_vitalism), shadowplayVitalism.size, players.size),
         row(stringResource(Res.string.raid_composition_shadowplay_without_vitalism), shadowplayWithoutVitalism.size, players.size),
         row(stringResource(Res.string.raid_composition_battlerage_occultism_witchcraft), battlerageOccultismOrWitchcraft.size, battlerage.size),
         row(stringResource(Res.string.raid_composition_battlerage_without_either), battlerageWithoutEither.size, battlerage.size),
         row(stringResource(Res.string.raid_composition_battlerage_occultism), battlerageOccultism.size, battlerage.size),
         row(stringResource(Res.string.raid_composition_battlerage_witchcraft), battlerageWitchcraft.size, battlerage.size),
         row(stringResource(Res.string.raid_composition_dps_auramancy), dpsAuramancy.size, dps.size),
         row(stringResource(Res.string.raid_composition_dps_without_auramancy), dpsWithoutAuramancy.size, dps.size),
         row(stringResource(Res.string.raid_composition_vitalism_confessor), vitalism.count { it == SpecType.CONFESSOR }, vitalism.size),
         row(stringResource(Res.string.raid_composition_vitalism_assassin), vitalism.count { it == SpecType.ASSASSIN }, vitalism.size),
         row(stringResource(Res.string.raid_composition_vitalism_soothsayer), vitalism.count { it == SpecType.SOOTHSAYER }, vitalism.size),
         row(stringResource(Res.string.raid_composition_vitalism_other), vitalism.count { it != SpecType.CONFESSOR && it != SpecType.ASSASSIN }, vitalism.size),
         row(stringResource(Res.string.raid_composition_dancer_comedian), dancer.count { it == SpecType.COMEDIAN }, dancer.size),
         row(stringResource(Res.string.raid_composition_dancer_seal_resolver), dancer.count { it == SpecType.SEAL_RESOLVER }, dancer.size),
         row(stringResource(Res.string.raid_composition_dancer_tough_dancer), dancer.count { it == SpecType.TOUGH_DANCER }, dancer.size),
         row(stringResource(Res.string.raid_composition_dancer_other), dancer.count { it !in META_DANCER_SPECS }, dancer.size)
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
  val specs = players.mapNotNull { card -> SpecType.fromName(card.currentBuild)?.let { it to card } }
  val groups = listOf(
    stringResource(Res.string.raid_composition_meta_cc) to META_CC_SPECS,
    stringResource(Res.string.raid_composition_meta_melee) to META_MELEE_SPECS,
    stringResource(Res.string.raid_composition_meta_healer) to META_HEALER_SPECS,
    stringResource(Res.string.raid_composition_meta_mage) to META_MAGE_SPECS,
    stringResource(Res.string.raid_composition_meta_dancer) to META_DANCER_SPECS,
    stringResource(Res.string.raid_composition_meta_ranged) to META_RANGED_SPEC
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
      names += mainRaid.flatten().mapNotNull { frame -> frame.playerName.takeIf { it.isNotBlank() } }.filter { it.meetsPvP() }
    }
    if (includeCo) {
      names += coRaid.flatten().mapNotNull { frame -> frame.playerName.takeIf { it.isNotBlank() } }.filter { it.meetsPvP() }
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
  BuffPreset("Light PvP", RaidBuffRequirements(selected = setOf(RaidBuffKey.STATUE_BUFF, RaidBuffKey.GOBLET, RaidBuffKey.WAR_DRUM, RaidBuffKey.FEAST_RIBS))),
  BuffPreset("Serious PvP", RaidBuffRequirements(selected = setOf(RaidBuffKey.STATUE_BUFF, RaidBuffKey.GOBLET, RaidBuffKey.WAR_DRUM, RaidBuffKey.SECRET_GIFT, RaidBuffKey.FEAST_RIBS, RaidBuffKey.JINHUI_WISH, RaidBuffKey.LONGING))),
  BuffPreset("Full Buffed", RaidBuffRequirements(selected = setOf(RaidBuffKey.STATUE_BUFF, RaidBuffKey.GOBLET, RaidBuffKey.WAR_DRUM, RaidBuffKey.SECRET_GIFT, RaidBuffKey.FEAST_RIBS, RaidBuffKey.JINHUI_WISH, RaidBuffKey.LONGING, RaidBuffKey.WHISPER), requireEnhancedLonging = true)),
  BuffPreset("Full-Buff BD PvP", RaidBuffRequirements(selected = setOf(RaidBuffKey.STATUE_BUFF, RaidBuffKey.GOBLET, RaidBuffKey.WAR_DRUM, RaidBuffKey.SECRET_GIFT, RaidBuffKey.FEAST_RIBS, RaidBuffKey.JINHUI_WISH, RaidBuffKey.LONGING, RaidBuffKey.WHISPER, RaidBuffKey.FACTION_WAR_TIME, RaidBuffKey.MONSTER_HUNTERS_DREAM), requireEnhancedLonging = true)),
  BuffPreset("Full-Buff Kraken PvP", RaidBuffRequirements(selected = setOf(RaidBuffKey.STATUE_BUFF, RaidBuffKey.GOBLET, RaidBuffKey.WAR_DRUM, RaidBuffKey.SECRET_GIFT, RaidBuffKey.FEAST_RIBS, RaidBuffKey.JINHUI_WISH, RaidBuffKey.LONGING, RaidBuffKey.WHISPER, RaidBuffKey.DAHUTAS_BUBBLE), requireEnhancedLonging = true)),
  BuffPreset("Uncontested Boss Kill", RaidBuffRequirements(selected = setOf(RaidBuffKey.STATUE_BUFF, RaidBuffKey.GOBLET, RaidBuffKey.WAR_DRUM, RaidBuffKey.SECRET_GIFT, RaidBuffKey.FEAST_RIBS, RaidBuffKey.JINHUI_WISH, RaidBuffKey.LONGING, RaidBuffKey.FACTION_WAR_TIME), lootThreshold = 100))
)

@Composable
private fun BuffsTab(mainRaid: List<List<RaidFramePayload>>, coRaid: List<List<RaidFramePayload>>, buffSelectBorder: Color = Color.White.copy(alpha = 0.06f)) {
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
  val selectedPreset by remember(baseRequirements) {
    mutableStateOf(BUFF_PRESETS.firstOrNull { it.requirements.serialize() == baseRequirements.serialize() } ?: BUFF_PRESETS.first())
  }
  var presetExpanded by remember { mutableStateOf(false) }
  val allMembers = (mainRaid.flatten() + coRaid.flatten()).filter { it.playerName.isNotBlank() }
  val observations = allMembers.associateWith { PlayerCacheInteractor.resolveRaidBuffObservation(it, gracePeriod) }
  val buffed = allMembers.filter { member ->
    val snapshot = observations.getValue(member).snapshot
    snapshot != null && requirements.matches(member.copy(buffs = snapshot.buffIds.map { id ->
      BuffPayload(buff_id = id)
    }))
  }.joinToString(", ") { it.playerName }
  val notBuffed = allMembers.filter { member ->
    val snapshot = observations.getValue(member).snapshot
    snapshot != null && !requirements.matches(member.copy(buffs = snapshot.buffIds.map { id ->
      BuffPayload(buff_id = id)
    }))
  }.joinToString(", ") { it.playerName }
  val notScannable = allMembers.filter { observations.getValue(it).snapshot == null }
    .joinToString(", ") { it.playerName }
  val localizedBuffLabels = RAID_BUFF_DEFINITIONS.associate { definition ->
    definition.key to when (definition.key) {
      RaidBuffKey.GOBLET -> stringResource(Res.string.raid_buff_goblet)
      RaidBuffKey.FEAST_RIBS -> stringResource(Res.string.raid_buff_feast_ribs)
      RaidBuffKey.LONGING -> stringResource(Res.string.raid_buff_longing)
      RaidBuffKey.WHISPER -> stringResource(Res.string.raid_buff_whisper)
      RaidBuffKey.BLESSED_ELIXIR -> stringResource(Res.string.raid_buff_blessed_elixir)
      RaidBuffKey.ANCIENTS_POTION -> stringResource(Res.string.raid_buff_ancients_potion)
      RaidBuffKey.JINHUI_WISH -> stringResource(Res.string.raid_buff_jinhui_wish)
      RaidBuffKey.SECRET_GIFT -> stringResource(Res.string.raid_buff_secret_gift)
      RaidBuffKey.FAIRY_PROTECTION -> stringResource(Res.string.raid_buff_fairy_protection)
      RaidBuffKey.COOKFIRE -> stringResource(Res.string.raid_buff_cookfire)
      RaidBuffKey.WAR_DRUM -> stringResource(Res.string.raid_buff_war_drum)
      RaidBuffKey.DAHUTAS_BUBBLE -> stringResource(Res.string.raid_buff_dahutas_bubble)
      RaidBuffKey.MONSTER_HUNTERS_DREAM -> stringResource(Res.string.raid_buff_monster_hunters_dream)
      RaidBuffKey.RED_FLOWER_FRUIT -> stringResource(Res.string.raid_buff_red_flower_fruit)
      RaidBuffKey.BLUE_FLOWER_FRUIT -> stringResource(Res.string.raid_buff_blue_flower_fruit)
      RaidBuffKey.STATUE_BUFF -> stringResource(Res.string.raid_buff_statue)
      RaidBuffKey.FACTION_WAR_TIME -> stringResource(Res.string.raid_buff_faction_war_time)
      RaidBuffKey.MOONLIGHT_JUICE -> stringResource(Res.string.raid_buff_moonlight_juice)
      RaidBuffKey.HUNTING_ELIXIR -> stringResource(Res.string.raid_buff_hunting_elixir)
      RaidBuffKey.CHOCOLATE -> stringResource(Res.string.raid_buff_chocolate)
      RaidBuffKey.LOOT_CAKE -> stringResource(Res.string.raid_buff_loot_cake)
      RaidBuffKey.SHORTBREAD_COOKIE -> stringResource(Res.string.raid_buff_shortbread_cookie)
      RaidBuffKey.GOLDEN_TAFFY -> stringResource(Res.string.raid_buff_golden_taffy)
      RaidBuffKey.EGG_OF_FORTUNE -> stringResource(Res.string.raid_buff_egg_of_fortune)
    }
  }
  Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Performance banner when buff scanning is disabled
      if (!RFConfig.state.collectAsState().value.performanceRaidBuffScanning) {
        PerformanceDisabledBanner(stringResource(Res.string.performance_buff_scanning_disabled_banner))
      }
      BoxWithConstraints(Modifier.fillMaxWidth()) {
      if (maxWidth >= 760.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BuffRaidPane(mainRaid, coRaid, selectedPlayer, requirements, gracePeriod, { selectedPlayer = it }, { player, offset -> if (player.playerName.isNotBlank()) { selectedPlayer = player; selectedPlayerPopupOffset = offset } }, Modifier.fillMaxWidth())
BuffCopyPane(notBuffed, buffed, notScannable, Modifier.fillMaxWidth())
            LootBuffRankList(allMembers, observations, Modifier.fillMaxWidth())
          }
          BuffControlsPane(requirements, { persistRequirements(it) }, selectedPreset, { persistRequirements(it.requirements) }, presetExpanded, { presetExpanded = !presetExpanded }, gracePeriod, { gp -> RFConfig.update { cfg -> cfg.copy(raidCallerBuffGracePeriod = gp.name) } }, lootThreshold, { v -> RFConfig.update { cfg -> cfg.copy(raidCallerLootBuffThreshold = v) } }, lootEnabled, { com.reoky.raidframer.RaidCallerSync.setLootBuffEnabled(it) }, localizedBuffLabels, Modifier.widthIn(min = 320.dp, max = 390.dp), buffSelectBorder)
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          BuffRaidPane(mainRaid, coRaid, selectedPlayer, requirements, gracePeriod, { selectedPlayer = it }, { player, offset -> if (player.playerName.isNotBlank()) { selectedPlayer = player; selectedPlayerPopupOffset = offset } }, Modifier.fillMaxWidth())
          BuffControlsPane(requirements, { persistRequirements(it) }, selectedPreset, { persistRequirements(it.requirements) }, presetExpanded, { presetExpanded = !presetExpanded }, gracePeriod, { gp -> RFConfig.update { cfg -> cfg.copy(raidCallerBuffGracePeriod = gp.name) } }, lootThreshold, { v -> RFConfig.update { cfg -> cfg.copy(raidCallerLootBuffThreshold = v) } }, lootEnabled, { com.reoky.raidframer.RaidCallerSync.setLootBuffEnabled(it) }, localizedBuffLabels, Modifier.fillMaxWidth(), buffSelectBorder)
          BuffCopyPane(notBuffed, buffed, notScannable, Modifier.fillMaxWidth())
          LootBuffRankList(allMembers, observations, Modifier.fillMaxWidth())
        }
      }
      }
    }
    selectedPlayer?.let { player ->
      val selectedObservation = PlayerCacheInteractor.resolveRaidBuffObservation(player, gracePeriod)
      val observationSnapshot = selectedObservation.snapshot
      val resolvedPlayer = observationSnapshot?.let { player.copy(buffs = it.buffIds.map { id -> BuffPayload(buff_id = id) }) } ?: player
      val observationText = selectedObservation.observedAt?.let { timestamp ->
        if (selectedObservation.isCurrent) {
          stringResource(Res.string.raid_buff_current_scan)
        } else {
          val timeAgoResult = timestamp.timeAgo()
          stringResource(Res.string.raid_buff_last_observed, timeAgoResult.resolveLocalizedString())
        }
      } ?: stringResource(Res.string.raid_buff_none_found)
      Popup(
        popupPositionProvider = object : PopupPositionProvider {
          override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val gap = 16
            val rightX = selectedPlayerPopupOffset.x + gap
            val leftX = selectedPlayerPopupOffset.x - popupContentSize.width - gap
            val maxX = (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8)
            val x = if (rightX <= maxX) rightX else leftX
              .coerceIn(8, maxX)
            val y = (selectedPlayerPopupOffset.y + gap)
              .coerceIn(8, (windowSize.height - popupContentSize.height - 8).coerceAtLeast(8))
            return IntOffset(x, y)
          }
        },
        onDismissRequest = { selectedPlayer = null },
        properties = PopupProperties(focusable = false)
      ) {
        Surface(color = RFColors.PopupBackground.copy(alpha = 0.98f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, RFColors.CardBorder), elevation = 6.dp) {
          Column(Modifier.padding(10.dp).widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val meetsRequirements = requirements.matches(resolvedPlayer)
            val displayName = if (player.playerName.length > 26) player.playerName.take(26) + "\u2026" else player.playerName
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(displayName, color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                  if (meetsRequirements) "(${stringResource(Res.string.raid_buff_passed)})" else "(${stringResource(Res.string.raid_buff_not_passed)})",
                  color = if (meetsRequirements) Color(0xFF7CFF8A) else Color(0xFFFF7777),
                  fontSize = 10.sp
                )
              }
              IconButton(onClick = { selectedPlayer = null }, modifier = Modifier.size(22.dp)) { Text("X", color = RFColors.TextSecondary, fontSize = 11.sp) }
            }
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            Text(stringResource(Res.string.raid_buff_has_buffs), color = Color(0xFF7CFF8A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(requirements.matchedDefinitions(resolvedPlayer).joinToString(", ") { localizedBuffLabels[it.key].orEmpty() }.ifBlank { stringResource(Res.string.raid_buff_none_found) }, color = RFColors.TextSecondary, fontSize = 11.sp)
            Text(stringResource(Res.string.raid_buff_missing_buffs), color = Color(0xFFFF7777), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            val missingBuffText = requirements.missingKeys(resolvedPlayer)
              .mapNotNull { localizedBuffLabels[it] }
              .joinToString(", ")
              .ifBlank { stringResource(Res.string.raid_buff_none_found) }
            Text(missingBuffText, color = RFColors.TextSecondary, fontSize = 11.sp)
            Text(stringResource(Res.string.raid_buff_loot_buffs), color = RFColors.lootBuffColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            // Per-loot-buff breakdown: name + %, descending, with a mini progress bar so
            // the raid lead can see at a glance how heavily a player loot buffed.
            val playerLootBuffs = resolvedPlayer.buffs
              .mapNotNull { buff -> lootBuffById(buff.buff_id) }
              .filter { it.lootPercent > 0 }
              .sortedByDescending { it.lootPercent }
            if (playerLootBuffs.isEmpty()) {
              Text(stringResource(Res.string.raid_buff_none_found), color = RFColors.TextSecondary, fontSize = 11.sp)
            } else {
              val maxLootAmount = playerLootBuffs.first().lootPercent.coerceAtLeast(1)
              playerLootBuffs.forEach { lootBuff ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  Text(lootBuff.name, color = RFColors.TextSecondary, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                  val fraction = (lootBuff.lootPercent.toFloat() / maxLootAmount).coerceIn(0f, 1f)
                  Box(
                    modifier = Modifier
                      .width(44.dp)
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
                  Text("${lootBuff.lootPercent}%", color = RFColors.lootBuffColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
                }
              }
            }
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            Text(observationText, color = RFColors.TextTertiary, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
          }
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
private fun BuffRaidPane(mainRaid: List<List<RaidFramePayload>>, coRaid: List<List<RaidFramePayload>>, selected: RaidFramePayload?, requirements: RaidBuffRequirements, gracePeriod: RaidBuffGracePeriod, onSelect: (RaidFramePayload) -> Unit, onSelectAt: (RaidFramePayload, IntOffset) -> Unit, modifier: Modifier) {
  Column(modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.78f), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.raid_main_raid_label), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        RaidComponent(mainRaid, selectedPlayerName = selected?.playerName, onPlayerClick = onSelect, onPlayerClickAt = onSelectAt, isBuffed = { requirements.matchesResolved(it, gracePeriod) }, isOutOfRange = { it.distance > 115 }, isObservationKnown = { PlayerCacheInteractor.resolveRaidBuffObservation(it, gracePeriod).snapshot != null })
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.raid_coraid_label), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        RaidComponent(coRaid, selectedPlayerName = selected?.playerName, onPlayerClick = onSelect, onPlayerClickAt = onSelectAt, isBuffed = { requirements.matchesResolved(it, gracePeriod) }, isOutOfRange = { it.distance > 115 }, isObservationKnown = { PlayerCacheInteractor.resolveRaidBuffObservation(it, gracePeriod).snapshot != null })
      }
    }
  }
}

@Composable
private fun BuffControlsPane(requirements: RaidBuffRequirements, onRequirements: (RaidBuffRequirements) -> Unit, preset: BuffPreset, onPreset: (BuffPreset) -> Unit, expanded: Boolean, onExpanded: () -> Unit, gracePeriod: RaidBuffGracePeriod, onGracePeriod: (RaidBuffGracePeriod) -> Unit, lootThreshold: Int, onLootThreshold: (Int) -> Unit, lootEnabled: Boolean, onLootEnabled: (Boolean) -> Unit, localizedBuffLabels: Map<RaidBuffKey, String>, modifier: Modifier, borderColor: Color = Color.White.copy(alpha = 0.06f)) {
  Column(modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp)).border(1.dp, borderColor, RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    var graceExpanded by remember { mutableStateOf(false) }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(Res.string.raid_buff_preset), color = Color.White, fontWeight = FontWeight.Bold)
        Box {
          Button(onClick = onExpanded, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White), modifier = Modifier.fillMaxWidth()) { Text(preset.label, color = Color.Black, maxLines = 1) }
          DropdownMenu(expanded = expanded, onDismissRequest = onExpanded) { BUFF_PRESETS.forEach { option -> DropdownMenuItem(onClick = { onPreset(option); onExpanded() }) { Text(option.label) } } }
        }
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(Res.string.raid_buff_grace_period), color = Color.White, fontWeight = FontWeight.Bold)
        Box {
          Button(onClick = { graceExpanded = !graceExpanded }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White), modifier = Modifier.fillMaxWidth()) { Text(gracePeriod.label(), color = Color.Black, maxLines = 1) }
          DropdownMenu(expanded = graceExpanded, onDismissRequest = { graceExpanded = false }) { RaidBuffGracePeriod.entries.forEach { option -> DropdownMenuItem(onClick = { onGracePeriod(option); graceExpanded = false }) { Text(option.label()) } } }
        }
      }
    }
    Text(stringResource(Res.string.raid_buff_requirements), color = Color.White, fontWeight = FontWeight.Bold)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val columns = if (maxWidth >= 360.dp) 2 else 1
      if (columns == 2) {
        FlowRow(maxItemsInEachRow = 2, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
          BuffRequirementCheckboxes(requirements, onRequirements, localizedBuffLabels)
        }
      } else {
        Column { BuffRequirementCheckboxes(requirements, onRequirements, localizedBuffLabels) }
      }
    }
    Text(stringResource(Res.string.raid_buff_loot_section), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
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
  Column(modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(stringResource(Res.string.raid_buff_categories_help_title), color = Color.White, fontWeight = FontWeight.Bold)
      BuffCategoriesHelp()
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(stringResource(Res.string.raid_copy_not_buffed_title), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
      Text(stringResource(Res.string.raid_copy_buffed_title), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      SelectableTextField(value = notBuffed, modifier = Modifier.weight(1f).fillMaxHeight(), minHeight = 0.dp)
      SelectableTextField(value = buffed, modifier = Modifier.weight(1f).fillMaxHeight(), minHeight = 0.dp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(notBuffed), null) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White), modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.raid_copy_not_buffed), color = Color.Black) }
      Button(onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(buffed), null) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White), modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.raid_copy_buffed), color = Color.Black) }
    }
    // Collapsible "Not Scannable" section — players we have no in-grace observation for.
    // Collapses to a single header row to avoid taking up screen real estate.
    Row(Modifier.fillMaxWidth().clickable { notScannableExpanded = !notScannableExpanded }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(stringResource(Res.string.raid_copy_not_scannable_title), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      Text(if (notScannableExpanded) "\u25B2" else "\u25BC", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    if (notScannableExpanded) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableTextField(value = notScannable, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 150.dp), minHeight = 0.dp)
        Button(onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(notScannable), null) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White), modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.raid_copy_not_scannable), color = Color.Black) }
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
            BuffCategoryHelpRow(stringResource(Res.string.raid_copy_buffed_title), stringResource(Res.string.raid_buff_categories_buffed))
            BuffCategoryHelpRow(stringResource(Res.string.raid_copy_not_buffed_title), stringResource(Res.string.raid_buff_categories_not_buffed))
            BuffCategoryHelpRow(stringResource(Res.string.raid_copy_not_scannable_title), stringResource(Res.string.raid_buff_categories_not_scannable))
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
private fun LootBuffRankList(members: List<RaidFramePayload>, observations: Map<RaidFramePayload, RaidBuffObservation>, modifier: Modifier) {
  // Scrollable, max-height breakdown of players sorted by their current summed loot buff %,
  // most → least. Uses the grace-period cached snapshots so players who loot buffed recently
  // (but are now out-of-range / empty) still appear. Rows flow across multiple columns so
  // more players are visible at once. Uses the buff color (gold) from ColorsHelper.
  val ranked = members
    .map { member ->
      val snapshot = observations[member]?.snapshot
      val amount = if (snapshot != null) lootBuffAmountForIds(snapshot.buffIds) else 0
      member to amount
    }
    .filter { it.second > 0 }
    .sortedByDescending { it.second }
  Column(modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Text("$amount%", color = RFColors.lootBuffColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
  }
}

@Composable
private fun BuffRequirementCheckboxes(requirements: RaidBuffRequirements, onRequirements: (RaidBuffRequirements) -> Unit, localizedBuffLabels: Map<RaidBuffKey, String>) {
  RAID_BUFF_DEFINITIONS.filter { it.section == RaidBuffSection.MAIN }.forEach { definition ->
    ControlledCheckbox(localizedBuffLabels[definition.key].orEmpty(), definition.key in requirements.selected) { checked -> onRequirements(requirements.copy(selected = if (checked) requirements.selected + definition.key else requirements.selected - definition.key)) }
  }
  ControlledCheckbox(stringResource(Res.string.raid_buff_orange_goblet), requirements.requireOrangeGoblet) { onRequirements(requirements.copy(requireOrangeGoblet = it, selected = requirements.selected + RaidBuffKey.GOBLET)) }
  ControlledCheckbox(stringResource(Res.string.raid_buff_allow_meatballs), requirements.allowMeatballs) { onRequirements(requirements.copy(allowMeatballs = it)) }
  ControlledCheckbox(stringResource(Res.string.raid_buff_require_enhanced), requirements.requireEnhancedLonging) { onRequirements(requirements.copy(requireEnhancedLonging = it, selected = requirements.selected + RaidBuffKey.LONGING)) }
}

@Composable
private fun ControlledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = Color.White))
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
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()

  val filteredHaranya = if (requirePvPParticipation) nearbyHaranya.filter { it.hasPvPParticipation() } else nearbyHaranya
  val filteredNuia = if (requirePvPParticipation) nearbyNuia.filter { it.hasPvPParticipation() } else nearbyNuia
  val filteredPirate = if (requirePvPParticipation) nearbyPirate.filter { it.hasPvPParticipation() } else nearbyPirate

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
        Text(
          text = String.format(stringResource(Res.string.raid_haranya_faction), filteredHaranya.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = filteredHaranya.mapIndexed { index, card ->
            RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole, gearScore = card.lastKnownGearScore)
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
        Text(
          text = String.format(stringResource(Res.string.raid_nuian_faction), filteredNuia.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = filteredNuia.mapIndexed { index, card ->
            RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole, gearScore = card.lastKnownGearScore)
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
        Text(
          text = String.format(stringResource(Res.string.raid_pirate_faction), filteredPirate.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = filteredPirate.mapIndexed { index, card ->
            RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole, gearScore = card.lastKnownGearScore)
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
    CheckBoxComponent(
      label = stringResource(Res.string.raid_nearby_require_pvp),
      initialChecked = requirePvPParticipation,
      onCheckedChange = onRequirePvPParticipationChange,
      textColor = Color.White
    )
  }
}

private fun averageGearScore(players: List<PlayerCard>): Int {
  val known = players.filter { it.lastKnownGearScore > 0 }
  return if (known.isEmpty()) 0 else known.map { it.lastKnownGearScore }.average().toInt()
}

@Composable
private fun NearbyGearTab(
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  raidDepartures: Map<Int, Set<String>> = emptyMap(),
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()
  var includePlayersThatLeftRaid by rememberSaveable { mutableStateOf(false) }
  val departedNames = remember(raidDepartures) {
    (raidDepartures[0] ?: emptySet()) + (raidDepartures[1] ?: emptySet())
  }
  fun sourcePlayers(players: List<PlayerCard>, faction: Faction): List<PlayerCard> = if (!includePlayersThatLeftRaid) {
    players
  } else {
    val currentNames = players.mapTo(mutableSetOf()) { it.name }
    val departedPlayers = departedNames.mapNotNull { PlayerCacheInteractor.getCard(it) }
      .filter { it.name !in currentNames && Faction.fromString(it.lastKnownFaction) == faction }
    (players + departedPlayers).distinctBy { it.name }
  }

  val filteredHaranya = sourcePlayers(nearbyHaranya, Faction.HARANYA).let { if (requirePvPParticipation) it.filter { card -> card.hasPvPParticipation() } else it }
  val filteredNuia = sourcePlayers(nearbyNuia, Faction.NUIA).let { if (requirePvPParticipation) it.filter { card -> card.hasPvPParticipation() } else it }
  val filteredPirate = sourcePlayers(nearbyPirate, Faction.PIRATE).let { if (requirePvPParticipation) it.filter { card -> card.hasPvPParticipation() } else it }

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
        Text(
          text = String.format(stringResource(Res.string.raid_haranya_faction), filteredHaranya.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = String.format(stringResource(Res.string.nearby_avg_gs_format), avgHaranya, filteredHaranya.count { it.lastKnownGearScore > 0 }),
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
        Text(
          text = String.format(stringResource(Res.string.raid_nuian_faction), filteredNuia.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = String.format(stringResource(Res.string.nearby_avg_gs_format), avgNuia, filteredNuia.count { it.lastKnownGearScore > 0 }),
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
        Text(
          text = String.format(stringResource(Res.string.raid_pirate_faction), filteredPirate.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = String.format(stringResource(Res.string.nearby_avg_gs_format), avgPirate, filteredPirate.count { it.lastKnownGearScore > 0 }),
          color = Color.LightGray,
          fontSize = 11.sp
        )
        GearScoreHistogram(
          players = filteredPirate,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
    CheckBoxComponent(
      label = stringResource(Res.string.raid_nearby_require_pvp),
      initialChecked = requirePvPParticipation,
      onCheckedChange = onRequirePvPParticipationChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_departed),
      initialChecked = includePlayersThatLeftRaid,
      onCheckedChange = { includePlayersThatLeftRaid = it },
      textColor = Color.White
    )
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
