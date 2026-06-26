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
import com.reoky.raidframer.core.helpers.formatFileSize
import com.reoky.raidframer.core.helpers.getDirectorySizeBytes
import com.reoky.raidframer.core.helpers.getExportDirectory
import com.reoky.raidframer.core.helpers.sliderValueToColor
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.CombatLogInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.seedtable.SeedTableInteractor
import com.reoky.raidframer.core.seedtable.SeedTableStatus
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.helper.UpdateHelper
import com.reoky.raidframer.core.helper.UpdateStatus
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
import java.awt.Desktop
import java.text.SimpleDateFormat
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

      ExportSettingsPanel()

      SeedTableSettingsPanel(wm)

      VersionPanel()

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

//    SettingsCheckbox(
//      checked = config.splitChatEnabled,
//      onCheckedChange = { isChecked -> RFConfig.update { it.copy(splitChatEnabled = isChecked) } },
//      label = stringResource(Res.string.settings_split_chat),
//      accent = false
//    )

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

//    SettingsCheckbox(
//      checked = config.gameScheduleHotkeyEnabled,
//      onCheckedChange = { isChecked -> RFConfig.update { it.copy(gameScheduleHotkeyEnabled = isChecked) } },
//      label = stringResource(Res.string.settings_game_schedule_hotkey),
//      accent = false
//    )

//    SettingsCheckbox(
//      checked = config.useSadlyDotEyeOhhh,
//      onCheckedChange = { isChecked -> RFConfig.update { it.copy(useSadlyDotEyeOhhh = isChecked) } },
//      label = stringResource(Res.string.settings_use_sadly),
//      accent = false
//    )

