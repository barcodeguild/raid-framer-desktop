package ui.overlay

import AppState
import CombatInteractor
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
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
import core.database.RFDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.dialog.FileSelectionDialog

@Composable
fun SettingsOverlay() {

  val scrollState = rememberScrollState()

  val showDialog = remember { mutableStateOf(false) }
  val selectedItem = remember { mutableStateOf(AppState.config.defaultLogPath) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.60f))
      .verticalScroll(scrollState)
  ) {
    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
      val interactionSource = remember { MutableInteractionSource() }
      val isCloseHovered by interactionSource.collectIsHoveredAsState()

      IconButton(
        onClick = {
          AppState.isSettingsOverlayVisible.value = false
        },
        modifier = Modifier
          .size(32.dp)
          .background(
            if (isCloseHovered) Color.Red.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.20f),
            MaterialTheme.shapes.small
          )
          .shadow(
            elevation = 0.dp,
            clip = true,
            ambientColor = Color.Transparent,
            spotColor = Color.Transparent
          )
          .hoverable(interactionSource = interactionSource)
          .clip(RoundedCornerShape(8.dp))
      ) {
        Text(
          "✕",
          fontSize = 18.sp,
          color = if (isCloseHovered) Color.White else Color.White,
          textAlign = TextAlign.Center
        )
      }
    }
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
            text = "Select the combat.log file for your ArcheRage installation to use for data-sourcing. This mod reads the game's logs and displays the data in real-time.",
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
                AppState.isSettingsOverlayVisible.value = false
                AppState.isAboutOverlayVisible.value = true
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
              text = "Enter your character name below to enable the auto-targeting feature. It needs to match exactly what is displayed in-game:",
              color = Color.White
            )
            val textFieldValue = rememberSaveable { mutableStateOf(AppState.config.playerName) }
            TextField(
              value = textFieldValue.value,
              onValueChange = {
                textFieldValue.value = it.toLowerCase().capitalize()
                AppState.config.playerName = it.toLowerCase().capitalize()
                CoroutineScope(Dispatchers.Default).launch {
                  RFDao.saveConfig(AppState.config)
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
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          var autoTargetChecked: Boolean by remember { mutableStateOf(AppState.config.autoTargetEnabled) }
          Checkbox(
            checked = autoTargetChecked,
            onCheckedChange = {
              autoTargetChecked = it
              AppState.config.autoTargetEnabled = it
              CoroutineScope(Dispatchers.Default).launch {
                RFDao.saveConfig(AppState.config)
              }
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
          var allowAutoTargetSelfChecked: Boolean by remember { mutableStateOf(AppState.config.allowAutoTargetSelf) }
          Checkbox(
            checked = allowAutoTargetSelfChecked,
            onCheckedChange = {
              allowAutoTargetSelfChecked = it
              AppState.config.allowAutoTargetSelf = it
              CoroutineScope(Dispatchers.Default).launch {
                RFDao.saveConfig(AppState.config)
              }
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
          var tabbedDetectionChecked: Boolean by remember { mutableStateOf(AppState.config.tabbedDetectionEnabled) }
          Checkbox(
            checked = tabbedDetectionChecked,
            onCheckedChange = { isChecked ->
              tabbedDetectionChecked = isChecked
              AppState.config.tabbedDetectionEnabled = isChecked
              CoroutineScope(Dispatchers.Default).launch {
                RFDao.saveConfig(AppState.config)
              }
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
          var colorAndTextDetectionChecked: Boolean by remember { mutableStateOf(AppState.config.colorAndTextDetectionEnabled) }
          Checkbox(
            checked = colorAndTextDetectionChecked,
            onCheckedChange = {
              colorAndTextDetectionChecked = it
              AppState.config.colorAndTextDetectionEnabled = it
              CoroutineScope(Dispatchers.Default).launch {
                RFDao.saveConfig(AppState.config)
              }
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
          var overlayResizingChecked: Boolean by remember { mutableStateOf(AppState.config.overlayResizingEnabled) }
          Checkbox(
            checked = overlayResizingChecked,
            onCheckedChange = {
              overlayResizingChecked = it
              AppState.config.overlayResizingEnabled = it
              AppState.isEverythingResizable.value = it
              CoroutineScope(Dispatchers.Default).launch {
                RFDao.saveConfig(AppState.config)
              }
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
          var searchEverywhereChecked: Boolean by remember { mutableStateOf(AppState.config.searchEverywhere) }
          Checkbox(
            checked = searchEverywhereChecked,
            onCheckedChange = {
              searchEverywhereChecked = it
              AppState.config.searchEverywhere = it
              CombatInteractor.shouldSearchEverywhere = it
              CoroutineScope(Dispatchers.Default).launch {
                RFDao.saveConfig(AppState.config)
                CombatInteractor.locateCombatLog()
              }
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
              AppState.isSettingsOverlayVisible.value = false
              AppState.isFiltersOverlayVisible.value = true
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