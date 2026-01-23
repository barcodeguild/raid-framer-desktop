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
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.settings_data_sourcing_description
import raid_framer_desktop.composeapp.generated.resources.settings_data_sourcing_sidenote
import raid_framer_desktop.composeapp.generated.resources.settings_data_sourcing_title
import raid_framer_desktop.composeapp.generated.resources.settings_title
import java.util.Locale.getDefault


@Composable
fun SettingsOverlay(wm: WindowManager? = null) {

  val config by RFConfig.state.collectAsState() // single source of truth

  val scrollState = rememberScrollState()
  val showDialog = remember { mutableStateOf(false) }

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

          Row(modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (config.defaultArcheRageDirectory.isNotBlank()) {
              Text(
                text = "ArcheRage Documents Directory:",
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
                text = "Please Specify Directory (It's the folder that has a system.cfg file inside it!)",
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
                text = "Lua Companion Options",
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
                text = "About",
                maxLines = 1,
                color = Color.Black
              )
            }
          }
          Row(Modifier.padding(start = 8.dp, end = 8.dp)) {
            Text(
              text = stringResource(Res.string.settings_data_sourcing_sidenote),
              color = Color.White,
              textAlign = TextAlign.Start,
              fontWeight = FontWeight.W400,
              fontSize = 14.sp)
          }
        }
      }

      GlobalOptionsPanel()
    }
    // looked too weird to keep
    // VerticalScrollbar(
    //   modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
    //   adapter = rememberScrollbarAdapter(scrollState)
    // )
  }

  //FileSelectionDialog(showDialog, config.defaultArcheRageDirectory)
}

@Composable
fun GlobalOptionsPanel() {
  val config by RFConfig.state.collectAsState() // single source of truth
  Box(
    modifier = Modifier
      .padding(16.dp)
      .wrapContentSize()
  ) {
    Column {
      Text(
        text = "Your Current Character",
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
              text = "Please ensure that your character and faction are specified correctly and kept up-to-date. ${AppGlobals.APP_NAME} uses this information to deduce friendly and hostile characters over time.",
              color = Color.White
            )
            TextField(
              value = config.playerName,
              onValueChange = {
                RFConfig.update { config ->
                  val newPlayerName = it.lowercase(getDefault()).capitalize()
                  config.copy(playerName = newPlayerName)
                }
              },
              textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 20.sp
              ),
              singleLine = true,
              maxLines = 1,
              placeholder = { Text("Enter Name Here") },
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = config.miniGraphEnabled,
            onCheckedChange = { isChecked ->
              RFConfig.update { it.copy(miniGraphEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Enable DPS/HEALS/CC Mini-Graph. Shows performance over time.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = config.splitChatEnabled,
            onCheckedChange = { isChecked ->
              RFConfig.update { it.copy(splitChatEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Enable OSRS-style split-chat window overlay. [EXPERIMENTAL]",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            text = "Automatically hide game overlay's while tabbed-out of the game. Doesn't apply to tool-tips (those with a title-bar).",
            color = Color.White
          )
        }
        // allow PvE damage option
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            text = "Allow PvE Damage to be counted towards DPS/Kill Counters. (Useful for solo players who want to see their damage output against PvE targets)",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = config.gameScheduleHotkeyEnabled,
            onCheckedChange = {
              isChecked ->
              RFConfig.update { it.copy(gameScheduleHotkeyEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Enable [HOLD TAB] to bring-up the official in-game event schedule website as an overlay.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = config.useSadlyDotEyeOhhh,
            onCheckedChange = {
              isChecked ->
              RFConfig.update { it.copy(useSadlyDotEyeOhhh = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "I'd prefer to use Sadly.io for in-game schedule data.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = config.dragonBreathOverlayEnabled,
            onCheckedChange = {
              isChecked ->
              RFConfig.update { it.copy(dragonBreathOverlayEnabled = isChecked) }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Enable dragon breath detector and counter overlay. (Note: You still have be in 100 meter range of the dragon rider to witness the breaths, this isn't magic!)",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(Color.White),
            modifier = Modifier.padding(16.dp)
          ) {
            Text(
              text = "Player Re-mappings, PvP Duels & Filters",
              maxLines = 1,
              color = Color.Black
            )
          }
        }
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