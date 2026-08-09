package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.pvpPerformancePoints
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PlayerRankingRow
import com.reoky.raidframer.ui.component.SimpleRankingRow
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.RaidComparisonPieChart
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.summary_battle_summary_title_format
import raid_framer_desktop.composeapp.generated.resources.summary_charms_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_distresses_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_haranya_builds
import raid_framer_desktop.composeapp.generated.resources.summary_most_item_usages
import raid_framer_desktop.composeapp.generated.resources.summary_nuia_builds
import raid_framer_desktop.composeapp.generated.resources.summary_pirate_builds
import raid_framer_desktop.composeapp.generated.resources.summary_silences_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_tab_buffs
import raid_framer_desktop.composeapp.generated.resources.summary_tab_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_tab_items
import raid_framer_desktop.composeapp.generated.resources.summary_tab_kd
import raid_framer_desktop.composeapp.generated.resources.summary_tab_ode
import raid_framer_desktop.composeapp.generated.resources.summary_tab_performance
import raid_framer_desktop.composeapp.generated.resources.summary_tab_life_mend
import raid_framer_desktop.composeapp.generated.resources.summary_top_life_mends
import raid_framer_desktop.composeapp.generated.resources.summary_tab_received
import raid_framer_desktop.composeapp.generated.resources.summary_tab_specs
import raid_framer_desktop.composeapp.generated.resources.summary_tab_spells
import raid_framer_desktop.composeapp.generated.resources.summary_tab_utility
import raid_framer_desktop.composeapp.generated.resources.summary_tab_faction_charts
import raid_framer_desktop.composeapp.generated.resources.summary_tab_cc_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_tab_utility_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_tab_glider_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_tab_special_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_trips_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_bubbles_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_bracings_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_shield_strip_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_weapon_disables_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_potion_disables_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_bd_glider_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_crystal_wings_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_glider_disables_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_provokes_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_tiger_strikes_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_freezes_by_faction
import raid_framer_desktop.composeapp.generated.resources.summary_tab_new_buffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_trips
import raid_framer_desktop.composeapp.generated.resources.summary_top_bubbles
import raid_framer_desktop.composeapp.generated.resources.summary_top_bracings
import raid_framer_desktop.composeapp.generated.resources.summary_top_shield_strip
import raid_framer_desktop.composeapp.generated.resources.summary_top_weapon_disables
import raid_framer_desktop.composeapp.generated.resources.summary_top_potion_disables
import raid_framer_desktop.composeapp.generated.resources.summary_top_bd_glider
import raid_framer_desktop.composeapp.generated.resources.summary_top_crystal_wings
import raid_framer_desktop.composeapp.generated.resources.summary_top_glider_disables
import raid_framer_desktop.composeapp.generated.resources.summary_top_provokes
import raid_framer_desktop.composeapp.generated.resources.summary_top_tiger_strikes
import raid_framer_desktop.composeapp.generated.resources.summary_top_freezes
import raid_framer_desktop.composeapp.generated.resources.summary_top_buffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_charms
import raid_framer_desktop.composeapp.generated.resources.summary_top_damage_taken
import raid_framer_desktop.composeapp.generated.resources.summary_top_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_defiance
import raid_framer_desktop.composeapp.generated.resources.summary_top_distresses
import raid_framer_desktop.composeapp.generated.resources.summary_top_garden_defiance
import raid_framer_desktop.composeapp.generated.resources.summary_top_glider_gamers
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_item_uses
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_spells_damage
import raid_framer_desktop.composeapp.generated.resources.summary_top_heals_received
import raid_framer_desktop.composeapp.generated.resources.summary_top_heal_ratio
import raid_framer_desktop.composeapp.generated.resources.summary_top_kills_haranya
import raid_framer_desktop.composeapp.generated.resources.summary_top_kills_nuia
import raid_framer_desktop.composeapp.generated.resources.summary_top_kills_pirate
import raid_framer_desktop.composeapp.generated.resources.summary_top_nuia_item_uses
import raid_framer_desktop.composeapp.generated.resources.summary_top_nuia_spells_damage
import raid_framer_desktop.composeapp.generated.resources.summary_top_ode_haranya
import raid_framer_desktop.composeapp.generated.resources.summary_top_ode_nuia
import raid_framer_desktop.composeapp.generated.resources.summary_top_ode_pirate
import raid_framer_desktop.composeapp.generated.resources.summary_top_pirate_item_uses
import raid_framer_desktop.composeapp.generated.resources.summary_top_pirate_spells_damage
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_performance
import raid_framer_desktop.composeapp.generated.resources.summary_top_nuia_performance
import raid_framer_desktop.composeapp.generated.resources.summary_top_pirate_performance
import raid_framer_desktop.composeapp.generated.resources.summary_top_potion_drinkers
import raid_framer_desktop.composeapp.generated.resources.summary_top_purges
import raid_framer_desktop.composeapp.generated.resources.summary_top_sac_dances
import raid_framer_desktop.composeapp.generated.resources.summary_top_silences
import raid_framer_desktop.composeapp.generated.resources.summary_top_songs
import java.text.DateFormat

@Preview
@Composable
fun PreviewSummaryOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    SummaryOverlay()
  }
}

