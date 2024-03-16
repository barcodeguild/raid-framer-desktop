package ui.overlay

import AppState
import CombatEventInteractor
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.helpers.humanReadableAbbreviation
import ui.dialog.exitDialog
import kotlin.system.exitProcess

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
fun CombatOverlay() {

  val shouldShowExitDialog = mutableStateOf(false)

  val damageByPlayer by CombatEventInteractor.damageByPlayer
  val healsByPlayer by CombatEventInteractor.healsByPlayer

  // Transform and sort the maps
  val sortedDamage = damageByPlayer.toList().sortedByDescending { it.second }
  val sortedHeals = healsByPlayer.toList().sortedByDescending { it.second }

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
      Row(Modifier.align(Alignment.TopStart).wrapContentSize()) {
        IconButton(
          onClick = { shouldShowExitDialog.value = true },
          modifier = Modifier
            .size(38.dp)
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
      }
      Row(Modifier.align(Alignment.TopEnd).wrapContentSize()) {
        IconButton(
          onClick = { AppState.toggleSettingsOverlayVisibility() },
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
          onClick = { },
          modifier = Modifier
            .size(32.dp)
            .background(Color.Transparent, MaterialTheme.shapes.small)
            .padding(top = 0.5.dp)
            .shadow(
              elevation = 0.dp,
              clip = true,
              ambientColor = Color.Transparent,
              spotColor = Color.Transparent
            )
        ) {
          Text("⌖", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.ExtraLight, textAlign = TextAlign.Center)
        }
        IconButton(
          onClick = { CombatEventInteractor.resetStats() },
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
    }

    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "\uD83D\uDD25 Total Damage  \uD83D\uDD25", color = Color.White)
        LazyColumn(
          contentPadding = PaddingValues(4.dp),
          modifier = Modifier.padding(8.dp)
        ) {
          items(sortedDamage.size.coerceAtMost(50)) { item ->
            Row(horizontalArrangement = Arrangement.Start) {
              Text(
                text = "${item + 1}. ${sortedDamage[item].first}: ",
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(0.7f)
              )
              Text(
                text = sortedDamage[item].second.humanReadableAbbreviation(),
                color = Color(249, 191, 59, 255),
                modifier = Modifier.weight(0.2f)
              )
            }
          }
        }
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "\uD83D\uDC89 Total Heals \uD83D\uDC89", color = Color.White)
        LazyColumn(
          contentPadding = PaddingValues(4.dp),
          modifier = Modifier.padding(8.dp)
        ) {
          items(sortedHeals.size.coerceAtMost(50)) { item ->
            Row(horizontalArrangement = Arrangement.Start) {
              Text(
                text = "${item + 1}. ${sortedHeals[item].first}: ",
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(0.8f)
              )
              Text(
                text = sortedHeals[item].second.humanReadableAbbreviation(),
                color = Color.Green,
                modifier = Modifier.weight(0.3f)
              )
            }
          }
        }
      }
    }

  }
}