package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.ConfigEntity
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.colorToSliderValue
import com.reoky.raidframer.core.helpers.sliderValueToColor
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.CombatLogInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.*
import java.util.Locale.getDefault
import kotlin.system.exitProcess

@Composable
private fun SettingsSection(
  title: String,
  description: String? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    shape = RoundedCornerShape(10.dp),
    color = RFColors.CardBackground,
    elevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
        .padding(16.dp)
    ) {
      Text(
        text = title,
        color = RFColors.AccentRed,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
      if (description != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          color = RFColors.TextSecondary,
          fontSize = 13.sp
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun SettingsCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  label: String,
  accent: Boolean = true
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp)
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(
        checkmarkColor = Color.White,
        checkedColor = if (accent) RFColors.AccentRed else RFColors.TextSecondary,
        uncheckedColor = RFColors.TextTertiary
      )
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = label,
      color = RFColors.TextPrimary,
      fontSize = 14.sp
    )
  }
}

@Composable
fun SettingsOverlay(wm: WindowManager? = null) {

  val config by RFConfig.state.collectAsState()
  val scrollState = rememberScrollState()
  val showUninstallConfirmDialog = remember { mutableStateOf(false) }
  val showUninstallDoneDialog = remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF121212))
      .verticalScroll(scrollState)
  ) {
    Column {
      TitleBarComponent(
        title = stringResource(Res.string.settings_title),
        onClose = { wm?.closeWindow(OverlayType.SETTINGS) }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Data Sourcing
      SettingsSection(
        title = stringResource(Res.string.settings_data_sourcing_title),
        description = stringResource(Res.string.settings_data_sourcing_description)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          if (config.defaultArcheRageDirectory.isNotBlank()) {
            Text(
              text = stringResource(Res.string.settings_arche_rage_directory_label),
              color = RFColors.TextSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = config.defaultArcheRageDirectory,
              color = Color(0xFF66BB6A),
              fontSize = 13.sp,
              modifier = Modifier.align(Alignment.CenterVertically)
            )
          } else {
            Text(
              text = stringResource(Res.string.settings_specify_directory_hint),
              color = RFColors.AccentRed,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Button(
            onClick = { wm?.openWindow(OverlayType.COMPANION) },
            colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = stringResource(Res.string.settings_lua_companion_options),
              maxLines = 1,
              color = Color.White,
              fontWeight = FontWeight.SemiBold
            )
          }
          Button(
            onClick = { wm?.openWindow(OverlayType.ABOUT) },
            colors = ButtonDefaults.buttonColors(RFColors.CardBorder),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = stringResource(Res.string.general_about),
              maxLines = 1,
              color = RFColors.TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      CharacterDisplayPanel()

      if (config.lastSessionStart > 0) {
        RecordingSessionPanel(config)
      }

      OverlayFeaturesPanel(wm)

      CombatOverlaySettingsPanel()

      // Uninstall :(
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        color = RFColors.CardBackground,
        elevation = 2.dp
      ) {
        Column(
          modifier = Modifier
            .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Button(
            onClick = { showUninstallConfirmDialog.value = true },
            colors = ButtonDefaults.buttonColors(Color(0xFFB71C1C))
          ) {
            Text(
              text = stringResource(Res.string.settings_uninstall_button),
              color = Color.White,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (showUninstallConfirmDialog.value) {
    AlertDialog(
      onDismissRequest = { showUninstallConfirmDialog.value = false },
      title = { Text(stringResource(Res.string.settings_uninstall_confirm_title), color = RFColors.TextPrimary) },
      text = { Text(stringResource(Res.string.settings_uninstall_confirm_text), color = RFColors.TextSecondary) },
      backgroundColor = RFColors.CardBackground,
      shape = RoundedCornerShape(10.dp),
      confirmButton = {
        Button(
          onClick = {
            showUninstallConfirmDialog.value = false
            CompanionInteractor.uninstall()
            showUninstallDoneDialog.value = true
          },
          colors = ButtonDefaults.buttonColors(Color(0xFFB71C1C))
        ) {
          Text(stringResource(Res.string.settings_uninstall_confirm_button), color = Color.White)
        }
      },
      dismissButton = {
        Button(
          onClick = { showUninstallConfirmDialog.value = false },
          colors = ButtonDefaults.buttonColors(RFColors.CardBorder)
        ) {
          Text(stringResource(Res.string.general_cancel), color = RFColors.TextPrimary)
        }
      }
    )
  }

  if (showUninstallDoneDialog.value) {
    AlertDialog(
      onDismissRequest = {
        Runtime.getRuntime().exec("control appwiz.cpl")
        exitProcess(0)
      },
      title = { Text(stringResource(Res.string.settings_uninstall_done_title), color = RFColors.TextPrimary) },
      text = { Text(stringResource(Res.string.settings_uninstall_done_text), color = RFColors.TextSecondary) },
      backgroundColor = RFColors.CardBackground,
      shape = RoundedCornerShape(10.dp),
      confirmButton = {
        Button(onClick = {
          Runtime.getRuntime().exec("control appwiz.cpl")
          exitProcess(0)
        }, colors = ButtonDefaults.buttonColors(RFColors.AccentRed)) {
          Text(stringResource(Res.string.general_exit), color = Color.White)
        }
      }
    )
  }
}

@Composable
private fun CharacterDisplayPanel() {
  val config by RFConfig.state.collectAsState()
  SettingsSection(
    title = stringResource(Res.string.settings_character_title),
    description = stringResource(Res.string.settings_character_description)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      TextField(
        value = config.playerName,
        onValueChange = { newValue ->
          RFConfig.update { config ->
            val newPlayerName = newValue.lowercase(getDefault())
              .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
            config.copy(playerName = newPlayerName)
          }
        },
        textStyle = TextStyle(
          textAlign = TextAlign.Center,
          fontSize = 20.sp,
          color = RFColors.TextPrimary
        ),
        singleLine = true,
        maxLines = 1,
        placeholder = { Text(stringResource(Res.string.settings_name_placeholder), color = RFColors.TextTertiary) },
        colors = TextFieldDefaults.textFieldColors(
          textColor = RFColors.TextPrimary,
          backgroundColor = Color(0xFF1E1E1E),
          focusedIndicatorColor = RFColors.AccentRed,
          unfocusedIndicatorColor = RFColors.CardBorder,
          placeholderColor = RFColors.TextTertiary,
          cursorColor = RFColors.AccentRed
        ),
        modifier = Modifier.width(220.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      val factionOptions = Faction.entries.filter { it != Faction.UNKNOWN }.map { it.value }
      Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
      ) {
        factionOptions.forEach { option ->
          Row(
            modifier = Modifier
              .padding(horizontal = 6.dp)
              .clickable { RFConfig.update { it.copy(playerFaction = option) } },
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = (option == config.playerFaction),
              onClick = { RFConfig.update { it.copy(playerFaction = option) } },
              colors = RadioButtonDefaults.colors(
                selectedColor = RFColors.AccentRed,
                unselectedColor = RFColors.TextTertiary
              )
            )
            Text(
              text = option,
              color = RFColors.TextPrimary,
              fontSize = 14.sp,
              modifier = Modifier.padding(start = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OverlayFeaturesPanel(wm: WindowManager? = null) {
  val config by RFConfig.state.collectAsState()
  val dragLock = LocalDragLock.current

  val sliderInteractionSource = remember { MutableInteractionSource() }
  val isSliderDragging = remember { mutableStateOf(false) }
  LaunchedEffect(sliderInteractionSource) {
    sliderInteractionSource.interactions.collect { interaction ->
      when (interaction) {
        is DragInteraction.Start -> {
          dragLock.value = true
          isSliderDragging.value = true
        }
        is DragInteraction.Stop,
        is DragInteraction.Cancel -> {
          dragLock.value = false
          isSliderDragging.value = false
        }
      }
    }
  }

  // General Settings
  SettingsSection(
    title = stringResource(Res.string.settings_general_title),
    description = stringResource(Res.string.settings_general_description)
  ) {
    SettingsCheckbox(
      checked = config.miniGraphEnabled,
      onCheckedChange = { isChecked ->
        CoroutineScope(Dispatchers.Main).launch {
          RFConfig.update { it.copy(miniGraphEnabled = isChecked) }
          if (isChecked) wm?.openWindow(OverlayType.MINI) else wm?.closeWindow(OverlayType.MINI)
        }
      },
      label = stringResource(Res.string.settings_mini_graph_description)
    )

    SettingsCheckbox(
      checked = config.splitChatEnabled,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(splitChatEnabled = isChecked) } },
      label = stringResource(Res.string.settings_split_chat),
      accent = false
    )

    SettingsCheckbox(
      checked = config.tabbedDetectionEnabled,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(tabbedDetectionEnabled = isChecked) } },
      label = stringResource(Res.string.settings_tabbed_detection)
    )

    SettingsCheckbox(
      checked = config.allowPVEDamage,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(allowPVEDamage = isChecked) } },
      label = stringResource(Res.string.settings_allow_pve_damage)
    )

    SettingsCheckbox(
      checked = config.gameScheduleHotkeyEnabled,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(gameScheduleHotkeyEnabled = isChecked) } },
      label = stringResource(Res.string.settings_game_schedule_hotkey),
      accent = false
    )

    SettingsCheckbox(
      checked = config.useSadlyDotEyeOhhh,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(useSadlyDotEyeOhhh = isChecked) } },
      label = stringResource(Res.string.settings_use_sadly),
      accent = false
    )

    SettingsCheckbox(
      checked = config.dragonBreathOverlayEnabled,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(dragonBreathOverlayEnabled = isChecked) } },
      label = stringResource(Res.string.settings_dragon_breath_overlay),
      accent = false
    )

    Spacer(modifier = Modifier.height(12.dp))
  }

  // General Overlay Settings
  SettingsSection(
    title = stringResource(Res.string.settings_general_overlay_title),
    description = stringResource(Res.string.settings_general_overlay_description)
  ) {
    Text(
      text = stringResource(Res.string.settings_general_opacity_slider),
      color = RFColors.TextSecondary,
      fontSize = 13.sp
    )
    Slider(
      value = config.windowOpacity,
      onValueChange = { newOpacity -> RFConfig.update { it.copy(windowOpacity = newOpacity) } },
      interactionSource = sliderInteractionSource,
      colors = SliderDefaults.colors(
        thumbColor = RFColors.AccentRed,
        activeTrackColor = RFColors.AccentRed,
        inactiveTrackColor = RFColors.CardBorder
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = stringResource(Res.string.settings_general_tint_slider),
      color = RFColors.TextSecondary,
      fontSize = 13.sp
    )

    val windowColorSlider = remember { mutableStateOf(colorToSliderValue(config.windowColor)) }
    LaunchedEffect(config.windowColor) {
      if (isSliderDragging.value) return@LaunchedEffect
      val newVal = colorToSliderValue(config.windowColor)
      val currentColorFromSlider = sliderValueToColor(windowColorSlider.value)
      if (currentColorFromSlider == config.windowColor) return@LaunchedEffect
      if (kotlin.math.abs(newVal - windowColorSlider.value) > 0.0001f) {
        windowColorSlider.value = newVal
      }
    }

    Slider(
      value = windowColorSlider.value,
      onValueChange = { value ->
        windowColorSlider.value = value
        RFConfig.update { it.copy(windowColor = sliderValueToColor(value)) }
      },
      interactionSource = sliderInteractionSource,
      colors = SliderDefaults.colors(
        thumbColor = RFColors.AccentRed,
        activeTrackColor = RFColors.AccentRed,
        inactiveTrackColor = RFColors.CardBorder
      )
    )
  }
}

@Composable
private fun CombatOverlaySettingsPanel() {
  val config by RFConfig.state.collectAsState()

  SettingsSection(
    title = stringResource(Res.string.settings_combat_overlay_title),
    description = stringResource(Res.string.settings_combat_fade_controls)
  ) {
    SettingsCheckbox(
      checked = config.combatShowDamageColumn,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatShowDamageColumn = isChecked) } },
      label = stringResource(Res.string.settings_show_damage_column)
    )

    SettingsCheckbox(
      checked = config.combatShowHealsColumn,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatShowHealsColumn = isChecked) } },
      label = stringResource(Res.string.settings_show_heals_column)
    )

    SettingsCheckbox(
      checked = config.combatShowCCColumn,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatShowCCColumn = isChecked) } },
      label = stringResource(Res.string.settings_show_cc_column)
    )

    SettingsCheckbox(
      checked = config.combatControlsFadeEnabled,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatControlsFadeEnabled = isChecked) } },
      label = stringResource(Res.string.settings_combat_fade_controls)
    )
  }
}