@Composable
fun SummaryOverlay(wm: WindowManager? = null) {

  val topSilences by PlayerCacheInteractor.topSilences.collectAsState()
  val topCharms by PlayerCacheInteractor.topCharms.collectAsState()
  val topDistresses by PlayerCacheInteractor.topDistresses.collectAsState()
  val topDamageSpellsHaranya by PlayerCacheInteractor.topDamageSpellsHaranya.collectAsState()
  val topDamageSpellsNuia by PlayerCacheInteractor.topDamageSpellsNuia.collectAsState()
  val topDamageSpellsPirate by PlayerCacheInteractor.topDamageSpellsPirate.collectAsState()
  val topItemUsesHaranya by PlayerCacheInteractor.topItemUsesHaranya.collectAsState()
  val topItemUsesNuia by PlayerCacheInteractor.topItemUsesNuia.collectAsState()
  val topItemUsesPirate by PlayerCacheInteractor.topItemUsesPirate.collectAsState()
  val topDebuffs by PlayerCacheInteractor.topDebuff.collectAsState()
  val topSongs by PlayerCacheInteractor.topSongs.collectAsState()
  val topGliderGamers by PlayerCacheInteractor.topGliderGamers.collectAsState()
  val topPotters by PlayerCacheInteractor.topPotters.collectAsState()
  val topItemSkillCasters by PlayerCacheInteractor.topItemSkillCasters.collectAsState()
  val topKillsHaranya by PlayerCacheInteractor.topKillsHaranya.collectAsState()
  val topKillsNuia by PlayerCacheInteractor.topKillsNuia.collectAsState()
  val topKillsPirate by PlayerCacheInteractor.topKillsPirate.collectAsState()

  val topPerformanceHaranya by PlayerCacheInteractor.topPerformanceHaranya.collectAsState()
  val topPerformanceNuia by PlayerCacheInteractor.topPerformanceNuia.collectAsState()
  val topPerformancePirate by PlayerCacheInteractor.topPerformancePirate.collectAsState()

  val topOdeHaranya by PlayerCacheInteractor.topOdeHaranya.collectAsState()
  val topOdeNuia by PlayerCacheInteractor.topOdeNuia.collectAsState()
  val topOdePirate by PlayerCacheInteractor.topOdePirate.collectAsState()

//  val topKillsDamage by PlayerCacheInteractor.topKills.collectAsState()
//  val topKillsKillingBlow by PlayerCacheInteractor.topKillsKB.collectAsState()
//  val topKillsLifetime by PlayerCacheInteractor.topKillsLifetime.collectAsState()
  val topDamageTaken by PlayerCacheInteractor.topDamageTaken.collectAsState()
  val tophealsReceived by PlayerCacheInteractor.topHealsReceived.collectAsState()
  val topBuffers by PlayerCacheInteractor.topBuffs.collectAsState()
  val topLifeMenders by PlayerCacheInteractor.topLifeMenders.collectAsState()

  // subscribe to the build count flows
  val buildCountsHaranya by PlayerCacheInteractor.buildCountsHaranya.collectAsState()
  val buildCountsNuia by PlayerCacheInteractor.buildCountsNuia.collectAsState()
  val buildCountsPirate by PlayerCacheInteractor.buildCountsPirate.collectAsState()

  // New debuff category rankings
  val topTigerStrikes by PlayerCacheInteractor.topTigerStrikes.collectAsState()
  val topFreezes by PlayerCacheInteractor.topFreezes.collectAsState()
  val topTrips by PlayerCacheInteractor.topTrips.collectAsState()
  val topBubbles by PlayerCacheInteractor.topBubbles.collectAsState()
  val topBracings by PlayerCacheInteractor.topBracings.collectAsState()
  val topShieldStrip by PlayerCacheInteractor.topShieldStrip.collectAsState()
  val topWeaponDisables by PlayerCacheInteractor.topWeaponDisables.collectAsState()
  val topPotionDisables by PlayerCacheInteractor.topPotionDisables.collectAsState()
  val topBdGlider by PlayerCacheInteractor.topBdGlider.collectAsState()
  val topCrystalWings by PlayerCacheInteractor.topCrystalWings.collectAsState()
  val topGliderDisables by PlayerCacheInteractor.topGliderDisables.collectAsState()
  val topProvoked by PlayerCacheInteractor.topProvoked.collectAsState()
  val topDefiance by PlayerCacheInteractor.topDefiance.collectAsState()
  val topGardenDefiance by PlayerCacheInteractor.topGardenDefiance.collectAsState()
  val topPurges by PlayerCacheInteractor.topPurges.collectAsState()
  val topSacDances by PlayerCacheInteractor.topSacDances.collectAsState()

  // New faction comparison flows
  val factionTigerStrikeData by PlayerCacheInteractor.factionTigerStrikeComparisonAll.collectAsState()
  val factionFreezeData by PlayerCacheInteractor.factionFreezeComparisonAll.collectAsState()
  val factionTripsData by PlayerCacheInteractor.factionTripsComparisonAll.collectAsState()
  val factionBubblesData by PlayerCacheInteractor.factionBubblesComparisonAll.collectAsState()
  val factionBracingsData by PlayerCacheInteractor.factionBracingsComparisonAll.collectAsState()
  val factionShieldStripData by PlayerCacheInteractor.factionShieldStripComparisonAll.collectAsState()
  val factionWeaponDisablesData by PlayerCacheInteractor.factionWeaponDisablesComparisonAll.collectAsState()
  val factionPotionDisablesData by PlayerCacheInteractor.factionPotionDisablesComparisonAll.collectAsState()
  val factionBdGliderData by PlayerCacheInteractor.factionBdGliderComparisonAll.collectAsState()
  val factionCrystalWingsData by PlayerCacheInteractor.factionCrystalWingsComparisonAll.collectAsState()
  val factionGliderDisablesData by PlayerCacheInteractor.factionGliderDisablesComparisonAll.collectAsState()
  val factionProvokedData by PlayerCacheInteractor.factionProvokedComparisonAll.collectAsState()

  val humanReadableDateString = DateFormat.getDateInstance(DateFormat.SHORT).format(System.currentTimeMillis())

  var selectedTabIndex by remember { mutableStateOf(0) }
  val tabs = listOf(
    stringResource(Res.string.summary_tab_faction_charts),
    stringResource(Res.string.summary_tab_debuffs),
    stringResource(Res.string.summary_tab_cc_debuffs),
    stringResource(Res.string.summary_tab_utility_debuffs),
    stringResource(Res.string.summary_tab_glider_debuffs),
    stringResource(Res.string.summary_tab_special_debuffs),
    stringResource(Res.string.summary_tab_new_buffs),
    stringResource(Res.string.summary_tab_spells),
    stringResource(Res.string.summary_tab_buffs),
    stringResource(Res.string.summary_tab_ode),
    stringResource(Res.string.summary_tab_kd),
    stringResource(Res.string.summary_tab_received),
    stringResource(Res.string.summary_tab_items),
    stringResource(Res.string.summary_tab_utility),
    stringResource(Res.string.summary_tab_specs),
    stringResource(Res.string.summary_tab_performance),
    stringResource(Res.string.summary_tab_life_mend)
  )

  val scope = rememberCoroutineScope()
  var isExporting by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    // Title bar with pagination arrows and dropdown selector
    Box(modifier = Modifier.fillMaxWidth()) {
      TitleBarComponent(
        title = stringResource(Res.string.summary_battle_summary_title_format, humanReadableDateString),
        onClose = { wm?.closeWindow(OverlayType.SUMMARY) },
        modifier = Modifier.fillMaxWidth()
      )
      
      // Navigation controls - centered vertically, positioned to the left of the close button
      var dropdownExpanded by remember { mutableStateOf(false) }

      Row(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .offset(y = (-1).dp)
          .padding(end = 43.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Back arrow
        TextButton(
          onClick = {
            selectedTabIndex = if (selectedTabIndex > 0) selectedTabIndex - 1 else tabs.size - 1
          },
          colors = ButtonDefaults.textButtonColors(
            contentColor = RFColors.TextSecondary
          ),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
          Text(
            text = "\u25C0",
            fontSize = 10.sp,
            color = RFColors.TextSecondary
          )
        }

        // Dropdown button
        TextButton(
          onClick = { dropdownExpanded = true },
          colors = ButtonDefaults.textButtonColors(
            contentColor = RFColors.TextPrimary
          )
        ) {
          Text(
            text = tabs[selectedTabIndex],
            fontSize = 11.sp
          )
          Text(
            text = " \u25BC",
            fontSize = 9.sp,
            color = RFColors.TextSecondary
          )
        }

        // Forward arrow
        TextButton(
          onClick = {
            selectedTabIndex = if (selectedTabIndex < tabs.size - 1) selectedTabIndex + 1 else 0
          },
          colors = ButtonDefaults.textButtonColors(
            contentColor = RFColors.TextSecondary
          ),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
          Text(
            text = "\u25B6",
            fontSize = 10.sp,
            color = RFColors.TextSecondary
          )
        }

        DropdownMenu(
          expanded = dropdownExpanded,
          onDismissRequest = { dropdownExpanded = false },
          modifier = Modifier
            .background(RFColors.CardBackground, RoundedCornerShape(0.dp)),
          shape = RoundedCornerShape(0.dp)
        ) {
          tabs.forEachIndexed { index, title ->
            DropdownMenuItem(
              onClick = {
                selectedTabIndex = index
                dropdownExpanded = false
              },
              content = {
                Text(
                  text = title,
                  color = if (selectedTabIndex == index) RFColors.AccentRed else RFColors.TextPrimary,
                  fontSize = 12.sp
                )
              }
            )
          }
        }
      }
    }

    // Tab Content
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
      when (selectedTabIndex) {
        0 -> FactionChartsTab(
          factionTripsData = factionTripsData,
          factionBubblesData = factionBubblesData,
          factionBracingsData = factionBracingsData,
          factionShieldStripData = factionShieldStripData,
          factionWeaponDisablesData = factionWeaponDisablesData,
          factionPotionDisablesData = factionPotionDisablesData,
          factionBdGliderData = factionBdGliderData,
          factionCrystalWingsData = factionCrystalWingsData,
          factionGliderDisablesData = factionGliderDisablesData,
          factionProvokedData = factionProvokedData,
          factionTigerStrikeData = factionTigerStrikeData,
          factionFreezeData = factionFreezeData
        )
        1 -> KeyDebuffsTab(
          topSilences = topSilences,
          topCharms = topCharms,
          topDistresses = topDistresses,
          wm = wm
        )
        2 -> CCDebuffsTab(
          topTrips = topTrips,
          topBubbles = topBubbles,
          topBracings = topBracings,
          wm = wm
        )
        3 -> UtilityDebuffsTab(
          topShieldStrip = topShieldStrip,
          topWeaponDisables = topWeaponDisables,
          topPotionDisables = topPotionDisables,
          wm = wm
        )
        4 -> GliderDebuffsTab(
          topBdGlider = topBdGlider,
          topCrystalWings = topCrystalWings,
          topGliderDisables = topGliderDisables,
          wm = wm
        )
        5 -> SpecialDebuffsTab(
          topProvoked = topProvoked,
          topTigerStrikes = topTigerStrikes,
          topFreezes = topFreezes,
          wm = wm
        )
         6 -> NewBuffsTab(topDefiance, topGardenDefiance, topPurges, topSacDances, wm)
         7 -> SpellDamageByFaction(
          topDamageSpellsHaranya = topDamageSpellsHaranya,
          topDamageSpellsNuia = topDamageSpellsNuia,
          topDamageSpellsPirate = topDamageSpellsPirate,
          wm = wm
        )
         8 -> BuffsDebuffsTab(
          topDebuffs = topDebuffs,
          topSongs = topSongs,
          topBuffers = topBuffers,
          wm = wm
        )
         9 -> OdeTab(
          topOdeHaranya = topOdeHaranya,
          topOdeNuia = topOdeNuia,
          topOdePirate = topOdePirate,
          wm = wm
        )
         10 -> KillsDeathsTab(
          topKillsHaranya = topKillsHaranya,
          topKillsNuia = topKillsNuia,
          topKillsPirate = topKillsPirate,
          wm = wm
        )
         11 -> DamageTakenHealsReceived(
          topDamageTaken = topDamageTaken,
          topHealsReceived = tophealsReceived,
          wm = wm
        )
         12 -> UtilityItemsByFaction(
          topItemUsesHaranya = topItemUsesHaranya,
          topItemUsesNuia = topItemUsesNuia,
          topItemUsesPirate = topItemUsesPirate,
          wm = wm
        )
         13 -> UtilityItemsTab(
          topPotters = topPotters,
          topGliderGamers = topGliderGamers,
          topItemSkillCasters = topItemSkillCasters,
          wm = wm
        )
         14 -> PlayerBuildsTab(
          buildCountsHaranya = buildCountsHaranya,
          buildCountsNuia = buildCountsNuia,
          buildCountsPirate = buildCountsPirate,
          wm = wm
        )
         15 -> PerformanceTab(
          topPerformanceHaranya = topPerformanceHaranya,
          topPerformanceNuia = topPerformanceNuia,
          topPerformancePirate = topPerformancePirate,
          wm = wm
        )
         16 -> LifeMendTab(
          topLifeMenders = topLifeMenders,
          wm = wm
        )
      }
    }
  }
}

