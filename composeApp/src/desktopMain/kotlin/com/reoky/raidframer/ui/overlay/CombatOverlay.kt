package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.interactor.CombatLogInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.core.model.CombatRankingCategory
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helper.UpdateHelper
import org.jetbrains.compose.resources.stringResource
import com.reoky.raidframer.ui.component.PlayerRankingRow
import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.ui.dialog.exitDialog
import com.reoky.raidframer.ui.dialog.updateDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.combat_column_pvp_damage
import raid_framer_desktop.composeapp.generated.resources.combat_column_pvp_heals
import raid_framer_desktop.composeapp.generated.resources.combat_column_pvp_cc
import raid_framer_desktop.composeapp.generated.resources.combat_column_pve_damage
import raid_framer_desktop.composeapp.generated.resources.combat_column_pve_heals
import raid_framer_desktop.composeapp.generated.resources.combat_column_pve_cc
import raid_framer_desktop.composeapp.generated.resources.combat_no_columns_message
import raid_framer_desktop.composeapp.generated.resources.combat_open_settings
import raid_framer_desktop.composeapp.generated.resources.combat_press_plus_to_record
import raid_framer_desktop.composeapp.generated.resources.combat_update_tooltip

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

@Composable
fun CombatOverlay(wm: WindowManager? = null) {

  val shouldShowExitDialog = remember { mutableStateOf(false) }
  exitDialog(shouldShowExitDialog)

  // Update dialog — shown once on startup if an update is available
  val shouldShowUpdateDialog = remember { mutableStateOf(false) }
  val pendingUpdate by UpdateHelper.pendingUpdate.collectAsState()
  LaunchedEffect(pendingUpdate) {
    if (pendingUpdate != null && !shouldShowUpdateDialog.value) {
      shouldShowUpdateDialog.value = true
    }
  }
  updateDialog(
    shouldShowUpdateDialog = shouldShowUpdateDialog,
    updateInfo = pendingUpdate,
    onDownloadAndInstall = {
      UpdateHelper.shouldScrollToUpdate = true
      wm?.openWindow(OverlayType.SETTINGS)
    }
  )

  // Collect the sorted lists from the PlayerCacheInteractor
  val sortedDamage by PlayerCacheInteractor.topDamage.collectAsState()
  val sortedHeals by PlayerCacheInteractor.topHeals.collectAsState()
  val sortedCC by PlayerCacheInteractor.topCC.collectAsState()

  // config state for showing/hiding columns
  val config by RFConfig.state.collectAsState()

  val customCategories = remember { mutableStateListOf<Pair<CombatRankingCategory, StateFlow<List<PlayerCard>>>>() }
  LaunchedEffect(config.combatCustomCategory1, config.combatCustomCategory2, config.combatCustomCategory3) {
    customCategories.clear()
    listOf(
      config.combatCustomCategory1,
      config.combatCustomCategory2,
      config.combatCustomCategory3
    ).forEach { categoryName ->
      if (categoryName.isNotBlank()) {
        CombatRankingCategory.fromString(categoryName)?.let { category ->
          customCategories.add(category to PlayerCacheInteractor.getRankingFlow(category))
        }
      }
    }
  }

  val customListStates = remember { mutableStateMapOf<String, LazyListState>() }
  LaunchedEffect(config.combatCustomCategory1, config.combatCustomCategory2, config.combatCustomCategory3) {
    val neededKeys = customCategories.map { it.first.name }.toSet()
    val existingKeys = customListStates.keys.toSet()
    (existingKeys - neededKeys).forEach { key -> customListStates.remove(key) }
    (neededKeys - existingKeys).forEach { key -> customListStates[key] = LazyListState() }
  }

  var isCustomStickyMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
  customCategories.forEach { (category, flow) ->
    val listState = customListStates[category.name] ?: return@forEach
    val categoryName = category.name
    LaunchedEffect(categoryName) {
      snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
        .collect { (isScrolling, index) ->
          isCustomStickyMap = isCustomStickyMap + (categoryName to if (isScrolling) false else (index == 0 && listState.firstVisibleItemScrollOffset == 0))
        }
    }
    LaunchedEffect(categoryName) {
      if (isCustomStickyMap[categoryName] != false) listState.scrollToItem(0)
    }
  }

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

  val damageColumnText = stringResource(
    if (config.allowPVEDamage) Res.string.combat_column_pve_damage else Res.string.combat_column_pvp_damage
  )
  val healsColumnText = stringResource(
    if (config.allowPVEDamage) Res.string.combat_column_pve_heals else Res.string.combat_column_pvp_heals
  )
  val ccColumnText = stringResource(
    if (config.allowPVEDamage) Res.string.combat_column_pve_cc else Res.string.combat_column_pvp_cc
  )

  val anyColumnVisibleGlobal by remember { derivedStateOf {
    config.combatShowDamageColumn || config.combatShowHealsColumn || config.combatShowCCColumn || customCategories.isNotEmpty()
  } }

  // --- Controls fade logic ---
  // Use hoverable + collectIsHoveredAsState so no experimental API is needed
  val overlayInteractionSource = remember { MutableInteractionSource() }
  val isOverlayHovered by overlayInteractionSource.collectIsHoveredAsState()
  val controlsAlpha by animateFloatAsState(
    targetValue = if (!config.combatControlsFadeEnabled || isOverlayHovered) 1f else 0f,
    animationSpec = tween(durationMillis = 500)
  )
  // Animate the title padding to match the icon row width (3 × 32dp = 96dp) so titles
  // expand into the space the icons occupied as they fade out.
  val controlsPaddingFloat by animateFloatAsState(
    targetValue = if (!config.combatControlsFadeEnabled || isOverlayHovered) 96f else 0f,
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

  Box(
    modifier = Modifier
      .fillMaxSize()
      .hoverable(interactionSource = overlayInteractionSource)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
    ) {
      if (anyColumnVisibleGlobal) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 0.dp)
            .zIndex(1f)
        ) {
          // center titles — full width, padded to avoid icons when visible
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = controlsPaddingFloat.dp)
              .align(Alignment.Center),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (config.combatShowDamageColumn) {
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(text = damageColumnText, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (config.combatShowHealsColumn) {
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(text = healsColumnText, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (config.combatShowCCColumn) {
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(text = ccColumnText, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            customCategories.forEach { (category, _) ->
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                  Text(text = category.icon, color = Color.White, fontFamily = FontsHelper.faSolid(), fontSize = 13.sp)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = stringResource(category.displayNameRes), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = category.icon, color = Color.White, fontFamily = FontsHelper.faSolid(), fontSize = 13.sp)
                }
              }
            }
          }

          // left icons — overlaid at start edge, never affects title layout
          Row(modifier = Modifier.align(Alignment.CenterStart).alpha(controlsAlpha), verticalAlignment = Alignment.CenterVertically) {
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

          // right icons — overlaid at end edge, never affects title layout
          Row(modifier = Modifier.align(Alignment.CenterEnd).alpha(controlsAlpha), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { wm?.openWindow(OverlayType.SUMMARY) }, modifier = Modifier.size(32.dp)) {
              val summaryInteractionSource = remember { MutableInteractionSource() }
              Text(text = "\uf200", fontFamily = FontsHelper.faSolid(), fontSize = 13.sp, color = if (summaryInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White, modifier = Modifier.hoverable(interactionSource = summaryInteractionSource))
            }
            // Settings cog — turns yellow when an update is available, with hover tooltip
            var showSettingsTooltip by remember { mutableStateOf(false) }
            Box {
              val settingsInteractionSource = remember { MutableInteractionSource() }
              val isSettingsHovered by settingsInteractionSource.collectIsHoveredAsState()
              val hasUpdate = pendingUpdate != null
              val cogColor = when {
                isSettingsHovered -> Color.Red
                hasUpdate -> RFColors.UpdateGold
                else -> Color.White
              }
              IconButton(onClick = { wm?.openWindow(OverlayType.SETTINGS) }, modifier = Modifier.size(32.dp)) {
                Text(text = "\uf013", fontFamily = FontsHelper.faSolid(), fontSize = 13.sp, color = cogColor, modifier = Modifier.hoverable(interactionSource = settingsInteractionSource))
              }
              if (hasUpdate && isSettingsHovered) {
                Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 36)) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    elevation = 4.dp,
                    color = Color.Black.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color.Gray)
                  ) {
                    Text(
                      text = stringResource(Res.string.combat_update_tooltip, pendingUpdate!!.version),
                      color = Color.White,
                      modifier = Modifier.padding(6.dp),
                      fontSize = 11.sp
                    )
                  }
                }
              }
            }
            IconButton(onClick = {
              if (CombatLogInteractor.isRecording.value) {
                PlayerCacheInteractor.stopSession()
                RFConfig.update { it.copy(lastSessionStart = 0L) }
              } else {
                wm?.openWindow(OverlayType.NEW_SESSION)
              }
            }, modifier = Modifier.size(32.dp)) {
              val isRecording = CombatLogInteractor.isRecording.collectAsState()
              val plusInteractionSource = remember { MutableInteractionSource() }
              val icon = if (isRecording.value) "\uf04d" else "\u002b"
              val color = if (plusInteractionSource.collectIsHoveredAsState().value) RFColors.AccentRed else if (isRecording.value) RFColors.AccentRed else Color.White
              Text(text = icon, fontFamily = FontsHelper.faSolid(), fontSize = 15.sp, color = color, modifier = Modifier.hoverable(interactionSource = plusInteractionSource))
            }
          }
        }

        if (CombatLogInteractor.showHint.collectAsState().value) {
          Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = stringResource(Res.string.combat_press_plus_to_record),
              color = RFColors.TextTertiary,
              fontSize = 11.sp,
              maxLines = 1
            )
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
                    isOwnCharacter = card.name == config.playerName,
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
                    isOwnCharacter = card.name == config.playerName,
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
                    isOwnCharacter = card.name == config.playerName,
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

          customCategories.forEach { (category, flow) ->
            val sortedList by flow.collectAsState()
            val listState = customListStates[category.name] ?: return@forEach
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              LazyColumn(
                contentPadding = PaddingValues(0.dp),
                state = listState,
                modifier = Modifier
                  .padding(start = 12.dp, bottom = 6.dp)
                  .fillMaxWidth()
              ) {
                itemsIndexed(sortedList, key = { _, card -> card.name }) { index, card ->
                  PlayerRankingRow(
                    index = index,
                    card = card,
                    valueText = when (category) {
                      CombatRankingCategory.CHARMS -> card.sessionCharmTotal.toString()
                      CombatRankingCategory.SILENCES -> card.sessionSilenceTotal.toString()
                      CombatRankingCategory.DISTRESSES -> card.sessionDistressTotal.toString()
                      CombatRankingCategory.DEBUFFS -> card.sessionDebuffTotal.toString()
                      CombatRankingCategory.SONGS -> card.sessionSongsTotal.toString()
                      CombatRankingCategory.BUFFS -> card.sessionBuffTotal.toString()
                      CombatRankingCategory.POTIONS -> card.sessionPotionTotal.toString()
                      CombatRankingCategory.GLIDERS -> card.sessionGliderTotal.toString()
                      CombatRankingCategory.ITEMS -> card.sessionItemSkillTotal.toString()
                    },
                    valueColor = category.valueColor,
                    isRetribution = card.isBuildingAggression,
                    flashingColor = flashingColorState.value,
                    isOwnCharacter = card.name == config.playerName,
                    onClick = {
                      AppState.selectPlayer(card.name)
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
    // Watermark footer — faint version/ode status for screenshots
    // Uses align(BottomEnd) inside the Box parent so it takes zero layout space.
    val odeStatus = if (config.allowOdeToRecoveryCountAsHeals) " ode" else ""
    Text(
      text = "v${AppGlobals.APP_VERSION}$odeStatus",
      modifier = Modifier.align(Alignment.BottomEnd).wrapContentSize().padding(end = 6.dp, bottom = 2.dp),
      color = Color.White.copy(alpha = 0.18f),
      fontSize = 8.sp,
      fontWeight = FontWeight.Light
    )
  }
}
