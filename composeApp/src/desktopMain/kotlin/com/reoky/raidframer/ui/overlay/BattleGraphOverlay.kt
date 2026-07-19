package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.debounce
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_damage
import raid_framer_desktop.composeapp.generated.resources.battle_graph_heal_prop
import raid_framer_desktop.composeapp.generated.resources.battle_graph_crowd_control_distribution
import raid_framer_desktop.composeapp.generated.resources.battle_graph_no_data
import raid_framer_desktop.composeapp.generated.resources.battle_graph_resume
import raid_framer_desktop.composeapp.generated.resources.battle_graph_pause
import raid_framer_desktop.composeapp.generated.resources.battle_graph_paused
import raid_framer_desktop.composeapp.generated.resources.battle_graph_search_placeholder
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_damage
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_heals
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_cc
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_dmg
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_heal
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_cc
import raid_framer_desktop.composeapp.generated.resources.battle_graph_max_edges

@Composable
fun BattleGraphOverlay(wm: WindowManager?) {
  val graphData by BattleGraphInteractor.graphData.collectAsState()
  val selectedMode by BattleGraphInteractor.selectedMode.collectAsState()
  val isPaused by BattleGraphInteractor.isPaused.collectAsState()
  val dragLock = LocalDragLock.current

  // Slider positions 0..1 (mapped to actual values via multiplier)
  var damageSlider by remember { mutableFloatStateOf(0.1f) }      // 25000 / 250_000
  var healSlider by remember { mutableFloatStateOf(0.1f) }       // 25000 / 250_000
  var ccSlider by remember { mutableFloatStateOf(0.02f) }        // 5 / 250
  var searchQuery by remember { mutableStateOf("") }
  var maxEdgesSlider by remember { mutableFloatStateOf(0.08f) }   // 20 / 250
  var selectedPlayerName by remember { mutableStateOf<String?>(null) }

  // Debounced push to interactor — avoids recomposition during drag
  LaunchedEffect(Unit) {
    snapshotFlow { damageSlider }.debounce(300).collect { v -> BattleGraphInteractor.setDamageThreshold((v * 250_000f).toLong()) }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { healSlider }.debounce(300).collect { v -> BattleGraphInteractor.setHealThreshold((v * 250_000f).toLong()) }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { ccSlider }.debounce(300).collect { v -> BattleGraphInteractor.setCCThreshold((v * 250f).toInt()) }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { maxEdgesSlider }.debounce(300).collect { v -> BattleGraphInteractor.setMaxEdges((v * 250f).toInt()) }
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
          .padding(horizontal = 12.dp, vertical = 10.dp)
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
          },
        horizontalAlignment = Alignment.End
      ) {
        // Search box
        TextField(
          value = searchQuery,
          onValueChange = { query ->
            searchQuery = query
            BattleGraphInteractor.setSearchQuery(query)
          },
          placeholder = { Text(stringResource(Res.string.battle_graph_search_placeholder), fontSize = 11.sp, color = RFColors.TextTertiary) },
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
            .width(340.dp)
            .height(56.dp),
          textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 11.sp,
            color = RFColors.TextPrimary
          ),
          singleLine = true,
          maxLines = 1,
          colors = TextFieldDefaults.colors(
            focusedContainerColor = RFColors.CardBackground,
            unfocusedContainerColor = RFColors.CardBackground,
            cursorColor = RFColors.TextPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Mode toggles
        Row(
          modifier = Modifier.width(340.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val modes = listOf(
            BattleGraphMode.DAMAGE to stringResource(Res.string.battle_graph_mode_damage),
            BattleGraphMode.HEALS to stringResource(Res.string.battle_graph_mode_heals),
            BattleGraphMode.CC to stringResource(Res.string.battle_graph_mode_cc)
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
                .weight(1f)
                .alpha(if (isSelected) 1f else 0.5f)
                .height(36.dp),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
              Text(
                text = label,
                color = if (isSelected) highlightColor else RFColors.TextSecondary,
                fontSize = 12.sp
              )
            }
          }
        }

        // Threshold slider - compact
        when (selectedMode) {
          BattleGraphMode.DAMAGE -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_dmg),
              initialValue = 0.1f,  // 25000 / 250_000
              multiplier = 250_000f,
              onValueChangeFinished = { v -> damageSlider = v },
              color = RFColors.dpsOrange,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.HEALS -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_heal),
              initialValue = 0.1f,  // 25000 / 250_000
              multiplier = 250_000f,
              onValueChangeFinished = { v -> healSlider = v },
              color = RFColors.healsGreen,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.CC -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_cc),
              initialValue = 0.02f,  // 5 / 250
              multiplier = 250f,
              onValueChangeFinished = { v -> ccSlider = v },
              color = RFColors.ccCyan,
              modifier = Modifier.width(340.dp)
            )
          }
        }

        // Max edges slider - below threshold
        Spacer(modifier = Modifier.height(8.dp))
        CompactThresholdSlider(
          label = stringResource(Res.string.battle_graph_max_edges),
          initialValue = 0.08f,  // 20 / 250
          multiplier = 250f,
          onValueChangeFinished = { v -> maxEdgesSlider = v },
          color = RFColors.TextPrimary,
          modifier = Modifier.width(340.dp)
        )

        // Play / Pause toggle
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
          onClick = { BattleGraphInteractor.togglePause() },
          modifier = Modifier
            .width(340.dp)
            .height(32.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
          Text(
            text = if (isPaused) stringResource(Res.string.battle_graph_resume) else stringResource(Res.string.battle_graph_pause),
            color = if (isPaused) RFColors.AccentRed else RFColors.TextPrimary,
            fontSize = 12.sp
          )
        }
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

      // PAUSED indicator in top-left corner
      if (isPaused) {
        Text(
          text = stringResource(Res.string.battle_graph_paused),
          color = RFColors.AccentRed,
          fontSize = 14.sp,
          fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(RFColors.CardBackground.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun CompactThresholdSlider(
  label: String,
  initialValue: Float,
  multiplier: Float,
  onValueChangeFinished: (Float) -> Unit,
  color: Color,
  modifier: Modifier = Modifier
) {
  var sliderValue by remember { mutableFloatStateOf(initialValue) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
  ) {
    Text(
      text = "$label:",
      color = RFColors.TextSecondary,
      fontSize = 10.sp,
      modifier = Modifier.width(64.dp)
    )
    Slider(
      value = sliderValue,
      onValueChange = { sliderValue = it },
      onValueChangeFinished = { onValueChangeFinished(sliderValue) },
      valueRange = 0f..1f,
      modifier = Modifier
        .weight(1f)
        .height(24.dp),
      colors = SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        inactiveTrackColor = RFColors.TextTertiary
      )
    )
    Text(
      text = formatThresholdValue(sliderValue * multiplier),
      color = color,
      fontSize = 10.sp,
      textAlign = TextAlign.End,
      modifier = Modifier.width(42.dp)
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