@Composable
private fun FactionChartsTab(
  factionTripsData: Map<String, Float>,
  factionBubblesData: Map<String, Float>,
  factionBracingsData: Map<String, Float>,
  factionShieldStripData: Map<String, Float>,
  factionWeaponDisablesData: Map<String, Float>,
  factionPotionDisablesData: Map<String, Float>,
  factionBdGliderData: Map<String, Float>,
  factionCrystalWingsData: Map<String, Float>,
  factionGliderDisablesData: Map<String, Float>,
  factionProvokedData: Map<String, Float>,
  factionTigerStrikeData: Map<String, Float>,
  factionFreezeData: Map<String, Float>
) {
  // Standardized faction colors for pie charts (from game's perspective system)
  val factionColors = mapOf(
    "Haranya" to Color(0xFF36F1CC),  // Allied/teal color
    "Nuia" to Color.Red.copy(alpha = 0.75f),  // Enemy/red color
    "Pirate" to Color(0xFFE56CAB)  // Pirate pink color
  )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Row 1: Silences, Charms, Distresses
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_silences_by_faction),
          icon = "\uf57f",
          dataFlow = PlayerCacheInteractor.factionSilenceComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_charms_by_faction),
          icon = "\uf004",
          dataFlow = PlayerCacheInteractor.factionCharmComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_distresses_by_faction),
          icon = "\uf567",
          dataFlow = PlayerCacheInteractor.factionDistressComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
      }
    }
    // Row 2: Trips, Bubbles, Bracings
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_trips_by_faction),
          icon = "\u2193",
          dataFlow = PlayerCacheInteractor.factionTripsComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_bubbles_by_faction),
          icon = "\u25CF",
          dataFlow = PlayerCacheInteractor.factionBubblesComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_bracings_by_faction),
          icon = "\u27A1",
          dataFlow = PlayerCacheInteractor.factionBracingsComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
      }
    }
    // Row 3: Shield Strip, Weapon Disables, Potion Disables
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_shield_strip_by_faction),
          icon = "\u2694",
          dataFlow = PlayerCacheInteractor.factionShieldStripComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_weapon_disables_by_faction),
          icon = "\u2620",
          dataFlow = PlayerCacheInteractor.factionWeaponDisablesComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_potion_disables_by_faction),
          icon = "\u2697",
          dataFlow = PlayerCacheInteractor.factionPotionDisablesComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
      }
    }
    // Row 4: BD Glider, Crystal Wings, Glider Disables
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_bd_glider_by_faction),
          icon = "\u2708",
          dataFlow = PlayerCacheInteractor.factionBdGliderComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_crystal_wings_by_faction),
          icon = "\u2708",
          dataFlow = PlayerCacheInteractor.factionCrystalWingsComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_glider_disables_by_faction),
          icon = "\u2708",
          dataFlow = PlayerCacheInteractor.factionGliderDisablesComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
      }
    }
    // Row 5: Provokes, Tiger Strikes, Freezes
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_provokes_by_faction),
          icon = "\u2757",
          dataFlow = PlayerCacheInteractor.factionProvokedComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_tiger_strikes_by_faction),
          icon = "\u26A1",
          dataFlow = PlayerCacheInteractor.factionTigerStrikeComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
        RaidComparisonPieChart(
          title = stringResource(Res.string.summary_freezes_by_faction),
          icon = "\u2744",
          dataFlow = PlayerCacheInteractor.factionFreezeComparisonAll,
          modifier = Modifier.weight(1f),
          factionColors = factionColors
        )
      }
    }
  }
}

