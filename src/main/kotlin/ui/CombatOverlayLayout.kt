package ui

import CombatEventInteractor
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.helpers.humanReadableAbbreviation
import viewmodel.CombatOverlayModel

@Preview
@Composable
fun PreviewCombatOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    CombatOverlayLayout(CombatOverlayModel())
  }
}

@Composable
fun CombatOverlayLayout(state: CombatOverlayModel? = null) {

  val damageByPlayer by CombatEventInteractor.damageByPlayer
  val healsByPlayer by CombatEventInteractor.healsByPlayer

  // Transform and sort the maps
  val sortedDamage = damageByPlayer.toList().sortedByDescending { it.second }
  val sortedHeals = healsByPlayer.toList().sortedByDescending { it.second }

  Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = state?.text?.value ?: "",
        color = Color.White,
        modifier = Modifier.align(Alignment.Center),
      )
      Button(
        onClick = { CombatEventInteractor.resetStats() },
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .size(48.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
      ) {
        Text("⟳", fontSize = 24.sp, color = Color.White, textAlign = TextAlign.Center)
      }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
      Text(text = "Total Damage", color = Color.White, modifier = Modifier.padding(8.dp))
      LazyColumn(
        modifier = Modifier.fillMaxWidth().align(Alignment.Start),
        contentPadding = PaddingValues(16.dp),
      ) {
        items(sortedDamage.size.coerceAtMost(10)) { item ->
          Text(text = "${sortedDamage[item].first}: ${sortedDamage[item].second.humanReadableAbbreviation()}", color = Color.Red)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
      Text(text = "Total Heals", color = Color.White, modifier = Modifier.padding(8.dp))
      LazyColumn(
        modifier = Modifier.fillMaxWidth().align(Alignment.Start),
        contentPadding = PaddingValues(16.dp)
      ) {
        items(sortedHeals.size.coerceAtMost(10)) { item ->
          Text(text = "${sortedHeals[item].first}: ${sortedHeals[item].second.humanReadableAbbreviation()}", color = Color.Green)
        }
      }
    }
  }
}