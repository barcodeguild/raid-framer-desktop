package ui.overlay

import AppState
import CombatEventInteractor
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.dialog.FileSelectionDialog

@Composable
fun SettingsOverlay() {

  val scrollState = rememberScrollState()

  val showDialog = mutableStateOf(false)
  val selectedItem = remember { mutableStateOf("") }
  var checked: Boolean by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.60f))
      .verticalScroll(scrollState)
  ) {
    Box(modifier = Modifier
      .align(Alignment.TopEnd)
      .background(Color.Red.copy(alpha = 0.60f)
      )
    ) {
      val interactionSource = remember { MutableInteractionSource() }
      val isHovered by interactionSource.collectIsHoveredAsState()

      IconButton(
        onClick = {
          AppState.isSettingsOverlayVisible.value = false
        },
        modifier = Modifier
          .size(38.dp)
          .background(Color.Transparent, MaterialTheme.shapes.small)
          .shadow(
            elevation = 0.dp,
            clip = true,
            ambientColor = Color.Transparent,
            spotColor = Color.Transparent
          )
          .hoverable(interactionSource = interactionSource)
      ) {
        Text("✕", fontSize = 18.sp, color = if (isHovered) Color.Black else Color.White, textAlign = TextAlign.Center)
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
            fontSize = 28.sp,
            modifier = Modifier
              .padding(top = 16.dp)
          )
          Text(
            text = "Data Sourcing",
            color = Color.White,
            textAlign = TextAlign.Start,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .padding(top = 16.dp, bottom = 8.dp)
          )
          Text(
            text = "You should select the combat log file for your ArcheRage installation to use for data sourcing. This mod works by reading from the game's combat.log file and displaying the data in real-time.",
            color = Color.White,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.W400,
            fontSize = 14.sp,
            modifier = Modifier
              .padding(top = 8.dp))
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
          Button(
            onClick = { showDialog.value = !showDialog.value },
            colors = ButtonDefaults.buttonColors(Color.White),
            modifier = Modifier.padding(top = 16.dp)
          ) {
            Text(
              text = "Select Logfile",
              color = Color.Black
            )
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

  FileSelectionDialog(CombatEventInteractor.possiblePaths, showDialog, selectedItem)
  LaunchedEffect(selectedItem.value) {
    CombatEventInteractor.selectedPath = selectedItem.value
    CombatEventInteractor.start()
  }
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
        fontSize = 20.sp,
        modifier = Modifier
          .padding(top = 8.dp, bottom = 8.dp)
      )
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = AppState.isAggroOverlayVisible.value,
            onCheckedChange = { newValue -> AppState.isAggroOverlayVisible.value = newValue },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Show full-height high-scores overlay.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = AppState.isAggroOverlayVisible.value,
            onCheckedChange = { newValue -> AppState.isAggroOverlayVisible.value = newValue },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Show super-tracker overlay.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = AppState.isAggroOverlayVisible.value,
            onCheckedChange = { newValue -> AppState.isAggroOverlayVisible.value = newValue },
            colors = CheckboxDefaults.colors(
              checkmarkColor = Color.White,
              checkedColor = Color.Red,
              uncheckedColor = Color.White
            )
          )
          Text(
            text = "Show `high-scores` overlay.",
            color = Color.White
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = AppState.isAggroOverlayVisible.value,
            onCheckedChange = { newValue -> AppState.isAggroOverlayVisible.value = newValue },
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
          Checkbox(
            checked = AppState.isEverythingResizable.value,
            onCheckedChange = { newValue -> AppState.isEverythingResizable.value = newValue },
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