//    SettingsCheckbox(
//      checked = config.dragonBreathOverlayEnabled,
//      onCheckedChange = { isChecked -> RFConfig.update { it.copy(dragonBreathOverlayEnabled = isChecked) } },
//      label = stringResource(Res.string.settings_dragon_breath_overlay),
//      accent = false
//    )

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
private fun ExportSettingsPanel() {
  val config by RFConfig.state.collectAsState()
  val exportDir = getExportDirectory()
  val directorySize = remember(exportDir) {
    if (exportDir != null) getDirectorySizeBytes(exportDir) else 0L
  }

  SettingsSection(
    title = stringResource(Res.string.settings_export_title),
    description = stringResource(Res.string.settings_export_description)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(Res.string.settings_export_directory_label),
            color = RFColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = exportDir ?: stringResource(Res.string.settings_export_directory_not_found),
            color = if (exportDir != null) Color(0xFF66BB6A) else RFColors.AccentRed,
            fontSize = 13.sp
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${stringResource(Res.string.settings_export_size_label)} ${formatFileSize(directorySize)}",
          color = RFColors.TextSecondary,
          fontSize = 12.sp
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Button(
        onClick = {
          if (exportDir != null) {
            Desktop.getDesktop().open(java.io.File(exportDir))
          }
        },
        colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
        enabled = exportDir != null
      ) {
        Text(
          text = stringResource(Res.string.settings_export_open_folder),
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    SettingsCheckbox(
      checked = config.exportIncludeRawJsonLogs,
      onCheckedChange = { isChecked -> RFConfig.update { it.copy(exportIncludeRawJsonLogs = isChecked) } },
      label = stringResource(Res.string.settings_export_include_raw_json),
      accent = true
    )
  }
}

@Composable
private fun VersionPanel() {
  val currentVersion = AppGlobals.APP_VERSION
  var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
  val scope = rememberCoroutineScope()

  SettingsSection(
    title = stringResource(Res.string.settings_about_title),
    description = stringResource(Res.string.settings_about_github_note)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(Res.string.settings_about_version_label),
            color = RFColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = currentVersion,
            color = RFColors.TextPrimary,
            fontSize = 13.sp
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        when (val status = updateStatus) {
          is UpdateStatus.Available -> Text(
            text = stringResource(Res.string.settings_update_available, status.newVersion),
            color = Color(0xFF66BB6A),
            fontSize = 12.sp
          )
          is UpdateStatus.Checking -> Text(
            text = stringResource(Res.string.settings_update_checking),
            color = RFColors.TextSecondary,
            fontSize = 12.sp
          )
          is UpdateStatus.UpToDate -> Text(
            text = stringResource(Res.string.settings_update_up_to_date),
            color = RFColors.TextSecondary,
            fontSize = 12.sp
          )
          is UpdateStatus.Error -> Text(
            text = stringResource(Res.string.settings_update_check_failed),
            color = RFColors.AccentRed,
            fontSize = 12.sp
          )
          is UpdateStatus.Idle -> {}
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Button(
        onClick = {
          updateStatus = UpdateStatus.Checking
          scope.launch(Dispatchers.IO) {
            UpdateHelper.checkForUpdates { status ->
              scope.launch(Dispatchers.Main) { updateStatus = status }
            }
          }
        },
        colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
        enabled = updateStatus is UpdateStatus.Idle || updateStatus is UpdateStatus.Error || updateStatus is UpdateStatus.UpToDate
      ) {
        Text(
          text = stringResource(Res.string.settings_about_check_updates_button),
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp
        )
      }
    }

    if (updateStatus is UpdateStatus.Available) {
      Spacer(modifier = Modifier.height(8.dp))
      val releaseUrl = (updateStatus as UpdateStatus.Available).releaseUrl
      Text(
        text = releaseUrl,
        color = Color(0xFF64B5F6),
        fontSize = 12.sp,
        modifier = Modifier.clickable {
          if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(java.net.URI(releaseUrl))
          }
        }
      )
    }
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

@Composable
private fun SeedTableSettingsPanel(wm: WindowManager? = null) {
  val status by SeedTableInteractor.status.collectAsState()

  SettingsSection(
    title = stringResource(Res.string.settings_seed_table_title),
    description = stringResource(Res.string.settings_seed_table_description)
  ) {
    when (val s = status) {
      is SeedTableStatus.None -> {
        Text(
          text = stringResource(Res.string.settings_seed_table_none),
          color = RFColors.TextSecondary,
          fontSize = 13.sp
        )
      }
      is SeedTableStatus.Applied -> {
        val days = s.ageMs / (24 * 60 * 60 * 1000)
        val hours = (s.ageMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val createdDate = SimpleDateFormat("MMM d, yyyy", getDefault()).format(java.util.Date(System.currentTimeMillis() - s.ageMs))
        val dateStr = String.format(stringResource(Res.string.settings_seed_table_date_format), createdDate, days, hours)
        Text(
          text = String.format(stringResource(Res.string.settings_seed_table_applied), s.playerCount, dateStr),
          color = if (s.isStale) RFColors.AccentRed else Color(0xFF66BB6A),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = {
          wm?.closeWindow(OverlayType.SETTINGS)
          SeedTableInteractor.showExportFileChooser { file ->
            SeedTableInteractor.exportSeedTable(file)
            wm?.openWindow(OverlayType.SETTINGS)
          }
        },
        colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
        modifier = Modifier.weight(1f)
      ) {
        Text(stringResource(Res.string.settings_seed_table_export), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      }
      Button(
        onClick = {
          wm?.closeWindow(OverlayType.SETTINGS)
          SeedTableInteractor.showImportFileChooser { file ->
            SeedTableInteractor.importSeedTable(file)
            wm?.openWindow(OverlayType.SETTINGS)
          }
        },
        colors = ButtonDefaults.buttonColors(RFColors.CardBorder),
        modifier = Modifier.weight(1f)
      ) {
        Text(stringResource(Res.string.settings_seed_table_import), color = RFColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      }
      if (status is SeedTableStatus.Applied) {
        Button(
          onClick = { SeedTableInteractor.removeSeedTable() },
          colors = ButtonDefaults.buttonColors(Color(0xFFB71C1C)),
          modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(Res.string.settings_seed_table_remove), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
      }
    }
  }
}