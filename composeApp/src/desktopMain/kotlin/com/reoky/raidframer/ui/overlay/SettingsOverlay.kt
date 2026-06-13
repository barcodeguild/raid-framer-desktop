package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.settings_data_sourcing_description
import raid_framer_desktop.composeapp.generated.resources.settings_data_sourcing_title
import raid_framer_desktop.composeapp.generated.resources.settings_title
import raid_framer_desktop.composeapp.generated.resources.settings_uninstall_done_text
import raid_framer_desktop.composeapp.generated.resources.settings_arche_rage_directory_label
import raid_framer_desktop.composeapp.generated.resources.settings_specify_directory_hint
import raid_framer_desktop.composeapp.generated.resources.settings_lua_companion_options
import raid_framer_desktop.composeapp.generated.resources.general_about
import raid_framer_desktop.composeapp.generated.resources.settings_mini_graph_description
import raid_framer_desktop.composeapp.generated.resources.settings_split_chat
import raid_framer_desktop.composeapp.generated.resources.settings_tabbed_detection
import raid_framer_desktop.composeapp.generated.resources.settings_allow_pve_damage
import raid_framer_desktop.composeapp.generated.resources.settings_game_schedule_hotkey
import raid_framer_desktop.composeapp.generated.resources.settings_use_sadly
import raid_framer_desktop.composeapp.generated.resources.settings_dragon_breath_overlay
import raid_framer_desktop.composeapp.generated.resources.settings_uninstall_confirm_title
import raid_framer_desktop.composeapp.generated.resources.settings_uninstall_confirm_text
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.toArgb
import com.reoky.raidframer.core.helpers.colorToSliderValue
import com.reoky.raidframer.core.helpers.sliderValueToColor
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.ui.LocalDragLock
import java.util.Locale.getDefault
import kotlin.system.exitProcess
import raid_framer_desktop.composeapp.generated.resources.general_cancel
import raid_framer_desktop.composeapp.generated.resources.general_exit
import raid_framer_desktop.composeapp.generated.resources.settings_character_description
import raid_framer_desktop.composeapp.generated.resources.settings_character_title
import raid_framer_desktop.composeapp.generated.resources.settings_combat_fade_controls
import raid_framer_desktop.composeapp.generated.resources.settings_combat_overlay_title
import raid_framer_desktop.composeapp.generated.resources.settings_general_description
import raid_framer_desktop.composeapp.generated.resources.settings_general_opacity_slider
import raid_framer_desktop.composeapp.generated.resources.settings_general_tint_slider
import raid_framer_desktop.composeapp.generated.resources.settings_general_title
import raid_framer_desktop.composeapp.generated.resources.settings_name_placeholder
import raid_framer_desktop.composeapp.generated.resources.settings_show_cc_column
import raid_framer_desktop.composeapp.generated.resources.settings_show_damage_column
import raid_framer_desktop.composeapp.generated.resources.settings_show_heals_column
import raid_framer_desktop.composeapp.generated.resources.settings_uninstall_button
import raid_framer_desktop.composeapp.generated.resources.settings_uninstall_confirm_button
import raid_framer_desktop.composeapp.generated.resources.settings_uninstall_done_title

