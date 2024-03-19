package ui.overlay

import CombatInteractor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun TrackerOverlay() {

  var castProgress by remember { mutableStateOf(0f) }
  val spellName by CombatInteractor.targetCurrentlyCasting.collectAsState()
  var isCasting by remember { mutableStateOf(false) }

  val animatedProgress by animateFloatAsState(
    targetValue = castProgress,
    animationSpec = tween(durationMillis = (castProgress * 2300).toInt(), delayMillis = 0)
  )

  LaunchedEffect(spellName) {
    isCasting = true
    castProgress = 1f // set the progress to 100%
    delay(4000) // delay for the casting time
    castProgress = 0f // reset the progress
    isCasting = false
  }

  Box {

    /* Close Button */
    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
      val interactionSource = remember { MutableInteractionSource() }
      val isCloseHovered by interactionSource.collectIsHoveredAsState()

      IconButton(
        onClick = {
          AppState.toggleTrackerOverlayVisibility()
        },
        modifier = Modifier
          .size(32.dp)
          .background(if (isCloseHovered) Color.Red.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.20f), MaterialTheme.shapes.small)
          .shadow(
            elevation = 0.dp,
            clip = true,
            ambientColor = Color.Transparent,
            spotColor = Color.Transparent
          )
          .hoverable(interactionSource = interactionSource)
          .clip(RoundedCornerShape(8.dp))
      ) {
        Text("✕", fontSize = 18.sp, color = if (isCloseHovered) Color.White else Color.White, textAlign = TextAlign.Center)
      }
    }

    /* Player Details */
    Column(Modifier.padding(16.dp)) {
      Text(
        text = "Super Tracker",
        color = Color.White,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        modifier = Modifier
          .padding(bottom = 8.dp)
      )
      Row(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)){
        Text(
          text = "Current Target: ",
          color = Color.White,
          textAlign = TextAlign.Center,
          fontSize = 12.sp,
          modifier = Modifier
            .padding(end = 8.dp)
        )
        Text(
          text = AppState.currentTargetName.value,
          color = Color.White,
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.W400,
          fontSize = 12.sp
        )
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
            color = Color.Red,
            modifier = Modifier
              .fillMaxWidth()
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
        var tabIndex by remember { mutableStateOf(0) }
        TabRow(
          selectedTabIndex = tabIndex,
          backgroundColor = Color.White.copy(alpha = 0.25f),
          contentColor = Color.Red.copy(alpha = 0.25f)
        ) {
          Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
            Text(
              text = "Incoming Damage",
              color = Color.White,
              modifier = Modifier.padding(6.dp)
            )
          }
          Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
            Text(
              text = "Outgoing Damage",
              color = Color.White,
              modifier = Modifier.padding(6.dp)
            )
          }
        }
        when (tabIndex) {
          0 -> {
            Text("Content for Tab 1")
          }
          1 -> {
            Text("Content for Tab 2")
          }
        }
      }

    }

  }
}