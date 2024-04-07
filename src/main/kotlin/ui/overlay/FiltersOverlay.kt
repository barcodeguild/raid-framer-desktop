package ui.overlay

import AppState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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

@Preview
@Composable
fun PreviewFiltersOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    FiltersOverlay()
  }
}

@Composable
fun FiltersOverlay() {
  var newFilter by remember { mutableStateOf("") }
  val filters = remember { mutableStateListOf<String>() }

  fun addFilter(filter: String) {
    if (filter.isBlank()) return
    if (filters.contains(filter)) return
    filters.add(filter)
  }

  Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f))) {

    // close button
    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
      val interactionSource = remember { MutableInteractionSource() }
      val isCloseHovered by interactionSource.collectIsHoveredAsState()
      IconButton(
        onClick = {
          AppState.isFiltersOverlayVisible.value = false
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

    Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
      Column {
        Text(
          text = "Subscriptions",
          fontSize = 24.sp,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(8.dp)
        )
        Text(
          text = "Subscriptions allow you to 'subscribe' to custom character name re-mappings and omissions as curated by your own guild or faction.\n\nEnter the URL of a .CSV file on the web containing custom filters. These will be combined / merged with your personal filters, such that personal filters always take precedence over remote filters. You can use this functionality to remove or re-map ambiguous names like 'Ironclad' that may be of no interest. You can make these in any common spreadsheet software. In the first column put the name, and the second column put the replacement name. The word 'REMOVE' (in all CAPS) will filter a name entirely. Lists are updated automatically based on the URL you enter below periodically:",
          fontSize = 14.sp,
          color = Color.White,
          fontWeight = FontWeight.Light,
          textAlign = TextAlign.Start,
          modifier = Modifier.padding(8.dp)
        )
        val textFieldValue = rememberSaveable { mutableStateOf(AppState.config.playerName) }
        TextField(
          value = textFieldValue.value,
          onValueChange = {
            textFieldValue.value = it
          },
          placeholder = { Text("Paste a URL here") },
          textStyle = TextStyle(
            textAlign = TextAlign.Center,
            fontSize = 20.sp
          ),
          singleLine = true,
          maxLines = 1,
          colors = TextFieldDefaults.textFieldColors(
            textColor = Color.White,
            backgroundColor = Color.Transparent,
            focusedIndicatorColor = Color.Red,
            unfocusedIndicatorColor = Color.White,
            placeholderColor = Color.LightGray,
            cursorColor = Color.Red
          ),
          modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        )
      }

      // Remote Filters

      // Personal Filters
      Column {
        Text(
          text = "Name Filters",
          fontSize = 24.sp,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(8.dp)
        )

        LazyColumn {
          items(filters.count()) { filter ->
            Row {
              Text(
                text = "$filter: ${filters[filter]}",
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
              )
            }
          }
        }
        Spacer(Modifier.weight(1f))
        TextField(
          value = newFilter,
          onValueChange = { newFilter = it },
          maxLines = 1,
          placeholder = { Text("type a name here") },
          modifier = Modifier.fillMaxWidth(),
          colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.White.copy(alpha = 0.20f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Color.White,
            textColor = Color.White
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              addFilter(newFilter)
            }
          )
        )
        Button(
          onClick = {
            addFilter(newFilter)
          },
          modifier = Modifier.padding(8.dp)
        ) {
          Text("Add to Filters")
        }
      }
    }

  }


}