@Composable
private fun CCDebuffsTab(
  topTrips: List<PlayerCard>,
  topBubbles: List<PlayerCard>,
  topBracings: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\u2193",
      title = stringResource(Res.string.summary_top_trips),
      cards = topTrips,
      valueExtractor = { it.sessionTripsTotal.toString() },
      valueColor = RFColors.tripsAmber,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u25CF",
      title = stringResource(Res.string.summary_top_bubbles),
      cards = topBubbles,
      valueExtractor = { it.sessionBubblesTotal.toString() },
      valueColor = RFColors.bubblesCyan,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u27A1",
      title = stringResource(Res.string.summary_top_bracings),
      cards = topBracings,
      valueExtractor = { it.sessionBracingsTotal.toString() },
      valueColor = RFColors.bracingsGreen,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun UtilityDebuffsTab(
  topShieldStrip: List<PlayerCard>,
  topWeaponDisables: List<PlayerCard>,
  topPotionDisables: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\u2694",
      title = stringResource(Res.string.summary_top_shield_strip),
      cards = topShieldStrip,
      valueExtractor = { it.sessionShieldStripTotal.toString() },
      valueColor = RFColors.shieldStripOrange,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u2620",
      title = stringResource(Res.string.summary_top_weapon_disables),
      cards = topWeaponDisables,
      valueExtractor = { it.sessionWeaponDisablesTotal.toString() },
      valueColor = RFColors.weaponDisablesRed,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u2697",
      title = stringResource(Res.string.summary_top_potion_disables),
      cards = topPotionDisables,
      valueExtractor = { it.sessionPotionDisablesTotal.toString() },
      valueColor = RFColors.potionDisablesPurple,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun GliderDebuffsTab(
  topBdGlider: List<PlayerCard>,
  topCrystalWings: List<PlayerCard>,
  topGliderDisables: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\u2708",
      title = stringResource(Res.string.summary_top_bd_glider),
      cards = topBdGlider,
      valueExtractor = { it.sessionBdGliderTotal.toString() },
      valueColor = RFColors.bdGliderTeal,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u2708",
      title = stringResource(Res.string.summary_top_crystal_wings),
      cards = topCrystalWings,
      valueExtractor = { it.sessionCrystalWingsTotal.toString() },
      valueColor = RFColors.crystalWingsBlue,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u2708",
      title = stringResource(Res.string.summary_top_glider_disables),
      cards = topGliderDisables,
      valueExtractor = { it.sessionGliderDisablesTotal.toString() },
      valueColor = RFColors.gliderDisablesPink,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun SpecialDebuffsTab(
  topProvoked: List<PlayerCard>,
  topTigerStrikes: List<PlayerCard>,
  topFreezes: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\u2757",
      title = stringResource(Res.string.summary_top_provokes),
      cards = topProvoked,
      valueExtractor = { it.sessionProvokedTotal.toString() },
      valueColor = RFColors.provokesDeepPurple,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u26A1",
      title = stringResource(Res.string.summary_top_tiger_strikes),
      cards = topTigerStrikes,
      valueExtractor = { it.sessionTigerStrikeTotal.toString() },
      valueColor = RFColors.techNoTigerStrikes,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\u2744",
      title = stringResource(Res.string.summary_top_freezes),
      cards = topFreezes,
      valueExtractor = { it.sessionFreezeTotal.toString() },
      valueColor = RFColors.freezeIceBlue,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun KeyDebuffsTab(
  topSilences: List<PlayerCard>,
  topCharms: List<PlayerCard>,
  topDistresses: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uf714",
      title = stringResource(Res.string.summary_top_silences),
      cards = topSilences,
      valueExtractor = { it.sessionSilenceTotal.toString() },
      valueColor = RFColors.factionHaranya,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\uf004",
      title = stringResource(Res.string.summary_top_charms),
      cards = topCharms,
      valueExtractor = { it.sessionCharmTotal.toString() },
      valueColor = RFColors.factionNuia,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
    StatColumn(
      icon = "\uf0c1",
      title = stringResource(Res.string.summary_top_distresses),
      cards = topDistresses,
      valueExtractor = { it.sessionDistressTotal.toString() },
      valueColor = RFColors.factionPirate,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun NewBuffsTab(
  topDefiance: List<PlayerCard>,
  topGardenDefiance: List<PlayerCard>,
  topPurges: List<PlayerCard>,
  topSacDances: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(modifier = Modifier.fillMaxSize()) {
    listOf(
      Triple("\u2694 " + stringResource(Res.string.summary_top_defiance), topDefiance, RFColors.defianceGold),
      Triple("\u2600 " + stringResource(Res.string.summary_top_garden_defiance), topGardenDefiance, RFColors.gardenDefianceBlue),
      Triple("\u2728 " + stringResource(Res.string.summary_top_purges), topPurges, RFColors.purgeGreen),
      Triple("\u2665 " + stringResource(Res.string.summary_top_sac_dances), topSacDances, RFColors.sacDancePurple)
    ).forEach { (title, cards, color) ->
      StatColumn(
        icon = "*",
        title = title,
        cards = cards,
        valueExtractor = { card ->
          when (cards) {
            topDefiance -> card.sessionDefianceTotal.toString()
            topGardenDefiance -> card.sessionGardenDefianceTotal.toString()
            topPurges -> card.sessionPurgeTotal.toString()
            else -> card.sessionSacDanceTotal.toString()
          }
        },
        valueColor = color,
        modifier = Modifier.weight(1f)
      ) { card ->
        AppState.selectPlayer(card.name)
        wm?.openWindow(OverlayType.PLAYER_CARD)
      }
    }
  }
}

@Composable
private fun SpellDamageByFaction(
  topDamageSpellsHaranya: List<PlayerCacheInteractor.SpellDamage>,
  topDamageSpellsNuia: List<PlayerCacheInteractor.SpellDamage>,
  topDamageSpellsPirate: List<PlayerCacheInteractor.SpellDamage>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    SpellStatColumn(
      icon = "\uD83D\uDD25", // dmg icon?
      title = stringResource(Res.string.summary_top_haranya_spells_damage),
      spells = topDamageSpellsHaranya,
      valueExtractor = { it.total.toLong().humanReadableAbbreviation() },
      valueColor = RFColors.factionHaranya,
      modifier = Modifier.weight(1f)
    ) {}

    SpellStatColumn(
      icon = "\uD83D\uDD25",
      title = stringResource(Res.string.summary_top_nuia_spells_damage),
      spells = topDamageSpellsNuia,
      valueExtractor = { it.total.toLong().humanReadableAbbreviation() },
      valueColor = RFColors.factionNuia,
      modifier = Modifier.weight(1f)
    ) {}

    SpellStatColumn(
      icon = "\uD83D\uDD25",
      title = stringResource(Res.string.summary_top_pirate_spells_damage),
      spells = topDamageSpellsPirate,
      valueExtractor = { it.total.toLong().humanReadableAbbreviation() },
      valueColor = RFColors.factionPirate,
      modifier = Modifier.weight(1f)
    ) {}
  }
}

@Composable
private fun BuffsDebuffsTab(
  topDebuffs: List<PlayerCard>,
  topSongs: List<PlayerCard>,
  topBuffers: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uf714",
      title = stringResource(Res.string.summary_top_debuffs),
      cards = topDebuffs,
      valueExtractor = { it.sessionDebuffTotal.toString() },
      valueColor = RFColors.factionHaranya,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uf004",
      title = stringResource(Res.string.summary_top_songs),
      cards = topSongs,
      valueExtractor = { it.sessionSongsTotal.toString() },
      valueColor = RFColors.factionNuia,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uf0c1",
      title = stringResource(Res.string.summary_top_buffs),
      cards = topBuffers,
      valueExtractor = { it.sessionBuffTotal.toString() },
      valueColor = RFColors.factionPirate,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun OdeTab(
  topOdeHaranya: List<PlayerCard>,
  topOdeNuia: List<PlayerCard>,
  topOdePirate: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uD83C\uDFB5",
      title = stringResource(Res.string.summary_top_ode_haranya),
      cards = topOdeHaranya,
      valueExtractor = { it.sessionOdeHealsTotal.humanReadableAbbreviation() },
      valueColor = RFColors.healsGreen,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uD83C\uDFB5",
      title = stringResource(Res.string.summary_top_ode_nuia),
      cards = topOdeNuia,
      valueExtractor = { it.sessionOdeHealsTotal.humanReadableAbbreviation() },
      valueColor =  RFColors.healsGreen,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uD83C\uDFB5",
      title = stringResource(Res.string.summary_top_ode_pirate),
      cards = topOdePirate,
      valueExtractor = { it.sessionOdeHealsTotal.humanReadableAbbreviation() },
      valueColor =  RFColors.healsGreen,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun KillsDeathsTab(
  topKillsHaranya: List<PlayerCard>,
  topKillsNuia: List<PlayerCard>,
  topKillsPirate: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uF54C",
      title = stringResource(Res.string.summary_top_kills_haranya),
      cards = topKillsHaranya,
      valueExtractor = { it.sessionKillTotal.toString() },
      valueColor = RFColors.killsHaranyaGreen,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uF54C",
      title = stringResource(Res.string.summary_top_kills_nuia),
      cards = topKillsNuia,
      valueExtractor = { it.sessionKillTotal.toString() },
      valueColor = RFColors.killsNuiaOrange,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uF54C",
      title = stringResource(Res.string.summary_top_kills_pirate),
      cards = topKillsPirate,
      valueExtractor = { it.sessionKillTotal.toString() },
      valueColor = RFColors.killsPirateRed,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun DamageTakenHealsReceived(
  topDamageTaken: List<PlayerCard>,
  topHealsReceived: List<PlayerCard>,
  wm: WindowManager?
) {
  // Calculate heal ratio data (damage taken -> heals received ratio)
  val healRatioData = remember(topDamageTaken) {
    topDamageTaken
      .filter { it.sessionDamageTakenTotal > 0 }
      .sortedByDescending { it.sessionDamageTakenTotal }
      .map { card ->
        val ratio = if (card.sessionDamageTakenTotal > 0) {
          card.sessionHealsReceivedTotal.toFloat() / card.sessionDamageTakenTotal.toFloat()
        } else 0f
        card to ratio
      }
  }

  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uD83D\uDD25",
      title = stringResource(Res.string.summary_top_damage_taken),
      cards = topDamageTaken,
      valueExtractor = { it.sessionDamageTakenTotal.toLong().humanReadableAbbreviation() },
      valueColor = RFColors.dpsOrange,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uD83D\uDC89",
      title = stringResource(Res.string.summary_top_heals_received),
      cards = topHealsReceived,
      valueExtractor = { it.sessionHealsReceivedTotal.toLong().humanReadableAbbreviation() },
      valueColor = RFColors.healsGreen,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uD83E\uDE78",
      title = stringResource(Res.string.summary_top_heal_ratio),
      cards = healRatioData.map { it.first },
      valueExtractor = { card ->
        val ratio = healRatioData.find { it.first.name == card.name }?.second ?: 0f
        "${(ratio * 100).toInt()}%"
      },
      valueColor = Color.White,
      modifier = Modifier.weight(1f),
      colorExtractor = { card ->
        val ratio = healRatioData.find { it.first.name == card.name }?.second ?: 0f
        healRatioColor(ratio)
      }
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

/**
 * Maps a heal ratio (0.0 = 0% healed, 1.0 = 100% healed) to a color gradient:
 * Red (0%) -> Orange (25%) -> Yellow (50%) -> Green (75%) -> Cyan (100%+)
 */
private fun healRatioColor(ratio: Float): Color {
  val clamped = ratio.coerceIn(0f, 1.5f)
  return when {
    clamped < 0.25f -> {
      val t = clamped / 0.25f
      Color(
        (255 * (1 - t * 0.38)).toInt(),
        (50 * t).toInt(),
        0,
        255
      )
    }
    clamped < 0.5f -> {
      val t = (clamped - 0.25f) / 0.25f
      Color(
        (153 + 102 * t).toInt(),
        (50 + 171 * t).toInt(),
        0,
        255
      )
    }
    clamped < 1.0f -> {
      val t = (clamped - 0.5f) / 0.5f
      Color(
        255,
        (221 + 34 * t).toInt(),
        (intArrayOf(0, 50, 100, 150, 200, 230).getOrElse((t * 5).toInt()) { 230 }),
        255
      )
    }
    else -> Color(0, 230, 255, 255) // Cyan for over-healed
  }
}

@Composable
private fun UtilityItemsTab(
  topPotters: List<PlayerCard>,
  topGliderGamers: List<PlayerCard>,
  topItemSkillCasters: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uf0c3",
      title = stringResource(Res.string.summary_top_potion_drinkers),
      cards = topPotters,
      valueExtractor = { it.sessionPotionTotal.toString() },
      valueColor = RFColors.potionTeal,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\u2708",
      title = stringResource(Res.string.summary_top_glider_gamers),
      cards = topGliderGamers,
      valueExtractor = { it.sessionGliderTotal.toString() },
      valueColor = RFColors.gliderBlue,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uf6d1",
      title = stringResource(Res.string.summary_most_item_usages),
      cards = topItemSkillCasters,
      valueExtractor = { it.sessionItemSkillTotal.toString() },
      valueColor = RFColors.itemSkillYellow,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun StatColumn(
  icon: String,
  title: String,
  cards: List<PlayerCard>,
  valueExtractor: (PlayerCard) -> String,
  valueColor: Color,
  modifier: Modifier = Modifier,
  colorExtractor: ((PlayerCard) -> Color)? = null, // Optional dynamic color per card
  onClick: (PlayerCard) -> Unit
) {
  val config by RFConfig.state.collectAsState()
  Column(
    modifier = modifier
      .fillMaxHeight()
      .padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(end = 4.dp)
      )
      Text(
        text = title,
        color = Color.White,
        textAlign = TextAlign.Center
      )
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(start = 4.dp)
      )
    }

    LazyColumn(
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      itemsIndexed(cards, key = { _, card -> "${card.name}:${card.lastKnownFaction}:${card.currentBuild}" }) { index, card ->
        PlayerRankingRow(
          index = index,
          card = card,
          valueText = valueExtractor(card),
          valueColor = colorExtractor?.invoke(card) ?: valueColor,
          isRetribution = card.isBuildingAggression,
          flashingColor = Color.Red,
          isOwnCharacter = card.name == config.playerName,
          onClick = { onClick(card) }
        )
      }
    }
  }
}

// kotlin
@Composable
private fun UtilityItemsByFaction(
  topItemUsesHaranya: List<PlayerCacheInteractor.ItemUsage>,
  topItemUsesNuia: List<PlayerCacheInteractor.ItemUsage>,
  topItemUsesPirate: List<PlayerCacheInteractor.ItemUsage>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    ItemStatColumn(
      icon = "\uF3A5",
      title = stringResource(Res.string.summary_top_haranya_item_uses),
      items = topItemUsesHaranya,
      valueExtractor = { it.count.toString() },
      valueColor = RFColors.factionHaranya,
      modifier = Modifier.weight(1f)
    ) { item ->
      // optional click: select or open player/item details if desired
    }

    ItemStatColumn(
      icon = "\uF3A5",
      title = stringResource(Res.string.summary_top_nuia_item_uses),
      items = topItemUsesNuia,
      valueExtractor = { it.count.toString() },
      valueColor = RFColors.factionNuia,
      modifier = Modifier.weight(1f)
    ) { item -> }

    ItemStatColumn(
      icon = "\uF3A5",
      title = stringResource(Res.string.summary_top_pirate_item_uses),
      items = topItemUsesPirate,
      valueExtractor = { it.count.toString() },
      valueColor = RFColors.factionPirate,
      modifier = Modifier.weight(1f)
    ) { item -> }
  }
}

@Composable
private fun ItemStatColumn(
  icon: String,
  title: String,
  items: List<PlayerCacheInteractor.ItemUsage>,
  valueExtractor: (PlayerCacheInteractor.ItemUsage) -> String,
  valueColor: Color,
  modifier: Modifier = Modifier,
  onClick: (PlayerCacheInteractor.ItemUsage) -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header (unchanged)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(end = 4.dp)
      )
      Text(
        text = title,
        color = Color.White,
        textAlign = TextAlign.Center
      )
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(start = 4.dp)
      )
    }

    // Scrollable List of items
    LazyColumn(
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      itemsIndexed(items, key = { _, i -> i.itemName.toString() }) { index, item ->
        SimpleRankingRow(
          index = index,
          name = stringResource(item.itemName),
          valueText = valueExtractor(item),
          valueColor = valueColor,
          onClick = { onClick(item) }
        )
      }
    }
  }
}

@Composable
private fun SpellStatColumn(
  icon: String,
  title: String,
  spells: List<PlayerCacheInteractor.SpellDamage>,
  valueExtractor: (PlayerCacheInteractor.SpellDamage) -> String,
  valueColor: Color,
  modifier: Modifier = Modifier,
  onClick: (PlayerCacheInteractor.SpellDamage) -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(end = 4.dp)
      )
      Text(
        text = title,
        color = Color.White,
        textAlign = TextAlign.Center
      )
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(start = 4.dp)
      )
    }

    // Scrollable List of spells
    LazyColumn(
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      itemsIndexed(spells, key = { _, s -> s.spell }) { index, spell ->
        SimpleRankingRow(
          index = index,
          name = spell.spell,
          valueText = valueExtractor(spell),
          valueColor = valueColor,
          onClick = { onClick(spell) }
        )
      }
    }
  }
}