@Composable
private fun RecordingSessionPanel(config: ConfigEntity) {
  val mode = if (config.allowPVEDamage) "PvE" else "PvP"
  var elapsedSeconds by remember { mutableStateOf(0L) }

  LaunchedEffect(config.lastSessionStart) {
    while (config.lastSessionStart > 0) {
      elapsedSeconds = (System.currentTimeMillis() - config.lastSessionStart) / 1000
      delay(1000)
    }
  }

  val minutes = elapsedSeconds / 60
  val seconds = elapsedSeconds % 60
  val timeStr = String.format("%02d:%02d", minutes, seconds)

  SettingsSection(
    title = "Recording Session",
    description = "Combat events are being recorded to ${config.lastSessionTitle}"
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Column {
        Text(
          text = "Type: ${config.lastSessionType} ($mode)",
          color = RFColors.TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "Duration: $timeStr",
          color = RFColors.TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Button(
        onClick = {
          PlayerCacheInteractor.stopSession()
          RFConfig.update { it.copy(lastSessionStart = 0L) }
        },
        colors = ButtonDefaults.buttonColors(RFColors.AccentRed)
      ) {
        Text(
          text = "Stop Recording",
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp
        )
      }
    }
  }
}

@Preview
@Composable
fun PreviewSettings() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    SettingsOverlay()
  }
}