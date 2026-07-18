package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.interactor.BattleGraphInteractor
import com.reoky.raidframer.core.interactor.BattleGraphMode
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.BattleGraphComponent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.battle_graph_title
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_damage
import raid_framer_desktop.composeapp.generated.resources.battle_graph_heal_prop
import raid_framer_desktop.composeapp.generated.resources.battle_graph_crowd_control_distribution
import raid_framer_desktop.composeapp.generated.resources.battle_graph_no_data

@Composable
fun BattleGraphOverlay(wm: WindowManager?) {
  val graphData by BattleGraphInteractor.graphData.collectAsState()
  val selectedMode by BattleGraphInteractor.selectedMode.collectAsState()

  var damageThresholdMin by remember { mutableStateOf(1000f) }
  var damageThresholdMax by remember { mutableStateOf(10_000_000f) }
  var healThresholdMin by remember { mutableStateOf(1000f) }
  var healThresholdMax by remember { mutableStateOf(10_000_000f) }
  var ccThresholdMin by remember { mutableStateOf(0f) }
  var ccThresholdMax by remember { mutableStateOf(5000f) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(RFColors.CardBackground)
  ) {
    TitleBarComponent(
      title = stringResource(Res.string.battle_graph_title),
      onClose = { wm?.closeWindow(OverlayType.BATTLE_GRAPH) }
    )

    // Mode toggles
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val modes = listOf(
        BattleGraphMode.DAMAGE to stringResource(Res.string.battle_graph_focused_damage),
        BattleGraphMode.HEALS to stringResource(Res.string.battle_graph_heal_prop),
        BattleGraphMode.CC to stringResource(Res.string.battle_graph_crowd_control_distribution)
      )
      modes.forEach { (mode, label) ->
        val isSelected = mode == selectedMode
        val highlightColor = when (mode) {
          BattleGraphMode.DAMAGE -> RFColors.dpsOrange
          BattleGraphMode.HEALS -> RFColors.healsGreen
          BattleGraphMode.CC -> RFColors.ccCyan
        }
        TextButton(
          onClick = { BattleGraphInteractor.setMode(mode) },
          modifier = Modifier
            .padding(horizontal = 4.dp)
            .alpha(if (isSelected) 1f else 0.5f)
        ) {
          Text(
            text = label,
            color = if (isSelected) highlightColor else RFColors.TextSecondary,
            fontSize = 11.sp
          )
        }
      }
    }

    // Threshold filters row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      when (selectedMode) {
        BattleGraphMode.DAMAGE -> {
          ThresholdFilter(
            label = "DMG",
            minValue = damageThresholdMin,
            maxValue = damageThresholdMax,
            rangeMin = 0f,
            rangeMax = 10_000_000f,
            onMinChange = { damageThresholdMin = it; BattleGraphInteractor.setDamageThreshold(it.toLong(), damageThresholdMax.toLong()) },
            onMaxChange = { damageThresholdMax = it; BattleGraphInteractor.setDamageThreshold(damageThresholdMin.toLong(), it.toLong()) },
            color = RFColors.dpsOrange
          )
        }
        BattleGraphMode.HEALS -> {
          ThresholdFilter(
            label = "Heal",
            minValue = healThresholdMin,
            maxValue = healThresholdMax,
            rangeMin = 0f,
            rangeMax = 10_000_000f,
            onMinChange = { healThresholdMin = it; BattleGraphInteractor.setHealThreshold(it.toLong(), healThresholdMax.toLong()) },
            onMaxChange = { healThresholdMax = it; BattleGraphInteractor.setHealThreshold(healThresholdMin.toLong(), it.toLong()) },
            color = RFColors.healsGreen
          )
        }
        BattleGraphMode.CC -> {
          ThresholdFilter(
            label = "CC",
            minValue = ccThresholdMin,
            maxValue = ccThresholdMax,
            rangeMin = 0f,
            rangeMax = 5000f,
            onMinChange = { ccThresholdMin = it; BattleGraphInteractor.setCCThreshold(it.toInt(), ccThresholdMax.toInt()) },
            onMaxChange = { ccThresholdMax = it; BattleGraphInteractor.setCCThreshold(ccThresholdMin.toInt(), it.toInt()) },
            color = RFColors.ccCyan
          )
        }
      }
    }

    // Graph area
    if (graphData.nodes.isNotEmpty()) {
      Box(modifier = Modifier.fillMaxSize()) {
        BattleGraphComponent(
          graphData = graphData,
          mode = selectedMode,
          modifier = Modifier.fillMaxSize()
        )
      }
    } else {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = stringResource(Res.string.battle_graph_no_data),
          color = RFColors.TextTertiary,
          fontSize = 14.sp
        )
      }
    }
  }
}

@Composable
private fun ThresholdFilter(
  label: String,
  minValue: Float,
  maxValue: Float,
  rangeMin: Float,
  rangeMax: Float,
  onMinChange: (Float) -> Unit,
  onMaxChange: (Float) -> Unit,
  color: Color
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(text = "$label:", color = RFColors.TextSecondary, fontSize = 10.sp)
    Text(
      text = formatThresholdValue(minValue),
      color = color,
      fontSize = 10.sp
    )
    Text(text = "-", color = RFColors.TextTertiary, fontSize = 10.sp)
    Text(
      text = formatThresholdValue(maxValue),
      color = color,
      fontSize = 10.sp
    )
  }
}

private fun formatThresholdValue(value: Float): String {
  return when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format("%.0fk", value / 1_000.0)
    else -> value.toInt().toString()
  }
}
