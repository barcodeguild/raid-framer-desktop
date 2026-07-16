package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.core.*
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.sin
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.reoky.raidframer.core.locale.AppLocale
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.colorToSliderValue
import com.reoky.raidframer.core.helpers.formatFileSize
import com.reoky.raidframer.core.helpers.getDirectorySizeBytes
import com.reoky.raidframer.core.helpers.getExportDirectory
import com.reoky.raidframer.core.helpers.sliderValueToColor
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.seedtable.SeedTableInteractor
import com.reoky.raidframer.core.seedtable.SeedTableStatus
import com.reoky.raidframer.core.model.CombatRankingCategory
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.helper.UpdateHelper
import com.reoky.raidframer.core.helper.UpdateStatus
import com.reoky.raidframer.core.helper.UpdateDownloader
import com.reoky.raidframer.core.helper.DownloadStatus
import com.reoky.raidframer.core.helper.UpdateInfo
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.PatchNotesComponent
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
  modifier: Modifier = Modifier,
  borderColor: Color = RFColors.CardBorder,
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    shape = RoundedCornerShape(10.dp),
    color = RFColors.CardBackground,
    elevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
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
  val showUninstallingDialog = remember { mutableStateOf(false) }
  val showUninstallDoneDialog = remember { mutableStateOf(false) }
  val uninstallScope = rememberCoroutineScope()

  // Auto-scroll to the update panel if opened from the update dialog
  LaunchedEffect(Unit) {
    if (UpdateHelper.shouldScrollToUpdate) {
      UpdateHelper.shouldScrollToUpdate = false
      kotlinx.coroutines.delay(500) // wait for full layout composition
      scrollState.animateScrollTo(scrollState.maxValue)
    }
  }

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
              color = RFColors.UpdateGreen,
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

      ExportSettingsPanel(wm)

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
            showUninstallingDialog.value = true
            uninstallScope.launch {
              CompanionInteractor.uninstall()
              showUninstallingDialog.value = false
              showUninstallDoneDialog.value = true
            }
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

  if (showUninstallingDialog.value) {
    AlertDialog(
      onDismissRequest = {},
      title = { Text(stringResource(Res.string.settings_uninstall_confirm_title), color = RFColors.TextPrimary) },
      text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = RFColors.AccentRed,
            strokeWidth = 2.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(stringResource(Res.string.settings_uninstall_shutting_down), color = RFColors.TextSecondary)
        }
      },
      backgroundColor = RFColors.CardBackground,
      shape = RoundedCornerShape(10.dp),
      confirmButton = {},
      dismissButton = {}
    )
  }

  if (showUninstallDoneDialog.value) {
    AlertDialog(
      onDismissRequest = {
        ProcessBuilder("control", "appwiz.cpl").start()
        exitProcess(0)
      },
      title = { Text(stringResource(Res.string.settings_uninstall_done_title), color = RFColors.TextPrimary) },
      text = { Text(stringResource(Res.string.settings_uninstall_done_text), color = RFColors.TextSecondary) },
      backgroundColor = RFColors.CardBackground,
      shape = RoundedCornerShape(10.dp),
      confirmButton = {
        Button(onClick = {
          ProcessBuilder("control", "appwiz.cpl").start()
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

    SettingsSection(
      title = stringResource(Res.string.settings_language_label),
      description = stringResource(Res.string.settings_language_restart_notice)
    ) {
      LanguageDropdown(currentCode = config.preferredLanguage)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(currentCode: String) {
  var expanded by remember { mutableStateOf(false) }
  val currentLabel = AppLocale.entryFor(currentCode).nativeLabel

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
  ) {
    OutlinedTextField(
      value = currentLabel,
      onValueChange = {},
      readOnly = true,
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RFColors.AccentRed,
        unfocusedBorderColor = RFColors.CardBorder,
        focusedTextColor = RFColors.TextPrimary,
        unfocusedTextColor = RFColors.TextPrimary,
        cursorColor = RFColors.AccentRed
      ),
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      }
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth(),
      containerColor = RFColors.CardBackground,
      tonalElevation = 4.dp
    ) {
      (listOf(AppLocale.SYSTEM_DEFAULT) + AppLocale.ENTRIES).forEach { entry ->
        DropdownMenuItem(
          text = { Text(text = entry.nativeLabel, color = RFColors.TextPrimary) },
          onClick = {
            RFConfig.update { it.copy(preferredLanguage = entry.code) }
            expanded = false
          },
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombatOverlaySettingsPanel() {
  val config by RFConfig.state.collectAsState()

  val categoryOptions = listOf("") + CombatRankingCategory.ALL_CATEGORIES.map { it.name }

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

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = stringResource(Res.string.settings_combat_custom_categories_title),
      color = RFColors.AccentRed,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = stringResource(Res.string.settings_combat_custom_categories_description),
      color = RFColors.TextPrimary,
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    CategoryDropdown(
      label = stringResource(Res.string.settings_combat_custom_category_1),
      selectedCategory = config.combatCustomCategory1,
      options = categoryOptions,
      onCategorySelected = { cat -> RFConfig.update { it.copy(combatCustomCategory1 = cat) } }
    )

    Spacer(modifier = Modifier.height(4.dp))

    CategoryDropdown(
      label = stringResource(Res.string.settings_combat_custom_category_2),
      selectedCategory = config.combatCustomCategory2,
      options = categoryOptions,
      onCategorySelected = { cat -> RFConfig.update { it.copy(combatCustomCategory2 = cat) } }
    )

    Spacer(modifier = Modifier.height(4.dp))

    CategoryDropdown(
      label = stringResource(Res.string.settings_combat_custom_category_3),
      selectedCategory = config.combatCustomCategory3,
      options = categoryOptions,
      onCategorySelected = { cat -> RFConfig.update { it.copy(combatCustomCategory3 = cat) } }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
  label: String,
  selectedCategory: String,
  options: List<String>,
  onCategorySelected: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  val displayLabel = if (selectedCategory.isBlank()) {
    stringResource(Res.string.category_none)
  } else {
    CombatRankingCategory.fromString(selectedCategory)?.let { category ->
      stringResource(category.displayNameRes)
    } ?: selectedCategory
  }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
  ) {
    OutlinedTextField(
      value = displayLabel,
      onValueChange = {},
      readOnly = true,
      label = { Text(text = label, fontSize = 12.sp, color = RFColors.TextSecondary) },
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RFColors.AccentRed,
        unfocusedBorderColor = RFColors.CardBorder,
        focusedTextColor = RFColors.TextPrimary,
        unfocusedTextColor = RFColors.TextPrimary,
        cursorColor = RFColors.AccentRed,
        focusedLabelColor = RFColors.TextSecondary,
        unfocusedLabelColor = RFColors.TextTertiary
      ),
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      }
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth(),
      containerColor = RFColors.CardBackground,
      tonalElevation = 4.dp
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = {
            Text(
              text = if (option.isBlank()) stringResource(Res.string.category_none)
              else CombatRankingCategory.fromString(option)?.let { stringResource(it.displayNameRes) }
              ?: option,
              color = RFColors.TextPrimary
            )
          },
          onClick = {
            onCategorySelected(option)
            expanded = false
          },
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        )
      }
    }
  }
}

@Composable
private fun ExportSettingsPanel(wm: WindowManager?) {
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
            color = if (exportDir != null) RFColors.UpdateGreen else RFColors.AccentRed,
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
            wm?.closeWindow(OverlayType.SETTINGS)
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
  var downloadStatus by remember { mutableStateOf<DownloadStatus?>(null) }
  val scope = rememberCoroutineScope()
  val config by RFConfig.state.collectAsState()
  val pendingUpdate by UpdateHelper.pendingUpdate.collectAsState()

  // Golden sheen animation — trigger on either manual check or startup check
  val hasUpdate = updateStatus is UpdateStatus.Available || pendingUpdate != null
  var showSheen by remember { mutableStateOf(hasUpdate) }
  var sheenElapsed by remember { mutableStateOf(0f) }

  LaunchedEffect(hasUpdate) {
    if (hasUpdate) {
      showSheen = true
      sheenElapsed = 0f
      val start = System.nanoTime()
      while (showSheen) {
        kotlinx.coroutines.delay(50)
        sheenElapsed = (System.nanoTime() - start) / 1_000_000_000f
        if (sheenElapsed >= 7f) {
          showSheen = false
          break
        }
      }
    } else {
      showSheen = false
    }
  }

  val sectionBorderColor = if (showSheen) {
    val cycle = (sheenElapsed % 1.5f) / 1.5f
    val pulse = (sin(cycle * Math.PI.toFloat()) * 0.3f + 0.35f).coerceIn(0f, 1f)
    RFColors.UpdateGold.copy(alpha = pulse)
  } else RFColors.CardBorder

  SettingsSection(
    title = stringResource(Res.string.settings_about_title),
    description = stringResource(Res.string.settings_about_github_note),
    borderColor = sectionBorderColor
  ) {
    // Auto-update toggle
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(Res.string.settings_auto_update_toggle),
        color = RFColors.TextSecondary,
        fontSize = 13.sp
      )
      Switch(
        checked = config.autoUpdateEnabled,
        onCheckedChange = { RFConfig.update { it.copy(autoUpdateEnabled = it.autoUpdateEnabled.not()) } },
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = RFColors.AccentRed,
          uncheckedThumbColor = RFColors.TextTertiary,
          uncheckedTrackColor = RFColors.BadgeBackground
        )
      )
    }

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
            text = stringResource(Res.string.settings_update_available, status.updateInfo.version),
            color = RFColors.UpdateGreen,
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

        // Download progress
        val ds = downloadStatus
        if (ds != null) {
          Spacer(modifier = Modifier.height(6.dp))
          when (ds) {
            is DownloadStatus.Progress -> {
              LinearProgressIndicator(
                progress = ds.percent / 100f,
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = RFColors.AccentRed,
                backgroundColor = RFColors.BadgeBackground
              )
              Spacer(modifier = Modifier.height(2.dp))
              val downloadedStr = formatFileSize(ds.bytesDownloaded)
              val totalStr = if (ds.totalBytes > 0) formatFileSize(ds.totalBytes) else "?"
              Text(
                text = stringResource(Res.string.settings_update_downloading, ds.percent.toInt(), downloadedStr, totalStr),
                color = RFColors.TextSecondary,
                fontSize = 11.sp
              )
            }
            is DownloadStatus.Verifying -> Text(
              text = stringResource(Res.string.settings_update_verifying),
              color = RFColors.TextSecondary,
              fontSize = 11.sp
            )
            is DownloadStatus.Installing -> Text(
              text = stringResource(Res.string.settings_update_installing),
              color = RFColors.UpdateGreen,
              fontSize = 11.sp
            )
            is DownloadStatus.Error -> Text(
              text = stringResource(Res.string.settings_update_download_failed, ds.message),
              color = RFColors.AccentRed,
              fontSize = 11.sp
            )
            is DownloadStatus.Success -> Text(
              text = stringResource(Res.string.settings_update_install_complete),
              color = RFColors.UpdateGreen,
              fontSize = 11.sp
            )
            is DownloadStatus.Cancelled -> Text(
              text = stringResource(Res.string.settings_update_cancelled),
              color = RFColors.TextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }
      Spacer(modifier = Modifier.width(12.dp))

      // Show "Download & Install" when update is available, otherwise "Check for Updates"
      if (updateStatus is UpdateStatus.Available && downloadStatus == null) {
        Button(
          onClick = {
            val info = (updateStatus as UpdateStatus.Available).updateInfo
            downloadStatus = DownloadStatus.Progress(0f, 0L, 0L)
            scope.launch(Dispatchers.IO) {
              val result = UpdateDownloader.downloadAndInstall(info) { status ->
                scope.launch(Dispatchers.Main) { downloadStatus = status }
              }
              scope.launch(Dispatchers.Main) {
                downloadStatus = result
                if (result is DownloadStatus.Success || result is DownloadStatus.Installing) {
                  // MSI is running in its own process — exit now so the installer can replace files
                  // The updater thread in UpdateDownloader will attempt a relaunch after install completes.
                  kotlinx.coroutines.delay(500) // brief pause so the user sees "Installing..." status
                  kotlin.system.exitProcess(0)
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(RFColors.AccentRed)
        ) {
          Text(
            text = stringResource(Res.string.settings_update_download_install_button),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }
      } else if (downloadStatus is DownloadStatus.Progress || downloadStatus is DownloadStatus.Verifying) {
        Button(
          onClick = { UpdateDownloader.cancel() },
          colors = ButtonDefaults.buttonColors(RFColors.TextTertiary)
        ) {
          Text(
            text = stringResource(Res.string.general_cancel),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }
      } else {
        Button(
          onClick = {
            updateStatus = UpdateStatus.Checking
            downloadStatus = null
            scope.launch(Dispatchers.IO) {
              UpdateHelper.checkForUpdates { status ->
                scope.launch(Dispatchers.Main) { updateStatus = status }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
          enabled = downloadStatus == null || downloadStatus is DownloadStatus.Error || downloadStatus is DownloadStatus.Success || downloadStatus is DownloadStatus.Cancelled
        ) {
          Text(
            text = stringResource(Res.string.settings_about_check_updates_button),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }
      }
    }

    if (updateStatus is UpdateStatus.Available) {
      Spacer(modifier = Modifier.height(8.dp))
      val releaseUrl = (updateStatus as UpdateStatus.Available).updateInfo.releaseUrl
      Text(
        text = releaseUrl,
        color = RFColors.LinkBlue,
        fontSize = 12.sp,
        modifier = Modifier.clickable {
          if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(java.net.URI(releaseUrl))
          }
        }
      )

      val releaseNotes = (updateStatus as UpdateStatus.Available).updateInfo.releaseNotes
      PatchNotesComponent(releaseNotes)
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
    title = stringResource(Res.string.settings_recording_session_title),
    description = stringResource(Res.string.settings_recording_session_description_format, config.lastSessionTitle)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Column {
        Text(
          text = stringResource(Res.string.settings_recording_session_type_format, config.lastSessionType, mode),
          color = RFColors.TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = stringResource(Res.string.settings_recording_session_duration_format, timeStr),
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
          text = stringResource(Res.string.settings_recording_session_stop),
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
          color = if (s.isStale) RFColors.AccentRed else RFColors.UpdateGreen,
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