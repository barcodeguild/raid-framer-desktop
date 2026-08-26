package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors

const val SESSION_TYPE_DONT_CARE = "Don't Care"

val SESSION_TYPES = listOf(
  SESSION_TYPE_DONT_CARE,
  "Abyssal Attack / Luscas",
  "Aegis Island",
  "Akasch Invasion",
  "Black Dragon",
  "CTF Arena",
  "Charbydis",
  "Cinder War",
  "Crimson Rift",
  "Dimensional Boundary",
  "Delphinad Ghost Ship (DGS)",
  "Drill Camp Arena",
  "Farming",
  "Freedich",
  "Free-for-All Arena",
  "Garden Anthalon",
  "Garden",
  "GM Dragon",
  "Gladiator Arena",
  "Glenn",
  "Grimghast Rift",
  "Halcy (Golden Plains Battle)",
  "Hasla Zombie",
  "Hiram Rift",
  "Housing Claim",
  "Instanced Dungeon Runs",
  "Jola, Meina, Glenn (JMG)",
  "Kraken",
  "Land Packs",
  "Malestorm Arena",
  "Meina",
  "Noryette Challenge",
  "Ocean Packs",
  "Player Duels",
  "Rangora",
  "Reset Raid",
  "Scramble",
  "Scrims",
  "Siege Calmlands",
  "Siege Heedmar",
  "Siege Marcala",
  "Siege Nuimari",
  "Sparring Arena",
  "Sungold CR",
  "Thunderwing Titan",
  "Titan Event",
  "Violent",
  "Whalesong",
  "World Bosses",
  "Yny War",
  "Custom"
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SessionTypeDropdown(
  selectedType: String,
  onTypeSelected: (String) -> Unit,
  onExpandedChange: ((Boolean) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var highlightedIndex by remember { mutableStateOf(0) }
  val focusRequester = remember { FocusRequester() }
  val normalizedQuery = searchQuery.trim()
  val filteredTypes = SESSION_TYPES
    .filter { type ->
      normalizedQuery.isBlank() || type.contains(normalizedQuery, ignoreCase = true)
    }
    .sortedWith(
      compareBy { type ->
        !type.startsWith(normalizedQuery, ignoreCase = true)
      }
    )

  LaunchedEffect(expanded) {
    onExpandedChange?.invoke(expanded)

    if (expanded) {
      searchQuery = ""
      highlightedIndex = 0
      focusRequester.requestFocus()
    }
  }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier
  ) {
    TextField(
      value = if (expanded) searchQuery else selectedType,
      onValueChange = {
        searchQuery = it
        highlightedIndex = 0
      },
      readOnly = !expanded,
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .onPreviewKeyEvent { event ->
          if (event.type != KeyEventType.KeyDown) {
            false
          } else {
            when (event.key) {
              Key.DirectionDown -> {
                if (filteredTypes.isNotEmpty()) {
                  highlightedIndex = (highlightedIndex + 1) % filteredTypes.size
                }
                true
              }

              Key.DirectionUp -> {
                if (filteredTypes.isNotEmpty()) {
                  highlightedIndex =
                    (highlightedIndex - 1 + filteredTypes.size) % filteredTypes.size
                }
                true
              }

              Key.Enter -> {
                filteredTypes.getOrNull(highlightedIndex)?.let { type ->
                  onTypeSelected(type)
                  expanded = false
                }
                true
              }

              else -> false
            }
          }
        },
      colors = TextFieldDefaults.textFieldColors(
        textColor = RFColors.TextPrimary,
        backgroundColor = Color(0xFF1E1E1E),
        focusedIndicatorColor = RFColors.AccentRed,
        unfocusedIndicatorColor = RFColors.CardBorder,
        cursorColor = RFColors.AccentRed
      ),
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      textStyle = TextStyle(fontSize = 13.sp),
      maxLines = 1
    )

    MaterialTheme(
      colors = MaterialTheme.colors.copy(surface = RFColors.CardBackground)
    ) {
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp))
      ) {
        filteredTypes.forEach { type ->
          DropdownMenuItem(
            onClick = {
              onTypeSelected(type)
              expanded = false
            },
            modifier = Modifier.background(
              if (filteredTypes.indexOf(type) == highlightedIndex) {
                RFColors.AccentRed.copy(alpha = 0.15f)
              } else {
                Color.Transparent
              }
            ),
            content = {
              Text(
                text = type,
                color = if (type == selectedType) RFColors.AccentRed else RFColors.TextPrimary,
                fontWeight = if (type == selectedType) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
          )
        }
      }
    }
  }
}
