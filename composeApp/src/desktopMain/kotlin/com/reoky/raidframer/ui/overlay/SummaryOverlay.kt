package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.pvpPerformancePoints
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PlayerRankingRow
import com.reoky.raidframer.ui.component.SimpleRankingRow
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.RaidComparisonPieChart
import com.reoky.raidframer.ui.export.ImageExportInteractor
import kotlinx.coroutines.launch
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
import raid_framer_desktop.composeapp.generated.resources.summary_top_buffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_charms
import raid_framer_desktop.composeapp.generated.resources.summary_top_damage_taken
import raid_framer_desktop.composeapp.generated.resources.summary_top_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_distresses
import raid_framer_desktop.composeapp.generated.resources.summary_top_glider_gamers
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_item_uses
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_spells_damage
import raid_framer_desktop.composeapp.generated.resources.summary_top_heals_received
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

  // subscribe to the build count flows
  val buildCountsHaranya by PlayerCacheInteractor.buildCountsHaranya.collectAsState()
  val buildCountsNuia by PlayerCacheInteractor.buildCountsNuia.collectAsState()
  val buildCountsPirate by PlayerCacheInteractor.buildCountsPirate.collectAsState()

  val humanReadableDateString = DateFormat.getDateInstance(DateFormat.SHORT).format(System.currentTimeMillis())

  var selectedTabIndex by remember { mutableStateOf(0) }
  val tabs = listOf(
    "Debuffs",
    "Spells",
    "Buffs",
    "Ode",
    "K/D",
    "Received",
    "Items",
    "Utility",
    "Specs",
    "Performance"
  )

  val scope = rememberCoroutineScope()
  var isExporting by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier.fillMaxWidth()
    ) {
      TitleBarComponent(
        title = stringResource(Res.string.summary_battle_summary_title_format, humanReadableDateString),
        onClose = { wm?.closeWindow(OverlayType.SUMMARY) },
        modifier = Modifier.fillMaxWidth()
      )
    }

    // Charts / Graphs Section
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      RaidComparisonPieChart(
        title = stringResource(Res.string.summary_silences_by_faction),
        icon = "\uf57f",
        dataFlow = PlayerCacheInteractor.factionSilenceComparisonAll,
        modifier = Modifier.weight(1f)
      )
      RaidComparisonPieChart(
        title = stringResource(Res.string.summary_charms_by_faction),
        icon = "\uf004",
        dataFlow = PlayerCacheInteractor.factionCharmComparisonAll,
        modifier = Modifier.weight(1f)
      )
      RaidComparisonPieChart(
        title = stringResource(Res.string.summary_distresses_by_faction),
        icon = "\uf567",
        dataFlow = PlayerCacheInteractor.factionDistressComparisonAll,
        modifier = Modifier.weight(1f)
      )
    }

    // Tab Row
    TabRow(
      selectedTabIndex = selectedTabIndex,
      backgroundColor = RFColors.CardBackground,
      contentColor = Color.White,
      modifier = Modifier.fillMaxWidth()
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = { selectedTabIndex = index },
          text = {
            Text(
              text = title,
              color = if (selectedTabIndex == index) Color.White else RFColors.TextSecondary
            )
          }
        )
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
        0 -> KeyDebuffsTab(
          topSilences = topSilences,
          topCharms = topCharms,
          topDistresses = topDistresses,
          wm = wm
        )
        1 -> SpellDamageByFaction(
          topDamageSpellsHaranya = topDamageSpellsHaranya,
          topDamageSpellsNuia = topDamageSpellsNuia,
          topDamageSpellsPirate = topDamageSpellsPirate,
          wm = wm
        )
        2 -> BuffsDebuffsTab(
          topDebuffs = topDebuffs,
          topSongs = topSongs,
          topBuffers = topBuffers,
          wm = wm
        )
        3 -> OdeTab(
          topOdeHaranya = topOdeHaranya,
          topOdeNuia = topOdeNuia,
          topOdePirate = topOdePirate,
          wm = wm
        )
        4 -> KillsDeathsTab(
          topKillsHaranya = topKillsHaranya,
          topKillsNuia = topKillsNuia,
          topKillsPirate = topKillsPirate,
          wm = wm
        )
        5 -> DamageTakenHealsReceived(
          topDamageTaken = topDamageTaken,
          topHealsReceived = tophealsReceived,
          wm = wm
        )
        6 -> UtilityItemsByFaction(
          topItemUsesHaranya = topItemUsesHaranya,
          topItemUsesNuia = topItemUsesNuia,
          topItemUsesPirate = topItemUsesPirate,
          wm = wm
        )
        7 -> UtilityItemsTab(
          topPotters = topPotters,
          topGliderGamers = topGliderGamers,
          topItemSkillCasters = topItemSkillCasters,
          wm = wm
        )
        8 -> PlayerBuildsTab(
          buildCountsHaranya = buildCountsHaranya,
          buildCountsNuia = buildCountsNuia,
          buildCountsPirate = buildCountsPirate,
          wm = wm
        )
        9 -> PerformanceTab(
          topPerformanceHaranya = topPerformanceHaranya,
          topPerformanceNuia = topPerformanceNuia,
          topPerformancePirate = topPerformancePirate,
          wm = wm
        )
      }
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
      valueColor = Color(0xFFAB47BC),
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
      valueColor = Color(0xFFEC407A),
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
      valueColor = Color(0xFF7E57C2),
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
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
      valueColor = Color(0xFFAB47BC),
      modifier = Modifier.weight(1f)
    ) {}

    SpellStatColumn(
      icon = "\uD83D\uDD25",
      title = stringResource(Res.string.summary_top_nuia_spells_damage),
      spells = topDamageSpellsNuia,
      valueExtractor = { it.total.toLong().humanReadableAbbreviation() },
      valueColor = Color(0xFFEC407A),
      modifier = Modifier.weight(1f)
    ) {}

    SpellStatColumn(
      icon = "\uD83D\uDD25",
      title = stringResource(Res.string.summary_top_pirate_spells_damage),
      spells = topDamageSpellsPirate,
      valueExtractor = { it.total.toLong().humanReadableAbbreviation() },
      valueColor = Color(0xFF7E57C2),
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
      valueColor = Color(0xFFAB47BC),
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
      valueColor = Color(0xFFEC407A),
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
      valueColor = Color(0xFF7E57C2),
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
      icon = "",
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
      icon = "",
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
      icon = "",
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
      valueColor = Color(0xFF66BB6A),
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
      valueColor = Color(0xFFFFA726),
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
      valueColor = Color(0xFFEF5350),
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
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "",
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
      icon = "",
      title = stringResource(Res.string.summary_top_heals_received),
      cards = topHealsReceived,
      valueExtractor = { it.sessionHealsReceivedTotal.toLong().humanReadableAbbreviation() },
      valueColor = RFColors.healsGreen,
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
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
      valueColor = Color(0xFF26A69A),
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uf5b0",
      title = stringResource(Res.string.summary_top_glider_gamers),
      cards = topGliderGamers,
      valueExtractor = { it.sessionGliderTotal.toString() },
      valueColor = Color(0xFF42A5F5),
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
      valueColor = Color(0xFFFFCA28),
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
        text = title.lowercase().capitalize(),
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
          valueColor = valueColor,
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
      valueColor = Color(0xFFAB47BC),
      modifier = Modifier.weight(1f)
    ) { item ->
      // optional click: select or open player/item details if desired
    }

    ItemStatColumn(
      icon = "\uF3A5",
      title = stringResource(Res.string.summary_top_nuia_item_uses),
      items = topItemUsesNuia,
      valueExtractor = { it.count.toString() },
      valueColor = Color(0xFFEC407A),
      modifier = Modifier.weight(1f)
    ) { item -> }

    ItemStatColumn(
      icon = "\uF3A5",
      title = stringResource(Res.string.summary_top_pirate_item_uses),
      items = topItemUsesPirate,
      valueExtractor = { it.count.toString() },
      valueColor = Color(0xFF7E57C2),
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
      valueColor = Color(0xFFAB47BC),
      modifier = Modifier.weight(1f)
    ) { /* optional click */ }

    BuildStatColumn(
      icon = "\u2694",
      title = stringResource(Res.string.summary_nuia_builds),
      builds = buildCountsNuia,
      valueColor = Color(0xFFEC407A),
      modifier = Modifier.weight(1f)
    ) { }

    BuildStatColumn(
      icon = "\u2694",
      title = stringResource(Res.string.summary_pirate_builds),
      builds = buildCountsPirate,
      valueColor = Color(0xFF7E57C2),
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
        SimpleRankingRow(
          index = index,
          name = entry.key.ifBlank { "Unknown" },
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
      valueColor = Color(0xFFAB47BC),
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
      valueColor = Color(0xFFEC407A),
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
      valueColor = Color(0xFF7E57C2),
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }
  }
}

