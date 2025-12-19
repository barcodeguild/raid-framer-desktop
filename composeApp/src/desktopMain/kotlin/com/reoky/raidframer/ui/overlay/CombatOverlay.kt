package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.animateColor
import com.reoky.raidframer.core.helpers.EventParserHelper
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
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import lol.rfcloud.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.ui.dialog.exitDialog
import com.reoky.raidframer.ui.component.PlayerRankingRow

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

  // --- Sticky Scroll Logic ---
  // We track if the user is "stuck" to the top. If they are, we force scroll to 0 on data updates.
  // If they scroll down manually, we release the stickiness.

  var isDamageSticky by remember { mutableStateOf(true) }
  LaunchedEffect(damageListState) {
    snapshotFlow { damageListState.isScrollInProgress to damageListState.firstVisibleItemIndex }
      .collect { (isScrolling, index) ->
        // If user is scrolling, update sticky state based on position
        if (isScrolling) {
          isDamageSticky = (index == 0)
        } else {
          // If not scrolling (e.g. idle or programmatic scroll landed us at top), re-engage sticky
          if (index == 0 && damageListState.firstVisibleItemScrollOffset == 0) isDamageSticky = true
        }
      }
  }
  LaunchedEffect(sortedDamage) {
    if (isDamageSticky) damageListState.scrollToItem(0)
  }

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
  LaunchedEffect(sortedHeals) {
    if (isHealsSticky) healsListState.scrollToItem(0)
  }

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
  LaunchedEffect(sortedCC) {
    if (isCCSticky) ccListState.scrollToItem(0)
  }
  // ---------------------------

  exitDialog(shouldShowExitDialog)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .wrapContentHeight(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
    ) {
      Row(Modifier.align(Alignment.TopEnd).wrapContentSize()) {
        IconButton(
          onClick = { shouldShowExitDialog.value = true },
          modifier = Modifier
            .size(32.dp)
            .padding(top = 4.dp)
            .background(Color.Transparent, MaterialTheme.shapes.small)
            .shadow(
              elevation = 0.dp,
              clip = true,
              ambientColor = Color.Transparent,
              spotColor = Color.Transparent
            )
        ) {
          val closeButtonInteractionSource = remember { MutableInteractionSource() }
          Text(
            text = "✕",
            fontSize = 18.sp,
            color = if (closeButtonInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.hoverable(interactionSource = closeButtonInteractionSource)
          )
        }
        IconButton(
          onClick = { },
          modifier = Modifier
            .size(32.dp)
            .padding(top = 4.dp)
            .background(Color.Transparent, MaterialTheme.shapes.small)
            .shadow(
              elevation = 0.dp,
              clip = true,
              ambientColor = Color.Transparent,
              spotColor = Color.Transparent,
            ),
        ) {
          val closeButtonInteractionSource = remember { MutableInteractionSource() }
          Text(
            text = "⛨",
            fontSize = 16.sp,
            color = if (closeButtonInteractionSource.collectIsHoveredAsState().value) Color.Red else Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.hoverable(interactionSource = closeButtonInteractionSource)
          )
        }
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = "\uD83D\uDD25 PvP Damage \uD83D\uDD25", color = Color.White)
        }
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = "\uD83D\uDC89 PvP Heals \uD83D\uDC89", color = Color.White)
        }
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = "⛨ CC Delivered ⛨", color = Color.White)
        }
        IconButton(
          onClick = {  },
          modifier = Modifier
            .size(32.dp)
            .background(Color.Transparent, MaterialTheme.shapes.small)
            .padding(top = 4.dp)
            .shadow(
              elevation = 0.dp,
              clip = true,
              ambientColor = Color.Transparent,
              spotColor = Color.Transparent
            )
        ) {
          Text("\uD83D\uDEE0\uFE0F", fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center)
        }
        IconButton(
          onClick = { PlayerCacheInteractor.resetAllSessions() },
          modifier = Modifier
            .size(32.dp)
            .background(Color.Transparent, MaterialTheme.shapes.small)
            .padding(bottom = 4.dp, end = 8.dp)
            .shadow(
              elevation = 0.dp,
              clip = true,
              ambientColor = Color.Transparent,
              spotColor = Color.Transparent
            )
        ) {
          Text("⟳", fontSize = 24.sp, color = Color.White, textAlign = TextAlign.Center)
        }
      }

      // Make the columns fill the full available width so weights perform correctly
      Row(
        Modifier
          .align(Alignment.Center)
          .fillMaxWidth()
          .padding(top = 16.dp, start = 8.dp, end = 8.dp)
      ) {
        // Damage Column
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          LazyColumn(
            contentPadding = PaddingValues(0.dp),
            state = damageListState,
            modifier = Modifier
              .padding(8.dp)
              .fillMaxWidth()
          ) {
            itemsIndexed(sortedDamage, key = { _, card -> card.name }) { index, card ->
              PlayerRankingRow(
                index = index,
                card = card,
                valueText = card.sessionDamageTotal.humanReadableAbbreviation(),
                valueColor = Color(249, 191, 59, 255),
                isRetribution = card.isBuildingAggression,
                flashingColor = flashingColorState.value,
                onClick = {
                  wm?.openWindow(OverlayType.TRACKER)
                }
              )
            }
          }
        }
        // Heals Column
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          LazyColumn(
            contentPadding = PaddingValues(0.dp),
            state = healsListState,
            modifier = Modifier
              .padding(8.dp)
              .fillMaxWidth()
          ) {
            itemsIndexed(sortedHeals, key = { _, card -> card.name }) { index, card ->
              PlayerRankingRow(
                index = index,
                card = card,
                valueText = card.sessionHealTotal.humanReadableAbbreviation(),
                valueColor = Color.Green,
                isRetribution = card.isBuildingAggression,
                flashingColor = flashingColorState.value,
                onClick = {
                  wm?.openWindow(OverlayType.SUMMARY)
                }
              )
            }
          }
        }
        // CC Column
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          LazyColumn(
            contentPadding = PaddingValues(0.dp),
            state = ccListState,
            modifier = Modifier
              .padding(8.dp)
              .fillMaxWidth()
          ) {
            itemsIndexed(sortedCC, key = { _, card -> card.name }) { index, card ->
              PlayerRankingRow(
                index = index,
                card = card,
                valueText = card.sessionCCTotal.toString(),
                valueColor = Color.Cyan,
                isRetribution = card.isBuildingAggression,
                flashingColor = flashingColorState.value,
                onClick = {
                  wm?.openWindow(OverlayType.SUMMARY)
                }
              )
            }
          }
        }
      }
    }
  }
}
