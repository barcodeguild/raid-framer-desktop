package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.ui.component.PlayerRankingRow
import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.ui.dialog.exitDialog
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.combat_column_pvp_damage
import raid_framer_desktop.composeapp.generated.resources.combat_column_pvp_heals
import raid_framer_desktop.composeapp.generated.resources.combat_column_cc
import raid_framer_desktop.composeapp.generated.resources.combat_no_columns_message
import raid_framer_desktop.composeapp.generated.resources.combat_open_settings

@Preview
@Composable
fun PreviewCombatOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    CombatOverlay()
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CombatOverlay(wm: WindowManager? = null) {

  val shouldShowExitDialog = remember { mutableStateOf(false) }
  exitDialog(shouldShowExitDialog)

  // Collect the sorted lists from the PlayerCacheInteractor
  val sortedDamage by PlayerCacheInteractor.topDamage.collectAsState()
  val sortedHeals by PlayerCacheInteractor.topHeals.collectAsState()
  val sortedCC by PlayerCacheInteractor.topCC.collectAsState()

  // the animation for red flashing text
  val flashingColorState = rememberInfiniteTransition().animateColor(
    initialValue = Color.White,
    targetValue = Color.Red,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  val damageListState = rememberLazyListState()
  val healsListState = rememberLazyListState()
  val ccListState = rememberLazyListState()

  // config state for showing/hiding columns
  val config by RFConfig.state.collectAsState()
  val anyColumnVisibleGlobal by remember { derivedStateOf {
    config.combatShowDamageColumn || config.combatShowHealsColumn || config.combatShowCCColumn
  } }

  // --- Controls fade logic ---
  var isOverlayHovered by remember { mutableStateOf(false) }
  val controlsAlpha by animateFloatAsState(
    targetValue = if (!config.combatControlsFadeEnabled || isOverlayHovered) 1f else 0f,
    animationSpec = tween(durationMillis = 500)
  )

  // --- Sticky Scroll Logic ---
  var isDamageSticky by remember { mutableStateOf(true) }
  LaunchedEffect(damageListState) {
    snapshotFlow { damageListState.isScrollInProgress to damageListState.firstVisibleItemIndex }
      .collect { (isScrolling, index) ->
        if (isScrolling) {
          isDamageSticky = (index == 0)
        } else {
          if (index == 0 && damageListState.firstVisibleItemScrollOffset == 0) isDamageSticky = true
        }
      }
  }
  LaunchedEffect(sortedDamage) { if (isDamageSticky) damageListState.scrollToItem(0) }

  var isHealsSticky by remember { mutableStateOf(true) }
  LaunchedEffect(healsListState) {
    snapshotFlow { healsListState.isScrollInProgress to healsListState.firstVisibleItemIndex }
      .collect { (isScrolling, index) ->
        if (isScrolling) {
          isHealsSticky = (index == 0)
        } else {
          if (index == 0 && healsListState.firstVisibleItemScrollOffset == 0) isHealsSticky = true
        }
      }
  }
  LaunchedEffect(sortedHeals) { if (isHealsSticky) healsListState.scrollToItem(0) }

  var isCCSticky by remember { mutableStateOf(true) }
  LaunchedEffect(ccListState) {
    snapshotFlow { ccListState.isScrollInProgress to ccListState.firstVisibleItemIndex }
      .collect { (isScrolling, index) ->
        if (isScrolling) {
          isCCSticky = (index == 0)
        } else {
          if (index == 0 && ccListState.firstVisibleItemScrollOffset == 0) isCCSticky = true
        }
      }
  }
  LaunchedEffect(sortedCC) { if (isCCSticky) ccListState.scrollToItem(0) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .pointerMoveFilter(
        onEnter = { isOverlayHovered = true; false },
        onExit = { isOverlayHovered = false; false }
      ),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
    ) {
      if (anyColumnVisibleGlobal) {
        // Header row: left icons, centered titles, right icons. Titles centered relative to each other.
        Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 0.dp).zIndex(1f), verticalAlignment = Alignment.CenterVertically) {
          // left icons
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(controlsAlpha)) {
            IconButton(onClick = { shouldShowExitDialog.value = true }, modifier = Modifier.size(32.dp)) {
              val closeInteractionSource = remember { MutableInteractionSource() }
              Text(text = "\uf00d", fontFamily = FontsHelper.faSolid(), fontSize = 16.sp, color = if (closeInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White, modifier = Modifier.hoverable(interactionSource = closeInteractionSource))
            }
            IconButton(onClick = { wm?.openWindow(OverlayType.POKEMON) }, modifier = Modifier.size(32.dp)) {
              val petsInteractionSource = remember { MutableInteractionSource() }
              Text(text = "\uf6d5", fontFamily = FontsHelper.faSolid(), fontSize = 13.sp, color = if (petsInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White, modifier = Modifier.hoverable(interactionSource = petsInteractionSource))
            }
            IconButton(onClick = { wm?.openWindow(OverlayType.RAID) }, modifier = Modifier.size(32.dp)) {
              val raidInteractionSource = remember { MutableInteractionSource() }
              Text(text = "\uf500", fontFamily = FontsHelper.faSolid(), fontSize = 13.sp, color = if (raidInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White, modifier = Modifier.hoverable(interactionSource = raidInteractionSource))
            }
          }

          // center titles area
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
              if (config.combatShowDamageColumn) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(text = stringResource(Res.string.combat_column_pvp_damage), color = Color.White) }
              }
              if (config.combatShowHealsColumn) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(text = stringResource(Res.string.combat_column_pvp_heals), color = Color.White) }
              }
              if (config.combatShowCCColumn) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(text = stringResource(Res.string.combat_column_cc), color = Color.White) }
              }
            }
          }

          // right icons
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(controlsAlpha)) {
            IconButton(onClick = { wm?.openWindow(OverlayType.SUMMARY) }, modifier = Modifier.size(32.dp)) {
              val summaryInteractionSource = remember { MutableInteractionSource() }
              Text(text = "\uf200", fontFamily = FontsHelper.faSolid(), fontSize = 13.sp, color = if (summaryInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White, modifier = Modifier.hoverable(interactionSource = summaryInteractionSource))
            }
            IconButton(onClick = { wm?.openWindow(OverlayType.SETTINGS) }, modifier = Modifier.size(32.dp)) {
              val settingsInteractionSource = remember { MutableInteractionSource() }
              Text(text = "\uf013", fontFamily = FontsHelper.faSolid(), fontSize = 13.sp, color = if (settingsInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White, modifier = Modifier.hoverable(interactionSource = settingsInteractionSource))
            }
            IconButton(onClick = { PlayerCacheInteractor.resetAllSessions() }, modifier = Modifier.size(32.dp)) {
              val plusInteractionSource = remember { MutableInteractionSource() }
              Text(text = "\u002b", fontFamily = FontsHelper.faSolid(), fontSize = 15.sp, color = if (plusInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White, modifier = Modifier.hoverable(interactionSource = plusInteractionSource))
            }
          }
        }

        // Body columns row below header ~ columns fill full width and extend to edges
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
          if (config.combatShowDamageColumn) {
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              LazyColumn(
                contentPadding = PaddingValues(0.dp),
                state = damageListState,
                modifier = Modifier
                  .padding(start = 12.dp, bottom = 6.dp)
                  .fillMaxWidth()
              ) {
                itemsIndexed(sortedDamage, key = { _, card -> card.name }) { index, card ->
                  PlayerRankingRow(
                    index = index,
                    card = card,
                    valueText = card.sessionDamageTotal.humanReadableAbbreviation(),
                    valueColor = RFColors.dpsOrange,
                    isRetribution = card.isBuildingAggression,
                    flashingColor = flashingColorState.value,
                    onClick = {
                      AppState.selectPlayer(card.name)
                      AppState.selectMetricType(GraphMetricType.DAMAGE)
                      wm?.openWindow(OverlayType.PLAYER_CARD)
                    }
                  )
                }
              }
            }
          }

          if (config.combatShowHealsColumn) {
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              LazyColumn(
                contentPadding = PaddingValues(0.dp),
                state = healsListState,
                modifier = Modifier
                  .padding(start = 12.dp, bottom = 6.dp)
                  .fillMaxWidth()
              ) {
                itemsIndexed(sortedHeals, key = { _, card -> card.name }) { index, card ->
                  PlayerRankingRow(
                    index = index,
                    card = card,
                    valueText = card.sessionHealTotal.humanReadableAbbreviation(),
                    valueColor = RFColors.healsGreen,
                    isRetribution = card.isBuildingAggression,
                    flashingColor = flashingColorState.value,
                    onClick = {
                      AppState.selectPlayer(card.name)
                      AppState.selectMetricType(GraphMetricType.HEALING)
                      wm?.openWindow(OverlayType.PLAYER_CARD)
                    }
                  )
                }
              }
            }
          }

          if (config.combatShowCCColumn) {
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              LazyColumn(
                contentPadding = PaddingValues(0.dp),
                state = ccListState,
                modifier = Modifier
                  .padding(start = 12.dp, bottom = 6.dp)
                  .fillMaxWidth()
              ) {
                itemsIndexed(sortedCC, key = { _, card -> card.name }) { index, card ->
                  PlayerRankingRow(
                    index = index,
                    card = card,
                    valueText = card.sessionCCTotal.toString(),
                    valueColor = RFColors.ccCyan,
                    isRetribution = card.isBuildingAggression,
                    flashingColor = flashingColorState.value,
                    onClick = {
                      AppState.selectPlayer(card.name)
                      AppState.selectMetricType(GraphMetricType.CC)
                      wm?.openWindow(OverlayType.PLAYER_CARD)
                    }
                  )
                }
              }
            }
          }
        }
      } else {
        // If all columns are hidden, show a friendly message directing user to settings cause that's hilarious
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .wrapContentHeight(),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(Res.string.combat_no_columns_message), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { wm?.openWindow(OverlayType.SETTINGS) }, colors = ButtonDefaults.buttonColors(Color.White)) {
              Text(text = stringResource(Res.string.combat_open_settings), color = Color.Black)
            }
          }
        }
      }
    }
  }
}
