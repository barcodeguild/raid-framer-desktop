package com.reoky.raidframer.ui.overlay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.hasPvPParticipation
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
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
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
import com.reoky.raidframer.ui.component.PlayerListTooltipComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
private enum class RaidTab { ATTENDANCE, NEARBY, NEARBY_GEAR, COMPOSITION }
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
  var requirePvPParticipation by rememberSaveable { mutableStateOf(false) }
  var raidWasDetected by remember { mutableStateOf(false) }
  if (!raidWasDetected && (mainRaid.value.isNotEmpty() || coRaid.value.isNotEmpty())) {
    raidWasDetected = true
  }
  Box(modifier = Modifier.fillMaxSize().background(Color(0xCC121212))) {
    if (mainRaid.value.isEmpty() && coRaid.value.isEmpty() && !raidWasDetected) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(Res.string.raid_no_raid_detected),
          color = Color.LightGray,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              CoroutineScope(Dispatchers.Main).launch {
                CompanionInteractor.sendMessage(IPCMessagePayload.TestPing())
              }
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
          ) {
            Text(stringResource(Res.string.raid_refresh_button), color = Color.Black)
          }
          Button(
            onClick = { wm?.closeWindow(OverlayType.RAID) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
          ) {
            Text(text = stringResource(Res.string.raid_close), color = Color.Black)
          }
        }
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
  val haranyaLabel = haranyaFormat.substringBefore("%d").trimEnd()
  val nuiaLabel = nuiaFormat.substringBefore("%d").trimEnd()
  val pirateLabel = pirateFormat.substringBefore("%d").trimEnd()
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
    chart(haranyaLabel, nearbyHaranya, RFColors.factionHaranya),
    chart(nuiaLabel, nearbyNuia, RFColors.factionNuia),
    chart(pirateLabel, nearbyPirate, RFColors.factionPirate)
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
        androidx.compose.material.Surface(
          color = RFColors.PopupBackground.copy(alpha = 0.98f),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, RFColors.CardBorder),
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
        androidx.compose.material.Surface(
          color = RFColors.CardBackground.copy(alpha = 0.78f),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, RFColors.CardBorder),
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
     androidx.compose.material.Surface(
       color = RFColors.CardBackground.copy(alpha = 0.78f),
       shape = RoundedCornerShape(8.dp),
       border = androidx.compose.foundation.BorderStroke(1.dp, RFColors.CardBorder),
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
      if (mainRaid.isNotEmpty()) {
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
      }
      if (coRaid.isNotEmpty()) {
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
      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
  val haranyaLabel = stringResource(Res.string.raid_haranya_faction).substringBefore("%d").trimEnd()
  val nuiaLabel = stringResource(Res.string.raid_nuian_faction).substringBefore("%d").trimEnd()
  val pirateLabel = stringResource(Res.string.raid_pirate_faction).substringBefore("%d").trimEnd()

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