// New: Player Builds tab and helper column
@Composable
private fun PlayerBuildsTab(
  buildCountsHaranya: Map<String, Int>,
  buildCountsNuia: Map<String, Int>,
  buildCountsPirate: Map<String, Int>,
  wm: WindowManager?
) {
  Row(modifier = Modifier.fillMaxSize()) {
    BuildStatColumn(
      icon = "\u2694",
      title = stringResource(Res.string.summary_haranya_builds),
      builds = buildCountsHaranya,
      valueColor = RFColors.factionHaranya,
      modifier = Modifier.weight(1f)
    ) { /* optional click */ }

    BuildStatColumn(
      icon = "\u2694",
      title = stringResource(Res.string.summary_nuia_builds),
      builds = buildCountsNuia,
      valueColor = RFColors.factionNuia,
      modifier = Modifier.weight(1f)
    ) { }

    BuildStatColumn(
      icon = "\u2694",
      title = stringResource(Res.string.summary_pirate_builds),
      builds = buildCountsPirate,
      valueColor = RFColors.factionPirate,
      modifier = Modifier.weight(1f)
    ) { }
  }
}

@Composable
private fun BuildStatColumn(
  icon: String,
  title: String,
  builds: Map<String, Int>,
  valueColor: Color,
  modifier: Modifier = Modifier,
  onClick: (Pair<String, Int>) -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(end = 4.dp)
      )
      Text(
        text = title,
        color = Color.White,
        textAlign = TextAlign.Center
      )
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 14.sp,
        color = Color.White,
        modifier = Modifier.padding(start = 4.dp)
      )
    }

    LazyColumn(
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      val sorted = builds.entries.sortedByDescending { it.value }
      itemsIndexed(sorted, key = { _, entry -> entry.key }) { index, entry ->
        val displayName = entry.key.let { raw ->
          SpecType.fromName(raw)?.let { stringResource(it.localizedDisplayNameRes) } ?: raw.ifBlank { "Unknown" }
        }
        SimpleRankingRow(
          index = index,
          name = displayName,
          valueText = entry.value.toString(),
          valueColor = valueColor,
          onClick = { onClick(entry.key to entry.value) }
        )
      }
    }
  }
}

