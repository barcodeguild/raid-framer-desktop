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
import androidx.compose.ui.draw.shadow
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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .wrapContentHeight(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
    ) {
      Text(
        text = state?.text?.value ?: "",
        color = Color.White,
        modifier = Modifier.align(Alignment.Center),
      )
      IconButton(
        onClick = { CombatEventInteractor.resetStats() },
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .size(38.dp)
          .background(Color.Transparent, MaterialTheme.shapes.small)
          .shadow(
            elevation = 0.dp,
            clip = true,
            ambientColor = Color.Transparent,
            spotColor = Color.Transparent
          )
      ) {
        Text("⟳", fontSize = 24.sp, color = Color.White, textAlign = TextAlign.Center)
      }
      Spacer(modifier = Modifier.width(12.dp))
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
        Text(text = "\uD83D\uDD25 Total Damage  \uD83D\uDD25", color = Color.White, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 4.dp))
        LazyColumn(
          contentPadding = PaddingValues(4.dp),
          modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 8.dp)
        ) {
          items(sortedDamage.size.coerceAtMost(50)) { item ->
            Row {
              Text(
                text = "$item. ${sortedDamage[item].first}: ",
                color = Color.White
              )
              Text(
                text = sortedDamage[item].second.humanReadableAbbreviation(),
                color = Color(249, 191, 59, 255)
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
        Text(text = "\uD83D\uDC89 Total Heals \uD83D\uDC89", color = Color.White, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 4.dp))
        LazyColumn(
          contentPadding = PaddingValues(4.dp),
          modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 8.dp)
        ) {
          items(sortedHeals.size.coerceAtMost(50)) { item ->
            Row {
              Text(
                text = "$item. ${sortedHeals[item].first}: ",
                color = Color.White
              )
              Text(
                text = sortedHeals[item].second.humanReadableAbbreviation(),
                color = Color.Green
              )
            }
          }
        }
      }
    }

  }
}