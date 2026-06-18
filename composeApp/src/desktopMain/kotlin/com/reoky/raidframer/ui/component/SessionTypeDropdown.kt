package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors

val SESSION_TYPES = listOf(
  "Abyssal Attack / Luscas",
  "Aegis Island",
  "Black Dragon",
  "CTF Arena",
  "Charbydis",
  "Cinder War",
  "Crimson Rift",
  "Dimensional Boundary",
  "Delphinad Ghost Ship (DGS)",
  "Drill Camp Arena",
  "Farming",
  "Free-for-All Arena",
  "Garden Anthalon",
  "Garden",
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
  "Scrims",
  "Sparring Arena",
  "Sungold CR",
  "Thunderwing Titan",
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

  LaunchedEffect(expanded) {
    onExpandedChange?.invoke(expanded)
  }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier
  ) {
    TextField(
      value = selectedType,
      onValueChange = {},
      readOnly = true,
      modifier = Modifier
        .fillMaxWidth(),
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

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier
        .fillMaxWidth()
        .background(RFColors.CardBackground, RoundedCornerShape(8.dp))
        .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp))
    ) {
      SESSION_TYPES.forEach { type ->
        DropdownMenuItem(
          onClick = {
            onTypeSelected(type)
            expanded = false
          },
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
