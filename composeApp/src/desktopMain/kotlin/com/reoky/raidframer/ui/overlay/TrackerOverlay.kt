package com.reoky.raidframer.ui.overlay

import com.reoky.raidframer.core.helpers.EventParserHelper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.renderDebuffThumbnailGrid
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CloseButton
import lol.rfcloud.core.helpers.annotatedStringForAttack
import lol.rfcloud.core.helpers.annotatedStringForHeal
import kotlinx.coroutines.delay

@Preview
@Composable
fun PreviewAggroOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    TrackerOverlay()
  }
}

@Composable
fun TrackerOverlay(wm: WindowManager? = null) {

  var showDmg by remember { mutableStateOf(false) }
  var castProgress by remember { mutableStateOf(0f) }
  val spellName = "Unknown Spell"
  var isCasting by remember { mutableStateOf(false) }

  val animatedProgress by animateFloatAsState(
    targetValue = castProgress,
    animationSpec = tween(durationMillis = (castProgress * 2000).toInt(), delayMillis = 0)
  )

  LaunchedEffect(spellName) {
    isCasting = false
    castProgress = 0f
    if (spellName.isBlank()) return@LaunchedEffect
    if (spellName.contains("Lunastone")) return@LaunchedEffect
    if (spellName.contains("Longing")) return@LaunchedEffect
    delay(250)
    isCasting = true
    castProgress = 1f // set the progress to 100%
  }

  var playerName = remember { "Reoky" } // testing for now


  val isCharmed = remember { mutableStateOf(false) }
  val recentDamageEvents = remember { mutableStateListOf<DamageEvent>() }
  val recentHealEvents = remember { mutableStateListOf<HealEvent>() }
  val activeDebuffs = remember { mutableStateListOf<String>() }

  // poll for recent heals and damage events
  LaunchedEffect(playerName) {
    while (true) {
      PlayerCacheInteractor.getCard(playerName)?.let { player ->
        isCharmed.value = player.isCharmed
        recentDamageEvents.clear()
        recentDamageEvents.addAll(player.recentDamageEvents)
        recentHealEvents.clear()
        recentHealEvents.addAll(player.recentHealEvents)
        activeDebuffs.clear()
        //activeDebuffs.addAll(player.activeDebuffs)
      }
      delay(5000)
    }
  }

  // special status glow effect
  var isSheeningSpecialStatus by remember { mutableStateOf(false) }
  var specialStatus by remember { mutableStateOf("") }
  LaunchedEffect(isCharmed.value) {
    isSheeningSpecialStatus = false
    if (!isCharmed.value) {
      specialStatus = ""
      return@LaunchedEffect
    }

    specialStatus = "Charmed"

    var i = 0
    while (i < 3) {
      isSheeningSpecialStatus = !isSheeningSpecialStatus
      delay(1500L)
      i++
    }
    isSheeningSpecialStatus = false
  }

  val specialColor by animateColorAsState(
    targetValue = if (isSheeningSpecialStatus) Color(128, 0, 128, 150) else Color.Transparent,
    animationSpec = tween(durationMillis = 1500) // Add this line to make the color transition smoother
  )

  var size by remember { mutableStateOf(IntSize(0, 0)) }
  val brush = Brush.linearGradient(
    colors = listOf(specialColor, Color.Transparent),
    start = Offset(0f, 0f),
    end = Offset(0f, size.height.toFloat())
  )

  Box(modifier = Modifier
    .fillMaxSize()
    .background(brush = brush)
    .onGloballyPositioned { coordinates ->
      size = coordinates.size
    }
  ) {

    // top buttons
    Box(
      modifier = Modifier.fillMaxWidth().align(Alignment.TopEnd)
    ) {
      Row(modifier = Modifier.wrapContentWidth().align(Alignment.TopEnd)) {

        /* Hide Damage Button */
        Box(modifier = Modifier.padding(6.dp)) {
          val interactionSource = remember { MutableInteractionSource() }
          val isHideHovered by interactionSource.collectIsHoveredAsState()

          IconButton(
            onClick = {
              showDmg = !showDmg
            },
            modifier = Modifier
              .size(32.dp)
              .background(
                if (isHideHovered) Color.Red.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.20f),
                MaterialTheme.shapes.small
              )
              .shadow(
                elevation = 0.dp,
                clip = true,
                ambientColor = Color.Transparent,
                spotColor = Color.Transparent
              )
              .hoverable(interactionSource = interactionSource)
              .clip(RoundedCornerShape(8.dp))
          ) {
            Text(
              "DMG",
              fontSize = 10.sp,
              color = if (isHideHovered) Color.White else Color.White,
              textAlign = TextAlign.Center
            )
          }
        }

        /* Close Button */
        CloseButton(
          onClose = { wm?.closeWindow(OverlayType.TRACKER) },
          //modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
        )
      }
    }

    /* Player Details */
    Column(Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {

      /* Title */
      Row {
        val title = buildAnnotatedString {
          withStyle(style = SpanStyle(color = Color.White)) {
            append(playerName)
          }
          withStyle(style = SpanStyle(color = Color.Cyan)) {
            append(if (specialStatus.isNotBlank()) " [$specialStatus]" else "")
          }
        }
        Row {
          Text(
            text = title,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
              .padding(bottom = 8.dp)

          )
          Spacer(modifier = Modifier.width(40.dp))
        }
      }

      /* Cast State */
      Column(
        modifier = Modifier
          .padding(top = 8.dp)
          .align(Alignment.CenterHorizontally)
      ) {
        if (isCasting) {
          LinearProgressIndicator(
            progress = animatedProgress,
            color = Color(97, 155, 255),
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp)
              .padding(start = 8.dp, end = 8.dp)
          )
          Text(
            text = spellName ?: "Casting",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            modifier = Modifier
              .padding(top = 4.dp, bottom = 4.dp)
              .fillMaxWidth()
          )
        } else {
          Spacer(modifier = Modifier.height(32.dp))
        }
      }
      Column(Modifier.padding(top = 16.dp)) {
        renderDebuffThumbnailGrid(activeDebuffs)
      }
      if (showDmg) {
        Column(Modifier.padding(top = 16.dp)) {
          var tabIndex by remember { mutableStateOf(0) }
          TabRow(
            selectedTabIndex = tabIndex,
            backgroundColor = Color.White.copy(alpha = 0.25f),
            contentColor = Color.Red.copy(alpha = 0.25f)
          ) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
              Text(
                text = "Recent Damage",
                color = Color.White,
                modifier = Modifier.padding(6.dp)
              )
            }
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
              Text(
                text = "Recent Heals",
                color = Color.White,
                modifier = Modifier.padding(6.dp)
              )
            }
          }
          when (tabIndex) {
            0 -> {
              LazyColumn(
                contentPadding = PaddingValues(6.dp)
              ) {
                items(recentDamageEvents.size) { item ->
                  val incomingInteractionSource = remember { MutableInteractionSource() }
                  val isHovered = incomingInteractionSource.collectIsHoveredAsState().value
                  Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                      .clickable { /* do nothing */ }
                      .background(if (isHovered) Color.White.copy(alpha = 0.20f) else Color.Transparent) // Change color when hovered
                      .hoverable(interactionSource = incomingInteractionSource)
                  ) {
                    Text(
                      text = annotatedStringForAttack(recentDamageEvents[item]),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.padding(2.dp)
                    )
                  }
                }
              }
            }

            1 -> {
              LazyColumn(
                contentPadding = PaddingValues(4.dp)
              ) {
                items(recentHealEvents.size) { item ->
                  val outgoingInteractionSource = remember { MutableInteractionSource() }
                  val isHovered = outgoingInteractionSource.collectIsHoveredAsState().value
                  Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                      .clickable { /* do nothing */ }
                      .background(if (isHovered) Color.White.copy(alpha = 0.20f) else Color.Transparent) // Change color when hovered
                      .hoverable(interactionSource = outgoingInteractionSource)
                  ) {
                    Text(
                      text = annotatedStringForHeal(recentHealEvents[item]),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.padding(2.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
