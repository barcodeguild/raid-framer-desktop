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
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PlayerRankingRow
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.RaidComparisonPieChart
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

  val topDebuffs by PlayerCacheInteractor.topDebuff.collectAsState()
  val topCharmers by PlayerCacheInteractor.topCharmers.collectAsState()
  val topGliderGamers by PlayerCacheInteractor.topGliderGamers.collectAsState()
  val topPotters by PlayerCacheInteractor.topPotters.collectAsState()
  val topItemSkillCasters by PlayerCacheInteractor.topItemSkillCasters.collectAsState()
  val topKillsDamage by PlayerCacheInteractor.topKills.collectAsState()
  val topKillsKillingBlow by PlayerCacheInteractor.topKillsKB.collectAsState()
  val topDeaths by PlayerCacheInteractor.topDeaths.collectAsState()
  val topBuffers by PlayerCacheInteractor.topBuffs.collectAsState()

  val humanReadableDateString = DateFormat.getDateInstance(DateFormat.SHORT).format(System.currentTimeMillis())

  var selectedTabIndex by remember { mutableStateOf(0) }
  val tabs = listOf("Buffs / Debuffs", "Kills / Deaths", "Utility / Items")

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
//        .height(256.dp)
        .padding(horizontal = 8.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      RaidComparisonPieChart(
        title = "Total Silences by Raid",
        icon = "\uf57f",
        dataFlow = PlayerCacheInteractor.raidSilenceComparison, // You'll need to create this
        modifier = Modifier.weight(1f)
      )

      RaidComparisonPieChart(
        title = "Total Distresses by Raid",
        icon = "\uf567",
        dataFlow = PlayerCacheInteractor.raidDistressComparison, // You'll need to create this
        modifier = Modifier.weight(1f)
      )

      RaidComparisonPieChart(
        title = "Total Charms by Raid",
        icon = "\uf004",
        dataFlow = PlayerCacheInteractor.raidCharmComparison, // You'll need to create this
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
        0 -> BuffsDebuffsTab(
          topDebuffs = topDebuffs,
          topCharmers = topCharmers,
          topBuffers = topBuffers,
          wm = wm
        )
        1 -> KillsDeathsTab(
          topKillsDamage = topKillsDamage,
          topKillsKillingBlow = topKillsKillingBlow,
          topDeaths = topDeaths,
          wm = wm
        )
        2 -> UtilityItemsTab(
          topPotters = topPotters,
          topGliderGamers = topGliderGamers,
          topItemSkillCasters = topItemSkillCasters,
          wm = wm
        )
      }
    }
  }
}

@Composable
private fun BuffsDebuffsTab(
  topDebuffs: List<PlayerCard>,
  topCharmers: List<PlayerCard>,
  topBuffers: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uf714",
      title = "Debuffs",
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
      title = "Charms",
      cards = topCharmers,
      valueExtractor = { it.sessionCharmTotal.toString() },
      valueColor = Color(0xFFEC407A),
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uf0c1",
      title = "Buffs",
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
  topKillsDamage: List<PlayerCard>,
  topKillsKillingBlow: List<PlayerCard>,
  topDeaths: List<PlayerCard>,
  wm: WindowManager?
) {
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    StatColumn(
      icon = "\uf71e",
      title = "Kills (DMG)",
      cards = topKillsDamage,
      valueExtractor = { it.sessionKillTotal.toString() },
      valueColor = Color(0xFF66BB6A),
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uf0e7",
      title = "Kills (KB)",
      cards = topKillsKillingBlow,
      valueExtractor = { it.sessionKillTotalKB.toString() },
      valueColor = Color(0xFFFFA726),
      modifier = Modifier.weight(1f)
    ) { card ->
      AppState.selectPlayer(card.name)
      wm?.openWindow(OverlayType.PLAYER_CARD)
    }

    StatColumn(
      icon = "\uf54c",
      title = "Deaths",
      cards = topDeaths,
      valueExtractor = { it.sessionDeathTotal.toString() },
      valueColor = Color(0xFFEF5350),
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
      title = "Potions",
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
      title = "Gliders",
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
      title = "Items",
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
