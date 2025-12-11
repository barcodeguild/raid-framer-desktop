package com.reoky.raidframer.ui.overlay

import com.reoky.raidframer.RaidFramer
import com.reoky.raidframer.core.helpers.ParserHelper
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.WindowManager
import lol.rfcloud.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.ui.dialog.exitDialog

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

  // This map is still needed for the retribution icon
  val retributionByPlayer by ParserHelper.retributionByPlayer.collectAsState()

  // the animation for red flashing text
  val flashingColorState = rememberInfiniteTransition().animateColor(
    initialValue = Color.White,
    targetValue = Color.Red,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

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
          onClick = { /* reset stats? */ },
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
      Row(Modifier.align(Alignment.Center).wrapContentSize().padding(top = 16.dp, start = 8.dp, end = 8.dp)) {
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          LazyColumn(
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(12.dp)
          ) {
            itemsIndexed(sortedDamage, key = { _, card -> card.name }) { index, card ->
              val damageInteractionSource = remember { MutableInteractionSource() }
              val isDamageHovered = damageInteractionSource.collectIsHoveredAsState().value
              Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                  .clickable {
                    RaidFramer.isTrackerOverlayVisible.value = true
                    RaidFramer.currentTargetName.value = card.name
                  }
                  .background(if (isDamageHovered) Color.Red.copy(alpha = 0.25f) else Color.Transparent) // Change color when hovered
                  .hoverable(interactionSource = damageInteractionSource)
              ) {
                Text(
                  text = "${index + 1}. ${card.name} ",
                  color = Color.White,
                  overflow = TextOverflow.Ellipsis,
                  maxLines = 1,
                  modifier = Modifier.weight(0.7f)
                )
                Text(
                  text = card.sessionDamageTotal.humanReadableAbbreviation(),
                  color = Color(249, 191, 59, 255),
                  maxLines = 1,
                  modifier = Modifier.weight(0.2f)
                )
                if (retributionByPlayer[card.name] != null) {
                  Text(
                    text = "⛨",
                    color = flashingColorState.value,
                    maxLines = 1,
                    modifier = Modifier.weight(0.1f)
                  )
                } else {
                  Spacer(modifier = Modifier.weight(0.1f))
                }
              }
            }
          }
        }
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          LazyColumn(
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(12.dp)
          ) {
            itemsIndexed(sortedHeals, key = { _, card -> card.name }) { index, card ->
              val healsInteractionSource = remember { MutableInteractionSource() }
              val isHealsHovered = healsInteractionSource.collectIsHoveredAsState().value
              Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                  .clickable {
                    RaidFramer.isTrackerOverlayVisible.value = true
                    RaidFramer.currentTargetName.value = card.name
                  }
                  .background(if (isHealsHovered) Color.Red.copy(alpha = 0.25f) else Color.Transparent) // Change color when hovered
                  .hoverable(interactionSource = healsInteractionSource)
              ) {
                Text(
                  text = "${index + 1}. ${card.name}",
                  color = Color.White,
                  overflow = TextOverflow.Ellipsis,
                  maxLines = 1,
                  modifier = Modifier.weight(0.7f)
                )
                Text(
                  text = card.sessionHealTotal.humanReadableAbbreviation(),
                  color = Color.Green,
                  maxLines = 1,
                  modifier = Modifier.weight(0.2f)
                )
                if (retributionByPlayer[card.name] != null) {
                  Text(
                    text = "⛨",
                    color = flashingColorState.value,
                    maxLines = 1,
                    modifier = Modifier.weight(0.1f)
                  )
                } else {
                  Spacer(modifier = Modifier.weight(0.1f))
                }
              }
            }
          }
        }
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(top = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          LazyColumn(
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(12.dp)
          ) {
            itemsIndexed(sortedCC, key = { _, card -> card.name }) { index, card ->
              val ccInteractionSource = remember { MutableInteractionSource() }
              val isCcHovered = ccInteractionSource.collectIsHoveredAsState().value
              Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                  .clickable {
                    RaidFramer.isTrackerOverlayVisible.value = true
                    RaidFramer.currentTargetName.value = card.name
                  }
                  .background(if (isCcHovered) Color.Red.copy(alpha = 0.25f) else Color.Transparent) // Change color when hovered
                  .hoverable(interactionSource = ccInteractionSource)
              ) {
                Text(
                  text = "${index + 1}. ${card.name}",
                  color = Color.White,
                  overflow = TextOverflow.Ellipsis,
                  maxLines = 1,
                  modifier = Modifier.weight(0.7f)
                )
                Text(
                  text = card.sessionCCTotal.toString(),
                  color = Color.Cyan,
                  maxLines = 1,
                  modifier = Modifier.weight(0.2f)
                )
                if (retributionByPlayer[card.name] != null) {
                  Text(
                    text = "⛨",
                    color = flashingColorState.value,
                    maxLines = 1,
                    modifier = Modifier.weight(0.1f)
                  )
                } else {
                  Spacer(modifier = Modifier.weight(0.1f))
                }
              }
            }
          }
        }
      }
    }
  }
}
