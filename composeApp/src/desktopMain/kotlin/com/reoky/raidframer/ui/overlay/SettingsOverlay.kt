package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CloseButton
import com.reoky.raidframer.ui.dialog.FileSelectionDialog
import java.util.Locale.getDefault


@Composable
fun SettingsOverlay(wm: WindowManager? = null) {

  val scrollState = rememberScrollState()

  val showDialog = remember { mutableStateOf(false) }
  val selectedItem = remember { mutableStateOf("C:\\Users\\Fren\\Documents\\ArcheRage") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.60f))
      .verticalScroll(scrollState)
  ) {

    // close button
    CloseButton(
      onClose = { wm?.closeWindow(OverlayType.SETTINGS) },
      modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
    )

    Column {
      Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        Column {
          Text(
            text = "Raid Framer Settings",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier
              .padding(top = 12.dp)
          )
          Text(
            text = "Data Sourcing",
            color = Color.White,
            textAlign = TextAlign.Start,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .padding(top = 16.dp, bottom = 8.dp)
          )
          Text(
            text = "Select the combat.log file in your ArcheRage documents directory or another prior log. This mod works by reading the game's logs and displaying the data in real-time.",
            color = Color.White,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.W400,
            fontSize = 14.sp,
            modifier = Modifier
              .padding(top = 8.dp)
          )
          if (selectedItem.value.isNotBlank()) {
            Text(
              text = "Current Source",
              color = Color.White,
              textAlign = TextAlign.Center,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
            )
            Text(
              text = "`${selectedItem.value}`",
              color = Color.Green,
              textAlign = TextAlign.Center,
              fontSize = 12.sp,
              modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
            )
          } else {
            Text(
              text = "No file selected.",
              color = Color.Red,
              textAlign = TextAlign.Center,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
            )
          }
          Row(modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = { showDialog.value = !showDialog.value },
              colors = ButtonDefaults.buttonColors(Color.White),
              modifier = Modifier.weight(1f).padding(16.dp)
            ) {
              Text(
                text = "Select Logfile",
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

  FileSelectionDialog(showDialog, selectedItem)
}

@Composable
fun GlobalOptionsPanel() {
  Box(
    modifier = Modifier
      .padding(16.dp)
      .wrapContentSize()
  ) {
    Column {
      Text(
        text = "Global Options",
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
              text = "Enter your character name and faction below. Rf it's not correct already ${AppGlobals.APP_NAME} won't work. This information is used to deduce which players are allies and enemies over time:",
              color = Color.White
            )
            val textFieldValue = rememberSaveable { mutableStateOf(RFConfig.state.value.playerName) }
            TextField(
              value = textFieldValue.value,
              onValueChange = {
                textFieldValue.value = it.lowercase(getDefault()).capitalize()
                RFConfig.update { oldState ->
                  oldState.copy(playerName = textFieldValue.value)
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
            val factionOptions = listOf("East", "West", "Pirate")
            val selectedFaction = rememberSaveable { mutableStateOf(RFConfig.state.value.playerFaction) }

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
                      selectedFaction.value = option
                      RFConfig.update { oldState -> oldState.copy(playerFaction = option) }
                    },
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  RadioButton(
                    selected = (option == selectedFaction.value),
                    onClick = {
                      selectedFaction.value = option
                      RFConfig.update { oldState -> oldState.copy(playerFaction = option) }
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
          var autoTargetChecked: Boolean by remember { mutableStateOf(false) } // default
          Checkbox(
            checked = autoTargetChecked,
            onCheckedChange = {
              autoTargetChecked = it
//              lol.rfcloud.AppState.config.autoTargetEnabled = it
//              CoroutineScope(Dispatchers.Default).launch {
//                RFDao.saveConfig(lol.rfcloud.AppState.config)
//              }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Automatically target players when dealing direct damage.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          var allowAutoTargetSelfChecked: Boolean by remember { mutableStateOf(false) } // replace default TODO
          Checkbox(
            checked = allowAutoTargetSelfChecked,
            onCheckedChange = {
              allowAutoTargetSelfChecked = it
//              lol.rfcloud.AppState.config.allowAutoTargetSelf = it
//              CoroutineScope(Dispatchers.Default).launch {
//                RFDao.saveConfig(lol.rfcloud.AppState.config)
//              }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Allow automatically targeting self with heals.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          var tabbedDetectionChecked: Boolean by remember { mutableStateOf(false) } // replace default TODO
          Checkbox(
            checked = tabbedDetectionChecked,
            onCheckedChange = { isChecked ->
              tabbedDetectionChecked = isChecked
//              lol.rfcloud.AppState.config.tabbedDetectionEnabled = isChecked
//              CoroutineScope(Dispatchers.Default).launch {
//                RFDao.saveConfig(lol.rfcloud.AppState.config)
//              }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Enable automatic hiding of overlays while tabbed-out of the game. [EXPERIMENTAL]",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          var colorAndTextDetectionChecked: Boolean by remember { mutableStateOf(false) } // replace default TODO
          Checkbox(
            checked = colorAndTextDetectionChecked,
            onCheckedChange = {
//              colorAndTextDetectionChecked = it
//              lol.rfcloud.AppState.config.colorAndTextDetectionEnabled = it
//              CoroutineScope(Dispatchers.Default).launch {
//                RFDao.saveConfig(lol.rfcloud.AppState.config)
//              }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Hide overlays that obstruct in-game windows using color and text recognition. [EXPERIMENTAL]",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          var overlayResizingChecked: Boolean by remember { mutableStateOf(false) } // replace default TODO
          Checkbox(
            checked = overlayResizingChecked,
            onCheckedChange = {
              overlayResizingChecked = it
//              lol.rfcloud.AppState.config.overlayResizingEnabled = it
//              lol.rfcloud.AppState.isEverythingResizable.value = it
//              CoroutineScope(Dispatchers.Default).launch {
//                RFDao.saveConfig(lol.rfcloud.AppState.config)
//              }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Enable resizing of overlay windows. [EXPERIMENTAL]",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          var searchEverywhereChecked: Boolean by remember { mutableStateOf(false) } // replace default TODO
          Checkbox(
            checked = searchEverywhereChecked,
            onCheckedChange = {
              searchEverywhereChecked = it
//              lol.rfcloud.AppState.config.searchEverywhere = it
//              CombatEventInteractor.shouldSearchEverywhere = it
//              CoroutineScope(Dispatchers.Default).launch {
//                RFDao.saveConfig(lol.rfcloud.AppState.config)
//                CombatEventInteractor.locateCombatLog()
//              }
            },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Search everywhere for combat.log and not just documents.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Button(
            onClick = {
//              lol.rfcloud.AppState.isSettingsOverlayVisible.value = false
//              lol.rfcloud.AppState.isFiltersOverlayVisible.value = true
            },
            colors = ButtonDefaults.buttonColors(Color.White),
            modifier = Modifier.padding(16.dp)
          ) {
            Text(
              text = "Filters",
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