@Composable
private fun PerformanceTab(
  topPerformanceHaranya: List<PlayerCard>,
  topPerformanceNuia: List<PlayerCard>,
  topPerformancePirate: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uD83C\uDFC6",
      title = stringResource(Res.string.summary_top_haranya_performance),
      cards = topPerformanceHaranya,
      valueExtractor = { it.pvpPerformancePoints().toString() },
      valueColor = RFColors.factionHaranya,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uD83C\uDFC6",
      title = stringResource(Res.string.summary_top_nuia_performance),
      cards = topPerformanceNuia,
      valueExtractor = { it.pvpPerformancePoints().toString() },
      valueColor = RFColors.factionNuia,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uD83C\uDFC6",
      title = stringResource(Res.string.summary_top_pirate_performance),
      cards = topPerformancePirate,
      valueExtractor = { it.pvpPerformancePoints().toString() },
      valueColor = RFColors.factionPirate,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

@Composable
private fun LifeMendTab(
  topLifeMenders: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\u2764",
      title = stringResource(Res.string.summary_top_life_mends),
      cards = topLifeMenders,
      valueExtractor = { card ->
        "${card.lifeMendTotal} (${card.lifeMendAverage.toLong().humanReadableAbbreviation()} - ${card.lifeMendQuality.label})"
      },
      valueColor = Color.White,
      colorExtractor = { it.lifeMendQuality.color },
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

