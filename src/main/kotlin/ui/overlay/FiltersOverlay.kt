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
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.helpers.openWebLink

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

  val filters = remember { mutableStateListOf<Map<String, String>>() }

  fun setFilter(filter: Pair<String, String>) {
    if (filter.first.isBlank() || filter.second.isBlank()) return
    if (filters.any { it.containsKey(filter.first) }) {
      filters[filters.indexOfFirst { it.containsKey(filter.first) }] = mapOf(filter.first to filter.second)
    } else {
      filters.add(mapOf(filter.first to filter.second))
    }
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
          text = "Subscriptions allow you to create custom character name re-mappings and omissions from lists curated by your own guild or faction. You can use this functionality to remove or re-map ambiguous names like 'Ironclad' that may be of no interest. Lists are updated automatically based on the URL you enter below:",
          fontSize = 14.sp,
          color = Color.White,
          fontWeight = FontWeight.Light,
          textAlign = TextAlign.Start,
          modifier = Modifier.padding(8.dp)
        )
        val selectedFilterURL = rememberSaveable { mutableStateOf("~Paste Link Here~") }
        TextField(
          value = selectedFilterURL.value,
          onValueChange = {
            selectedFilterURL.value = it
          },
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
          modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally).padding(8.dp, 0.dp)
        )

//        Text(
//          text = "• Filters from the web are combined with any local filters. Local filters always take precedence over remote ones.",
//          fontSize = 14.sp,
//          color = Color.White,
//          fontWeight = FontWeight.Light,
//          textAlign = TextAlign.Start,
//          modifier = Modifier.padding(8.dp)
//        )
//        Text(
//          text = "• You can make CSV filters in any common spreadsheet software. In the first column put the name, and the second column put the replacement name. The word 'REMOVE' (in all CAPS) will filter that player/npc entirely.",
//          fontSize = 14.sp,
//          color = Color.White,
//          fontWeight = FontWeight.Light,
//          textAlign = TextAlign.Start,
//          modifier = Modifier.padding(8.dp)
//        )
//        Text(
//          text = "Enter the URL of a .CSV file on the web containing custom filters:",
//          fontSize = 14.sp,
//          color = Color.White,
//          fontWeight = FontWeight.Light,
//          textAlign = TextAlign.Start,
//          modifier = Modifier.padding(8.dp)
//        )


        // button to see an example
        Row {
          Button(
            onClick = {
              openWebLink("https://www.raidframer.lol/filters.csv")
            },
            colors = ButtonDefaults.buttonColors(Color.White),
            modifier = Modifier.padding(16.dp)
          ) {
            Text(
              text = "See Example CSV",
              maxLines = 1,
              color = Color.Black
            )
          }
          Button(
            onClick = {
              selectedFilterURL.value = "https://www.raidframer.lol/filters.csv"
            },
            colors = ButtonDefaults.buttonColors(Color.White),
            modifier = Modifier.padding(16.dp)
          ) {
            Text(
              text = "Use Default",
              maxLines = 1,
              color = Color.Black
            )
          }
        }

        // Current Filters
        Text(
          text = "Current Filters",
          fontSize = 24.sp,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(8.dp)
        )

        LazyColumn {
          items(filters.count()) { index ->
            Row {
              val hoveredItem = remember { MutableInteractionSource() }
              IconButton(
                onClick = { },
                modifier = Modifier
                  .size(32.dp)
                  .padding(top = 4.dp)
                  .background(Color.Transparent, MaterialTheme.shapes.small)
                  .shadow(
                    elevation = 0.dp,
                    clip = true,
                    ambientColor = Color.Transparent,
                    spotColor = Color.Transparent
                  )
              ) {
                Text(
                  text = "✕",
                  fontSize = 18.sp,
                  color = if (hoveredItem.collectIsHoveredAsState().value) Color.Red else Color.White,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.hoverable(interactionSource = hoveredItem)
                )
              }
              Text(
                text = "${filters[index].keys.first()} ${if (filters[index].values.first() == "REMOVED") "will be removed." else "will become " + filters[index].values.first()}.",
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


        // Personal Filters
        Text(
          text = "Overrides",
          fontSize = 24.sp,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(8.dp)
        )
        Text(
          text = "Filters from the web are combined with any local filters. Local filters always take precedence over remote ones. Use 'REMOVE' for the replacement to remove the player or NPC entirely.",
          fontSize = 14.sp,
          color = Color.White,
          fontWeight = FontWeight.Light,
          textAlign = TextAlign.Start,
          modifier = Modifier.padding(8.dp)
        )
        var original by remember { mutableStateOf("") }
        var replacement by remember { mutableStateOf("") }
        Row {
          TextField(
            value = original,
            onValueChange = { original = it.lowercase().capitalize() },
            placeholder = { Text("Original") },
            modifier = Modifier.weight(1f).padding(8.dp, 0.dp),
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
          )
          TextField(
            value = replacement,
            onValueChange = { replacement = it.capitalize() },
            placeholder = { Text("Replacement") },
            modifier = Modifier.weight(1f).padding(8.dp, 0.dp),
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
            keyboardActions = KeyboardActions(
              onDone = {
                setFilter(original to replacement)
              }
            )
          )
        }
        Button(
          onClick = {
            setFilter(original to replacement)
          },
          colors = ButtonDefaults.buttonColors(Color.White),
          modifier = Modifier.padding(16.dp)
        ) {
          Text("Add to Filters")
        }
      }
    }
  }

}