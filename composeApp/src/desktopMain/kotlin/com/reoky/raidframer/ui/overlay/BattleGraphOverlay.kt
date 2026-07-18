package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.interactor.BattleGraphInteractor
import com.reoky.raidframer.core.interactor.BattleGraphMode
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CompactSessionTotals
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.BattleGraphComponent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_damage
import raid_framer_desktop.composeapp.generated.resources.battle_graph_heal_prop
import raid_framer_desktop.composeapp.generated.resources.battle_graph_crowd_control_distribution
import raid_framer_desktop.composeapp.generated.resources.battle_graph_no_data

@Composable
fun BattleGraphOverlay(wm: WindowManager?) {
  val graphData by BattleGraphInteractor.graphData.collectAsState()
  val selectedMode by BattleGraphInteractor.selectedMode.collectAsState()
  val dragLock = LocalDragLock.current

  var damageThreshold by remember { mutableStateOf(1000f) }
  var healThreshold by remember { mutableStateOf(1000f) }
  var ccThreshold by remember { mutableStateOf(0f) }
  var searchQuery by remember { mutableStateOf("") }
  var maxNodes by remember { mutableStateOf(25f) }
  var selectedPlayerName by remember { mutableStateOf<String?>(null) }

  val sliderInteractionSource = remember { MutableInteractionSource() }
  LaunchedEffect(sliderInteractionSource) {
    sliderInteractionSource.interactions.collect { interaction ->
      when (interaction) {
        is DragInteraction.Start -> { dragLock.value = true }
        is DragInteraction.Stop,
        is DragInteraction.Cancel -> { dragLock.value = false }
      }
    }
  }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    val titleText = when (selectedMode) {
      BattleGraphMode.DAMAGE -> stringResource(Res.string.battle_graph_focused_damage)
      BattleGraphMode.HEALS -> stringResource(Res.string.battle_graph_heal_prop)
      BattleGraphMode.CC -> stringResource(Res.string.battle_graph_crowd_control_distribution)
    }

    TitleBarComponent(
      title = titleText,
      onClose = { wm?.closeWindow(OverlayType.BATTLE_GRAPH) }
    )

    // Graph area with controls overlaid in upper-right
    Box(modifier = Modifier.fillMaxSize()) {
      if (graphData.nodes.isNotEmpty()) {
        BattleGraphComponent(
          graphData = graphData,
          mode = selectedMode,
          onOpenPlayerCard = { playerName ->
            AppState.selectPlayer(playerName)
            wm?.openWindow(OverlayType.PLAYER_CARD)
          },
          onFilterByName = { name ->
            searchQuery = name
            BattleGraphInteractor.setSearchQuery(name)
          },
          onFilterBySpec = { specName ->
            searchQuery = specName
            BattleGraphInteractor.setSearchQuery(specName)
          },
          onNodeSelected = { name -> selectedPlayerName = name },
          modifier = Modifier.fillMaxSize()
        )
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

      // Compact controls overlay in upper-right
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(RFColors.CardBackground.copy(alpha = 0.85f))
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End
      ) {
        // Search box
        TextField(
          value = searchQuery,
          onValueChange = { query ->
            searchQuery = query
            BattleGraphInteractor.setSearchQuery(query)
          },
          placeholder = { Text("Search Characters & Classes", fontSize = 11.sp, color = RFColors.TextTertiary) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              TextButton(
                onClick = {
                  searchQuery = ""
                  BattleGraphInteractor.setSearchQuery("")
                },
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
              ) {
                Text(
                  text = "×",
                  color = RFColors.TextSecondary,
                  fontSize = 18.sp
                )
              }
            }
          },
          modifier = Modifier
            .width(320.dp)
            .height(56.dp)
            .pointerInput(Unit) {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent()
                  when (event.type) {
                    PointerEventType.Enter -> dragLock.value = true
                    PointerEventType.Exit -> dragLock.value = false
                  }
                }
              }
            }
            .onFocusChanged { focusState ->
              dragLock.value = focusState.isFocused
            },
          textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 11.sp,
            color = RFColors.TextPrimary
          ),
          singleLine = true,
          maxLines = 1,
          colors = TextFieldDefaults.textFieldColors(
            backgroundColor = RFColors.CardBackground,
            cursorColor = RFColors.TextPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Mode toggles - compact row
        Row(
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val modes = listOf(
            BattleGraphMode.DAMAGE to "DMG",
            BattleGraphMode.HEALS to "Heal",
            BattleGraphMode.CC to "CC"
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
              modifier = Modifier.alpha(if (isSelected) 1f else 0.5f).height(24.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
              Text(
                text = label,
                color = if (isSelected) highlightColor else RFColors.TextSecondary,
                fontSize = 10.sp
              )
            }
          }
        }

        // Threshold slider - compact
        when (selectedMode) {
          BattleGraphMode.DAMAGE -> {
            CompactThresholdSlider(
              label = "Min DMG",
              value = damageThreshold,
              rangeMax = 250_000f,
              onValueChange = { damageThreshold = it },
              onValueChangeFinished = { BattleGraphInteractor.setDamageThreshold(damageThreshold.toLong()) },
              color = RFColors.dpsOrange,
              interactionSource = sliderInteractionSource,
              modifier = Modifier.width(320.dp)
            )
          }
          BattleGraphMode.HEALS -> {
            CompactThresholdSlider(
              label = "Min Heal",
              value = healThreshold,
              rangeMax = 250_000f,
              onValueChange = { healThreshold = it },
              onValueChangeFinished = { BattleGraphInteractor.setHealThreshold(healThreshold.toLong()) },
              color = RFColors.healsGreen,
              interactionSource = sliderInteractionSource,
              modifier = Modifier.width(320.dp)
            )
          }
          BattleGraphMode.CC -> {
            CompactThresholdSlider(
              label = "Min CC",
              value = ccThreshold,
              rangeMax = 250f,
              onValueChange = { ccThreshold = it },
              onValueChangeFinished = { BattleGraphInteractor.setCCThreshold(ccThreshold.toInt()) },
              color = RFColors.ccCyan,
              interactionSource = sliderInteractionSource,
              modifier = Modifier.width(320.dp)
            )
          }
        }

        // Max objects slider - below threshold
        CompactThresholdSlider(
          label = "Max Nodes",
          value = maxNodes,
          rangeMax = 250f,
          onValueChange = { maxNodes = it },
          onValueChangeFinished = { BattleGraphInteractor.setMaxNodes(maxNodes.toInt()) },
          color = RFColors.TextPrimary,
          interactionSource = sliderInteractionSource,
          modifier = Modifier.width(320.dp)
        )
      }

      // Session totals widget in bottom-left when a node is selected
      selectedPlayerName?.let { playerName ->
        CompactSessionTotals(
          playerName = playerName,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(8.dp)
            .widthIn(max = 200.dp)
        )
      }
    }
  }
}

@Composable
private fun CompactThresholdSlider(
  label: String,
  value: Float,
  rangeMax: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit,
  color: Color,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier
  ) {
    Text(text = "$label:", color = RFColors.TextSecondary, fontSize = 9.sp)
    Slider(
      value = value,
      onValueChange = onValueChange,
      onValueChangeFinished = onValueChangeFinished,
      valueRange = 0f..rangeMax,
      modifier = Modifier.weight(1f),
      colors = SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        inactiveTrackColor = RFColors.TextTertiary
      ),
      interactionSource = interactionSource
    )
    Text(
      text = formatThresholdValue(value),
      color = color,
      fontSize = 9.sp
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
