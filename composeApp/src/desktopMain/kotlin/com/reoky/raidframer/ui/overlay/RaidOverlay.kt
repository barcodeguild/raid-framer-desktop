package com.reoky.raidframer.ui.overlay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import com.reoky.raidframer.ui.component.CompositionBreakdownList
import com.reoky.raidframer.ui.component.FactionComposition
import com.reoky.raidframer.ui.component.GearScoreHistogram
import com.reoky.raidframer.ui.component.RaidComponent
import com.reoky.raidframer.ui.component.SelectableTextField
import com.reoky.raidframer.ui.component.TitleBarComponent
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
  Box(modifier = Modifier.fillMaxSize()) {
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
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
            )
            RaidTab.COMPOSITION -> CompositionTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              playerFaction = playerFaction,
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
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  var requireGearOver15k by rememberSaveable { mutableStateOf(false) }
  val filter: (PlayerCard) -> Boolean = { card ->
    (!requirePvPParticipation || card.hasPvPParticipation()) &&
      (!requireGearOver15k || card.lastKnownGearScore > 15000)
  }
  val haranyaLabel = stringResource(Res.string.raid_haranya_faction).substringBefore(" (%d)")
  val nuiaLabel = stringResource(Res.string.raid_nuian_faction).substringBefore(" (%d)")
  val pirateLabel = stringResource(Res.string.raid_pirate_faction).substringBefore(" (%d)")
  fun chart(label: String, players: List<PlayerCard>, color: Color): FactionComposition {
    val filtered = players.filter(filter)
    val counts = mutableMapOf<SkillTreeType, Int>()
    filtered.forEach { card ->
      SpecType.fromName(card.currentBuild)?.trees?.forEach { tree -> counts[tree] = (counts[tree] ?: 0) + 1 }
    }
    val chartFaction = when (label) {
      haranyaLabel -> Faction.HARANYA
      nuiaLabel -> Faction.NUIA
      pirateLabel -> Faction.PIRATE
      else -> Faction.UNKNOWN
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
    charts[0].factionLabel to nearbyHaranya.filter(filter),
    charts[1].factionLabel to nearbyNuia.filter(filter),
    charts[2].factionLabel to nearbyPirate.filter(filter)
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
       if (maxWidth >= 1120.dp) {
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
         label = "Gear over 15k",
         initialChecked = requireGearOver15k,
         onCheckedChange = { requireGearOver15k = it },
         textColor = RFColors.TextPrimary
       )
     }
     ResponsiveFactionSections(factionPlayers, "Composition statistics") { faction, players ->
       FactionStatistics(faction, players)
     }
     ResponsiveFactionSections(factionPlayers, "Meta spec breakdown") { faction, players ->
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
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(title, color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      if (maxWidth >= 1120.dp) {
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
private fun FactionStatistics(faction: String, players: List<PlayerCard>) {
      val specs = players.mapNotNull { SpecType.fromName(it.currentBuild) }
      fun has(tree: SkillTreeType, spec: SpecType) = tree in spec.trees
      fun row(label: String, yes: Int, total: Int) = CompositionBreakdown(label, yes)
      val battlerage = specs.filter { SkillTreeType.BATTLERAGE in it.trees }
      val dps = specs.filter { it.trees.any { tree -> tree in setOf(SkillTreeType.ARCHERY, SkillTreeType.BATTLERAGE, SkillTreeType.GUNSLINGER, SkillTreeType.MALEDICTION, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE) } }
      val vitalism = specs.filter { SkillTreeType.VITALISM in it.trees }
      val dancer = specs.filter { SkillTreeType.SPELLDANCE in it.trees }
      CompositionBreakdownList(faction, players.size, listOf(
        row("Shadowplay + Vitalism", specs.count { has(SkillTreeType.SHADOWPLAY, it) && has(SkillTreeType.VITALISM, it) }, players.size),
        row("Shadowplay without Vitalism", specs.count { has(SkillTreeType.SHADOWPLAY, it) && !has(SkillTreeType.VITALISM, it) }, players.size),
        row("Battlerage + Occultism or Witchcraft", battlerage.count { has(SkillTreeType.OCCULTISM, it) || has(SkillTreeType.WITCHCRAFT, it) }, battlerage.size),
        row("Battlerage without either", battlerage.count { !has(SkillTreeType.OCCULTISM, it) && !has(SkillTreeType.WITCHCRAFT, it) }, battlerage.size),
        row("Battlerage + Occultism", battlerage.count { has(SkillTreeType.OCCULTISM, it) }, battlerage.size),
        row("Battlerage + Witchcraft", battlerage.count { has(SkillTreeType.WITCHCRAFT, it) }, battlerage.size),
        row("DPS + Auramancy", dps.count { has(SkillTreeType.AURAMANCY, it) }, dps.size),
        row("DPS without Auramancy", dps.count { !has(SkillTreeType.AURAMANCY, it) }, dps.size),
        row("Vitalism: Confessor", vitalism.count { it == SpecType.CONFESSOR }, vitalism.size),
        row("Vitalism: Assassin", vitalism.count { it == SpecType.ASSASSIN }, vitalism.size),
        row("Vitalism: Soothsayer", vitalism.count { it == SpecType.SOOTHSAYER }, vitalism.size),
        row("Vitalism: other", vitalism.count { it != SpecType.CONFESSOR && it != SpecType.ASSASSIN }, vitalism.size),
        row("Dancer: Comedian", dancer.count { it == SpecType.COMEDIAN }, dancer.size),
        row("Dancer: Seal Resolver", dancer.count { it == SpecType.SEAL_RESOLVER }, dancer.size),
        row("Dancer: Tough Dancer", dancer.count { it == SpecType.TOUGH_DANCER }, dancer.size),
        row("Dancer: other", dancer.count { it !in META_DANCER_SPECS }, dancer.size)
      ))
}

@Composable
private fun MetaSpecBreakdown(faction: String, players: List<PlayerCard>) {
  val specs = players.mapNotNull { card -> SpecType.fromName(card.currentBuild)?.let { it to card } }
  val groups = listOf(
    "Meta CC" to META_CC_SPECS,
    "Meta melee" to META_MELEE_SPECS,
    "Meta healer" to META_HEALER_SPECS,
    "Meta mage" to META_MAGE_SPECS,
    "Meta dancer" to META_DANCER_SPECS,
    "Meta ranged" to META_RANGED_SPEC
  )
  val known = groups.flatMap { it.second }.toSet()
  val other = specs.filter { it.first !in known }
  Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
    CompositionBreakdownList(
      title = faction,
      total = players.size,
      items = groups.map { (name, set) -> CompositionBreakdown(name, specs.count { it.first in set }) } +
        CompositionBreakdown("Other", other.size)
    )
    Text(
      text = "Other examples: ${other.map { it.first.name.lowercase().replace('_', ' ') }.ifEmpty { listOf("none") }.joinToString(", ")}",
      color = RFColors.TextTertiary,
      fontSize = 9.sp,
      lineHeight = 11.sp
    )
  }
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
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()

  val filteredHaranya = if (requirePvPParticipation) nearbyHaranya.filter { it.hasPvPParticipation() } else nearbyHaranya
  val filteredNuia = if (requirePvPParticipation) nearbyNuia.filter { it.hasPvPParticipation() } else nearbyNuia
  val filteredPirate = if (requirePvPParticipation) nearbyPirate.filter { it.hasPvPParticipation() } else nearbyPirate

  val avgHaranya = averageGearScore(filteredHaranya)
  val avgNuia = averageGearScore(filteredNuia)
  val avgPirate = averageGearScore(filteredPirate)

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
  }
}
