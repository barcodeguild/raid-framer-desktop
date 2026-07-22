package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.definitions.blacklistedDebuffNames
import com.reoky.raidframer.core.definitions.blacklistedBuffNames
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
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_kills
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_buffs
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_debuffs
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_charms
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_distress
import raid_framer_desktop.composeapp.generated.resources.battle_graph_focused_silence
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
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_kills
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_buffs
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_debuffs
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_charms
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_distress
import raid_framer_desktop.composeapp.generated.resources.battle_graph_mode_silence
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_dmg
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_heal
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_cc
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_kills
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_buffs
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_debuffs
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_charms
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_distress
import raid_framer_desktop.composeapp.generated.resources.battle_graph_min_silence
import raid_framer_desktop.composeapp.generated.resources.battle_graph_max_edges
import raid_framer_desktop.composeapp.generated.resources.battle_graph_all_spells

@Composable
fun BattleGraphOverlay(wm: WindowManager?) {
  val graphData by BattleGraphInteractor.graphData.collectAsState()
  val selectedMode by BattleGraphInteractor.selectedMode.collectAsState()
  val isPaused by BattleGraphInteractor.isPaused.collectAsState()
  val dragLock = LocalDragLock.current

  // Slider positions 0..1 (mapped to actual values via multiplier)
  var damageSlider by remember { mutableFloatStateOf(0.125f) }     // 25000 / 200_000
  var healSlider by remember { mutableFloatStateOf(0.125f) }      // 25000 / 200_000
  var ccSlider by remember { mutableFloatStateOf(0.1f) }          // 5 / 50
  var searchQuery by remember { mutableStateOf("") }
  var maxEdgesSlider by remember { mutableFloatStateOf(0.267f) }   // 20 / 75
  var selectedPlayerName by remember { mutableStateOf<String?>(null) }

  // Debounced push to interactor — avoids recomposition during drag
  LaunchedEffect(Unit) {
    snapshotFlow { damageSlider }.debounce(300).collect { v -> BattleGraphInteractor.setDamageThreshold((v * 200_000f).toLong()) }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { healSlider }.debounce(300).collect { v -> BattleGraphInteractor.setHealThreshold((v * 200_000f).toLong()) }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { ccSlider }.debounce(300).collect { v -> BattleGraphInteractor.setCCThreshold((v * 50f).toInt()) }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { maxEdgesSlider }.debounce(300).collect { v -> BattleGraphInteractor.setMaxEdges((v * 75f).toInt()) }
  }

  Column(
    modifier = Modifier.fillMaxSize().background(Color(0xFF121212))
  ) {
    val titleText = when (selectedMode) {
      BattleGraphMode.DAMAGE -> stringResource(Res.string.battle_graph_focused_damage)
      BattleGraphMode.HEALS -> stringResource(Res.string.battle_graph_heal_prop)
      BattleGraphMode.CC -> stringResource(Res.string.battle_graph_crowd_control_distribution)
      BattleGraphMode.KILLS -> stringResource(Res.string.battle_graph_focused_kills)
      BattleGraphMode.BUFFS -> stringResource(Res.string.battle_graph_focused_buffs)
      BattleGraphMode.DEBUFFS -> stringResource(Res.string.battle_graph_focused_debuffs)
      BattleGraphMode.CHARMS -> stringResource(Res.string.battle_graph_focused_charms)
      BattleGraphMode.DISTRESS -> stringResource(Res.string.battle_graph_focused_distress)
      BattleGraphMode.SILENCE -> stringResource(Res.string.battle_graph_focused_silence)
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
          textStyle = TextStyle(
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

        // Row 1: DAMAGE, HEALS, CC
        ModeToggleRow(
          modes = listOf(
            BattleGraphMode.DAMAGE to stringResource(Res.string.battle_graph_mode_damage),
            BattleGraphMode.HEALS to stringResource(Res.string.battle_graph_mode_heals),
            BattleGraphMode.CC to stringResource(Res.string.battle_graph_mode_cc)
          ),
          selectedMode = selectedMode,
          onModeSelected = { BattleGraphInteractor.setMode(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2: KILLS, BUFFS, DEBUFFS
        ModeToggleRow(
          modes = listOf(
            BattleGraphMode.KILLS to stringResource(Res.string.battle_graph_mode_kills),
            BattleGraphMode.BUFFS to stringResource(Res.string.battle_graph_mode_buffs),
            BattleGraphMode.DEBUFFS to stringResource(Res.string.battle_graph_mode_debuffs)
          ),
          selectedMode = selectedMode,
          onModeSelected = { BattleGraphInteractor.setMode(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Row 3: CHARMS, DISTRESS, SILENCE
        ModeToggleRow(
          modes = listOf(
            BattleGraphMode.CHARMS to stringResource(Res.string.battle_graph_mode_charms),
            BattleGraphMode.DISTRESS to stringResource(Res.string.battle_graph_mode_distress),
            BattleGraphMode.SILENCE to stringResource(Res.string.battle_graph_mode_silence)
          ),
          selectedMode = selectedMode,
          onModeSelected = { BattleGraphInteractor.setMode(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Threshold slider for current mode
        when (selectedMode) {
          BattleGraphMode.DAMAGE -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_dmg),
              initialValue = 0.125f,  // 25000 / 200_000
              multiplier = 200_000f,
              minValue = 0.005f,  // minimum 1000 dmg
              onValueChangeFinished = { v -> damageSlider = v },
              color = RFColors.dpsOrange,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.HEALS -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_heal),
              initialValue = 0.125f,  // 25000 / 200_000
              multiplier = 200_000f,
              minValue = 0.005f,  // minimum 1000 heals
              onValueChangeFinished = { v -> healSlider = v },
              color = RFColors.healsGreen,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.CC -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_cc),
              initialValue = 0.1f,  // 5 / 50
              multiplier = 50f,
              minValue = 0.1f,  // minimum 5 CC
              onValueChangeFinished = { v -> ccSlider = v },
              color = RFColors.ccCyan,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.KILLS -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_kills),
              initialValue = 0.02f,  // 1 / 50
              multiplier = 50f,
              minValue = 0.02f,  // minimum 1 kill
              onValueChangeFinished = { v -> BattleGraphInteractor.setKillThreshold((v * 50f).toInt()) },
              color = RFColors.killsRed,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.BUFFS -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_buffs),
              initialValue = 0.02f,  // 1 / 50
              multiplier = 50f,
              minValue = 0.02f,  // minimum 1 buff
              onValueChangeFinished = { v -> BattleGraphInteractor.setBuffThreshold((v * 50f).toInt()) },
              color = RFColors.buffsBlue,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.DEBUFFS -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_debuffs),
              initialValue = 0.02f,  // 1 / 50
              multiplier = 50f,
              minValue = 0.02f,  // minimum 1 debuff
              onValueChangeFinished = { v -> BattleGraphInteractor.setDebuffThreshold((v * 50f).toInt()) },
              color = RFColors.debuffsPurple,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.CHARMS -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_charms),
              initialValue = 0.02f,  // 1 / 50
              multiplier = 50f,
              minValue = 0.02f,  // minimum 1 charm
              onValueChangeFinished = { v -> BattleGraphInteractor.setCharmThreshold((v * 50f).toInt()) },
              color = RFColors.charmPink,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.DISTRESS -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_distress),
              initialValue = 0.02f,  // 1 / 50
              multiplier = 50f,
              minValue = 0.02f,  // minimum 1 distress
              onValueChangeFinished = { v -> BattleGraphInteractor.setDistressThreshold((v * 50f).toInt()) },
              color = RFColors.distressPurple,
              modifier = Modifier.width(340.dp)
            )
          }
          BattleGraphMode.SILENCE -> {
            CompactThresholdSlider(
              label = stringResource(Res.string.battle_graph_min_silence),
              initialValue = 0.02f,  // 1 / 50
              multiplier = 50f,
              minValue = 0.02f,  // minimum 1 silence
              onValueChangeFinished = { v -> BattleGraphInteractor.setSilenceThreshold((v * 50f).toInt()) },
              color = RFColors.silencePurple,
              modifier = Modifier.width(340.dp)
            )
          }
        }

        // Spell filter dropdown for BUFFS/DEBUFFS
        if (selectedMode == BattleGraphMode.BUFFS || selectedMode == BattleGraphMode.DEBUFFS) {
          Spacer(modifier = Modifier.height(8.dp))
          val isDebuffMode = selectedMode == BattleGraphMode.DEBUFFS
          val blacklistedNames = if (isDebuffMode) blacklistedDebuffNames else blacklistedBuffNames
          val spellMap = remember(graphData.edges, selectedMode) {
            graphData.edges.flatMap { it.spellBreakdown.entries }
              .groupBy { it.key }
              .mapValues { it.value.sumOf { entry -> entry.value }.toInt() }
              .filterKeys { it !in blacklistedNames }
          }
          val sortedSpells = remember(spellMap) {
            spellMap.entries.sortedByDescending { it.value }.take(100)
          }
          val allLabel = stringResource(Res.string.battle_graph_all_spells)
          val options = remember(sortedSpells, allLabel) {
            listOf(null to allLabel) + sortedSpells.map { it.key to "${it.key} (${it.value})" }
          }
          SpellFilterDropdown(
            label = if (selectedMode == BattleGraphMode.BUFFS) "Buff" else "Debuff",
            options = options,
            color = if (selectedMode == BattleGraphMode.BUFFS) RFColors.buffsBlue else RFColors.debuffsPurple,
            onSpellSelected = { spell ->
              if (selectedMode == BattleGraphMode.BUFFS) BattleGraphInteractor.setSelectedBuffSpell(spell)
              else BattleGraphInteractor.setSelectedDebuffSpell(spell)
            },
            modifier = Modifier.width(340.dp)
          )
        }

        // Max edges slider - below threshold
        Spacer(modifier = Modifier.height(8.dp))
        CompactThresholdSlider(
          label = stringResource(Res.string.battle_graph_max_edges),
          initialValue = 0.267f,  // 20 / 75
          multiplier = 75f,
          minValue = 1f / 75f,  // minimum 1 edge
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
  minValue: Float = 0f,
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
      valueRange = minValue..1f,
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

@Composable
private fun ModeToggleRow(
  modes: List<Pair<BattleGraphMode, String>>,
  selectedMode: BattleGraphMode,
  onModeSelected: (BattleGraphMode) -> Unit
) {
  Row(
    modifier = Modifier.width(340.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    modes.forEach { (mode, label) ->
      val isSelected = mode == selectedMode
      val highlightColor = when (mode) {
        BattleGraphMode.DAMAGE -> RFColors.dpsOrange
        BattleGraphMode.HEALS -> RFColors.healsGreen
        BattleGraphMode.CC -> RFColors.ccCyan
        BattleGraphMode.KILLS -> RFColors.killsRed
        BattleGraphMode.BUFFS -> RFColors.buffsBlue
        BattleGraphMode.DEBUFFS -> RFColors.debuffsPurple
        BattleGraphMode.CHARMS -> RFColors.charmPink
        BattleGraphMode.DISTRESS -> RFColors.distressPurple
        BattleGraphMode.SILENCE -> RFColors.silencePurple
      }
      TextButton(
        onClick = { onModeSelected(mode) },
        modifier = Modifier
          .weight(1f)
          .alpha(if (isSelected) 1f else 0.5f)
          .height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
      ) {
        Text(
          text = label,
          color = if (isSelected) highlightColor else RFColors.TextSecondary,
          fontSize = 10.sp
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpellFilterDropdown(
  label: String,
  options: List<Pair<String?, String>>,
  color: Color,
  onSpellSelected: (String?) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  var selectedOption by remember { mutableStateOf(options.firstOrNull()) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier
  ) {
    OutlinedTextField(
      value = selectedOption?.second ?: "",
      onValueChange = {},
      readOnly = true,
      label = { Text(text = label, fontSize = 10.sp, color = RFColors.TextSecondary) },
      modifier = Modifier
        .width(340.dp)
        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = color,
        unfocusedBorderColor = RFColors.CardBorder,
        focusedTextColor = RFColors.TextPrimary,
        unfocusedTextColor = RFColors.TextPrimary,
        cursorColor = color,
        focusedLabelColor = RFColors.TextSecondary,
        unfocusedLabelColor = RFColors.TextTertiary
      ),
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      textStyle = TextStyle(fontSize = 11.sp),
      singleLine = true,
      maxLines = 1
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.width(340.dp),
      containerColor = RFColors.CardBackground,
      tonalElevation = 4.dp
    ) {
      options.forEach { (spell, display) ->
        DropdownMenuItem(
          text = {
            Text(
              text = display,
              color = if (spell == null) RFColors.TextTertiary else RFColors.TextPrimary,
              fontSize = 11.sp,
              maxLines = 2,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          },
          onClick = {
            selectedOption = Pair(spell, display)
            onSpellSelected(spell)
            expanded = false
          },
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        )
      }
    }
  }
}

private fun formatThresholdValue(value: Float): String {
  return when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format("%.0fk", value / 1_000.0)
    else -> value.toInt().toString()
  }
}
