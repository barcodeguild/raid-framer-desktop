package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PlayerRankingRow
import com.reoky.raidframer.ui.component.SimpleRankingRow
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.RaidComparisonPieChart
import org.jetbrains.compose.resources.stringResource
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
    "K/D",
    "Received",
    "Items",
    "Utility",
    "Specs"
  )

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    TitleBarComponent(
      title = "Battle Summary ($humanReadableDateString)",
      onClose = { wm?.closeWindow(OverlayType.SUMMARY) }
    )

    // Charts / Graphs Section
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      RaidComparisonPieChart(
        title = "Silences by Faction",
        icon = "\uf57f",
        dataFlow = PlayerCacheInteractor.factionSilenceComparisonAll,
        modifier = Modifier.weight(1f)
      )
      RaidComparisonPieChart(
        title = "Charms by Faction",
        icon = "\uf004",
        dataFlow = PlayerCacheInteractor.factionCharmComparisonAll,
        modifier = Modifier.weight(1f)
      )
      RaidComparisonPieChart(
        title = "Distresses by Faction",
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
        3 -> KillsDeathsTab(
          topKillsHaranya = topKillsHaranya,
          topKillsNuia = topKillsNuia,
          topKillsPirate = topKillsPirate,
          wm = wm
        )
        4 -> DamageTakenHealsReceived(
          topDamageTaken = topDamageTaken,
          topHealsReceived = tophealsReceived,
          wm = wm
        )
        5 -> UtilityItemsByFaction(
          topItemUsesHaranya = topItemUsesHaranya,
          topItemUsesNuia = topItemUsesNuia,
          topItemUsesPirate = topItemUsesPirate,
          wm = wm
        )
        6 -> UtilityItemsTab(
          topPotters = topPotters,
          topGliderGamers = topGliderGamers,
          topItemSkillCasters = topItemSkillCasters,
          wm = wm
        )
        7 -> PlayerBuildsTab(
          buildCountsHaranya = buildCountsHaranya,
          buildCountsNuia = buildCountsNuia,
          buildCountsPirate = buildCountsPirate,
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
      title = "Top Silences",
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
      title = "Top Charms",
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
      title = "Top Distresses",
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
      title = "Top Haranya Spells Damage",
      spells = topDamageSpellsHaranya,
      valueExtractor = { it.total.toLong().humanReadableAbbreviation() },
      valueColor = Color(0xFFAB47BC),
      modifier = Modifier.weight(1f)
    ) {}

    SpellStatColumn(
      icon = "\uD83D\uDD25",
      title = "Top Nuia Spells Damage",
      spells = topDamageSpellsNuia,
      valueExtractor = { it.total.toLong().humanReadableAbbreviation() },
      valueColor = Color(0xFFEC407A),
      modifier = Modifier.weight(1f)
    ) {}

    SpellStatColumn(
      icon = "\uD83D\uDD25",
      title = "Top Pirate Spells Damage",
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
      title = "Top Debuffs",
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
      title = "Top Songs",
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
      title = "Top Buffs",
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
      title = "Top Kills Haranya",
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
      title = "Top Kills Nuia",
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
      title = "Top Kills Pirate",
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
      title = "\uD83D\uDD25 Top Damage Taken \uD83D\uDD25",
      cards = topDamageTaken,
      valueExtractor = { it.sessionDamageTakenTotal.toLong().humanReadableAbbreviation() },
      valueColor = Color(0xFFEF5350),
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "",
      title = "\uD83D\uDC89 Top Heals Received \uD83D\uDC89",
      cards = topHealsReceived,
      valueExtractor = { it.sessionHealsReceivedTotal.toLong().humanReadableAbbreviation() },
      valueColor = Color(0xFF66BB6A),
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
      title = "Top Potion Drinkers",
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
      title = "Top Glider Gamers",
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
      title = "Most Item Usages",
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
    }

    // Scrollable List
    LazyColumn(
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      itemsIndexed(cards, key = { _, card -> card.name }) { index, card ->
        PlayerRankingRow(
          index = index,
          card = card,
          valueText = valueExtractor(card),
          valueColor = valueColor,
          isRetribution = card.isBuildingAggression,
          flashingColor = Color.Red,
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
      title = "Top Haranya Item Uses",
      items = topItemUsesHaranya,
      valueExtractor = { it.count.toString() },
      valueColor = Color(0xFFAB47BC),
      modifier = Modifier.weight(1f)
    ) { item ->
      // optional click: select or open player/item details if desired
    }

    ItemStatColumn(
      icon = "\uF3A5",
      title = "Top Nuia Item Uses",
      items = topItemUsesNuia,
      valueExtractor = { it.count.toString() },
      valueColor = Color(0xFFEC407A),
      modifier = Modifier.weight(1f)
    ) { item -> }

    ItemStatColumn(
      icon = "\uF3A5",
      title = "Top Pirate Item Uses",
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
    }

    // Scrollable List of items
    LazyColumn(
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      // use a stable key; StringResource.toString() is fine for uniqueness here
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
      title = "Haranya Builds",
      builds = buildCountsHaranya,
      valueColor = Color(0xFFAB47BC),
      modifier = Modifier.weight(1f)
    ) { /* optional click */ }

    BuildStatColumn(
      icon = "\u2694",
      title = "Nuia Builds",
      builds = buildCountsNuia,
      valueColor = Color(0xFFEC407A),
      modifier = Modifier.weight(1f)
    ) { }

    BuildStatColumn(
      icon = "\u2694",
      title = "Pirate Builds",
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
        text = title.lowercase().capitalize(),
        color = Color.White,
        textAlign = TextAlign.Center
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