@Composable
fun SettingsOverlay(wm: WindowManager? = null) {

  val config by RFConfig.state.collectAsState() // single source of truth

  val scrollState = rememberScrollState()
  val showDialog = remember { mutableStateOf(false) }
  val showUninstallConfirmDialog = remember { mutableStateOf(false) }
  val showUninstallDoneDialog = remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
  ) {
    Column {
      TitleBarComponent(
        title = stringResource(Res.string.settings_title),
        onClose = { wm?.closeWindow(OverlayType.SETTINGS) }
      )
      Box(modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
        Column {

          // Data Sourcing Section
          Row(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
            Text(
              text = stringResource(Res.string.settings_data_sourcing_title),
              color = Color.White,
              textAlign = TextAlign.Start,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
              text = stringResource(Res.string.settings_data_sourcing_description),
              color = Color.White,
              textAlign = TextAlign.Start,
              fontWeight = FontWeight.W400,
              fontSize = 14.sp
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (config.defaultArcheRageDirectory.isNotBlank()) {
              Text(
                text = stringResource(Res.string.settings_arche_rage_directory_label),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp).align(Alignment.CenterVertically)
              )
              Text(
                text = config.defaultArcheRageDirectory,
                color = Color.Green,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
              )
            } else {
              Text(
                text = stringResource(Res.string.settings_specify_directory_hint),
                color = Color.Red,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
              )
            }
          }

          Row(modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = { wm?.openWindow(OverlayType.COMPANION) },
              colors = ButtonDefaults.buttonColors(Color.White),
              modifier = Modifier.weight(1f).padding(16.dp)
            ) {
              Text(
                text = stringResource(Res.string.settings_lua_companion_options),
                maxLines = 1,
                color = Color.Black
              )
            }
            Button(
              onClick = {
                wm?.openWindow(OverlayType.ABOUT)
              },
              colors = ButtonDefaults.buttonColors(Color.White),
              modifier = Modifier.weight(1f).padding(16.dp)
            ) {
              Text(
                text = stringResource(Res.string.general_about),
                maxLines = 1,
                color = Color.Black
              )
            }
          }
        }
      }

      Divider(color = Color.DarkGray, thickness = 1.dp)

      GlobalOptionsPanel(wm)

      Divider(color = Color.DarkGray, thickness = 1.dp)

      CombatOverlaySettingsPanel(wm)

      Divider(color = Color.DarkGray, thickness = 1.dp)

      Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
        Button(
          onClick = { showUninstallConfirmDialog.value = true },
          colors = ButtonDefaults.buttonColors(Color.Red)
        ) {
          Text(text = stringResource(Res.string.settings_uninstall_button), color = Color.White)
        }
      }
    }
  }

  //FileSelectionDialog(showDialog, config.defaultArcheRageDirectory)

  if (showUninstallConfirmDialog.value) {
    AlertDialog(
      onDismissRequest = { showUninstallConfirmDialog.value = false },
      title = { Text(stringResource(Res.string.settings_uninstall_confirm_title), color = Color.White) },
      text = { Text(stringResource(Res.string.settings_uninstall_confirm_text), color = Color.White) },
      backgroundColor = Color.DarkGray,
      confirmButton = {
        Button(
          onClick = {
            showUninstallConfirmDialog.value = false
            CompanionInteractor.uninstall()
            showUninstallDoneDialog.value = true
          },
          colors = ButtonDefaults.buttonColors(Color.Red)
        ) {
          Text(stringResource(Res.string.settings_uninstall_confirm_button), color = Color.White)
        }
      },
      dismissButton = {
        Button(onClick = { showUninstallConfirmDialog.value = false }, colors = ButtonDefaults.buttonColors(Color.Gray)) {
          Text(stringResource(Res.string.general_cancel), color = Color.White)
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
      title = { Text(stringResource(Res.string.settings_uninstall_done_title), color = Color.White) },
      text = { Text(stringResource(Res.string.settings_uninstall_done_text), color = Color.White) },
      backgroundColor = Color.DarkGray,
      confirmButton = {
        Button(onClick = {
          Runtime.getRuntime().exec("control appwiz.cpl")
          exitProcess(0)
        }, colors = ButtonDefaults.buttonColors(Color.White)) {
          Text(stringResource(Res.string.general_exit), color = Color.Black)
        }
      }
    )
  }
}

@Composable
fun GlobalOptionsPanel(wm: WindowManager? = null) {
  val config by RFConfig.state.collectAsState() // single source of truth
  val dragLock = LocalDragLock.current

  // Lock window dragging while the slider thumb is being dragged
  val sliderInteractionSource = remember { MutableInteractionSource() }
  // track whether the slider is actively being dragged so we don't sync the local slider
  // value from the global config mid-drag (and avoid the thumb jumping on release).
  val isSliderDragging = remember { mutableStateOf(false) }
  LaunchedEffect(sliderInteractionSource) {
    sliderInteractionSource.interactions.collect { interaction ->
      when (interaction) {
        is DragInteraction.Start -> {
          dragLock.value = true
          isSliderDragging.value = true
        }
        is DragInteraction.Stop, is DragInteraction.Cancel -> {
          dragLock.value = false
          isSliderDragging.value = false
        }
      }
    }
  }
  Box(
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .wrapContentSize()
  ) {
    Column {
      Text(
        text = stringResource(Res.string.settings_character_title),
        color = Color.White,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier
          .padding(bottom = 8.dp)
      )
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column {
            Text(
              text = stringResource(Res.string.settings_character_description),
              color = Color.White
            )
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
                fontSize = 20.sp
              ),
              singleLine = true,
              maxLines = 1,
              placeholder = { Text(stringResource(Res.string.settings_name_placeholder)) },
              colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Red,
                unfocusedIndicatorColor = Color.White,
                placeholderColor = Color.LightGray,
                cursorColor = Color.Red
              ),
              modifier = Modifier.width(200.dp).align(Alignment.CenterHorizontally)
            )

            // same as listing the three factions manually, except kept in sync with the enum
            val factionOptions = Faction.entries.filter { it != Faction.UNKNOWN }.map { it. value }

            Row(
              modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally),
              verticalAlignment = Alignment.CenterVertically
            ) {
              factionOptions.forEach { option ->
                Row(
                  modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable {
                      RFConfig.update { it.copy(playerFaction = option) }
                    },
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  RadioButton(
                    selected = (option == config.playerFaction),
                    onClick = {
                      RFConfig.update { it.copy(playerFaction = option) }
                    },
                    colors = RadioButtonDefaults.colors(
                      selectedColor = Color.Red,
                      unselectedColor = Color.White
                    )
                  )
                  Text(
                        text = option,
                    color = Color.White,
                    modifier = Modifier.padding(start = 4.dp)
                  )
                }
              }
            }
          }
        }
        Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
        Column {
          Text(
            text = stringResource(Res.string.settings_general_title),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Text(
            text = stringResource(Res.string.settings_general_description),
            color = Color.White,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.W400,
            fontSize = 14.sp
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
          Text(
            text = stringResource(Res.string.settings_general_opacity_slider),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
          )
          Slider(
            value = config.windowOpacity,
            onValueChange = { newOpacity -> RFConfig.update { it.copy(windowOpacity = newOpacity) } },
            interactionSource = sliderInteractionSource,
            modifier = Modifier
              .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
              thumbColor = Color.Red,
              activeTrackColor = Color.Red,
              inactiveTrackColor = Color.Gray
            )
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
          Text(
            text = stringResource(Res.string.settings_general_tint_slider),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
          )

          val windowColorSlider = remember { mutableStateOf(colorToSliderValue(config.windowColor)) }
          LaunchedEffect(config.windowColor) {
            if (isSliderDragging.value) return@LaunchedEffect
            val newVal = colorToSliderValue(config.windowColor)
            val currentColorFromSlider = sliderValueToColor(windowColorSlider.value)
            if (currentColorFromSlider == config.windowColor) return@LaunchedEffect
            if (kotlin.math.abs(newVal - windowColorSlider.value) > 0.0001f) { // 0 is black, 1 is white
              windowColorSlider.value = newVal
            }
          }

          Slider(
            value = windowColorSlider.value,
            onValueChange = { value ->
              windowColorSlider.value = value
              RFConfig.update { it.copy(windowColor = sliderValueToColor(value)) } // set color continuously
            },
            interactionSource = sliderInteractionSource,
            modifier = Modifier.padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
              thumbColor = Color.Red,
              activeTrackColor = Color.Red,
              inactiveTrackColor = Color.Gray
            )
          )
        }

        Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
          Checkbox(
            checked = config.miniGraphEnabled,
            onCheckedChange = { isChecked ->
              CoroutineScope(Dispatchers.Main).launch {
                RFConfig.update { it.copy(miniGraphEnabled = isChecked) }
                if (isChecked) wm?.openWindow(OverlayType.MINI) else wm?.closeWindow(OverlayType.MINI)
              }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = stringResource(Res.string.settings_mini_graph_description),
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
          Checkbox(
            checked = config.splitChatEnabled,
            onCheckedChange = { isChecked ->
              RFConfig.update { it.copy(splitChatEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Gray,
              uncheckedColor = Color.White
            )
          )
              Text(
                text = stringResource(Res.string.settings_split_chat),
                color = Color.White
              )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
          Checkbox(
            checked = config.tabbedDetectionEnabled,
            onCheckedChange = { isChecked ->
              RFConfig.update { it.copy(tabbedDetectionEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
              Text(
                text = stringResource(Res.string.settings_tabbed_detection),
                color = Color.White
              )
        }
        // allow PvE damage option
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
          Checkbox(
            checked = config.allowPVEDamage,
            onCheckedChange = {
              isChecked ->
              RFConfig.update { it.copy(allowPVEDamage = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
              Text(
                text = stringResource(Res.string.settings_allow_pve_damage),
                color = Color.White
              )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
          Checkbox(
            checked = config.gameScheduleHotkeyEnabled,
            onCheckedChange = {
              isChecked ->
              RFConfig.update { it.copy(gameScheduleHotkeyEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Gray,
              uncheckedColor = Color.White
            )
          )
              Text(
                text = stringResource(Res.string.settings_game_schedule_hotkey),
                color = Color.White
              )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
          Checkbox(
            checked = config.useSadlyDotEyeOhhh,
            onCheckedChange = {
              isChecked ->
              RFConfig.update { it.copy(useSadlyDotEyeOhhh = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Gray,
              uncheckedColor = Color.White
            )
          )
              Text(
                text = stringResource(Res.string.settings_use_sadly),
                color = Color.White
              )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
          Checkbox(
            checked = config.dragonBreathOverlayEnabled,
            onCheckedChange = {
              isChecked ->
              RFConfig.update { it.copy(dragonBreathOverlayEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Gray,
              uncheckedColor = Color.White
            )
          )
              Text(
                text = stringResource(Res.string.settings_dragon_breath_overlay),
                color = Color.White
              )
        }

//        Row(verticalAlignment = Alignment.CenterVertically) {
//          Button(
//            onClick = {},
//            colors = ButtonDefaults.buttonColors(Color.White),
//            modifier = Modifier.padding(16.dp)
//          ) {
//            Text(
//              text = stringResource(Res.string.settings_player_remappings_button),
//              maxLines = 1,
//              color = Color.Black
//            )
//          }
//        }
      }
    }
  }
}

@Composable
fun CombatOverlaySettingsPanel(wm: WindowManager? = null) {
  val config by RFConfig.state.collectAsState()
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
    Text(
      text = stringResource(Res.string.settings_combat_overlay_title),
      color = Color.White,
      textAlign = TextAlign.Center,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
      Checkbox(
        checked = config.combatShowDamageColumn,
        onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatShowDamageColumn = isChecked) } },
        colors = CheckboxDefaults.colors(
          checkmarkColor = Color.White,
          checkedColor = Color.Red,
          uncheckedColor = Color.White
        )
      )
      Text(text = stringResource(Res.string.settings_show_damage_column), color = Color.White, modifier = Modifier.padding(start = 8.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
      Checkbox(
        checked = config.combatShowHealsColumn,
        onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatShowHealsColumn = isChecked) } },
        colors = CheckboxDefaults.colors(
          checkmarkColor = Color.White,
          checkedColor = Color.Red,
          uncheckedColor = Color.White
        )
      )
      Text(text = stringResource(Res.string.settings_show_heals_column), color = Color.White, modifier = Modifier.padding(start = 8.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
      Checkbox(
        checked = config.combatShowCCColumn,
        onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatShowCCColumn = isChecked) } },
        colors = CheckboxDefaults.colors(
          checkmarkColor = Color.White,
          checkedColor = Color.Red,
          uncheckedColor = Color.White
        )
      )
      Text(text = stringResource(Res.string.settings_show_cc_column), color = Color.White, modifier = Modifier.padding(start = 8.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
      Checkbox(
        checked = config.combatControlsFadeEnabled,
        onCheckedChange = { isChecked -> RFConfig.update { it.copy(combatControlsFadeEnabled = isChecked) } },
        colors = CheckboxDefaults.colors(
          checkmarkColor = Color.White,
          checkedColor = Color.Red,
          uncheckedColor = Color.White
        )
      )
      Text(text = stringResource(Res.string.settings_combat_fade_controls), color = Color.White, modifier = Modifier.padding(start = 8.dp))
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
