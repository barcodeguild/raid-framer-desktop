package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PetListItem
import com.reoky.raidframer.ui.component.TitleBarComponent

@Preview
@Composable
fun PokemonOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    PokemonOverlay()
  }
}

@Composable
fun PokemonOverlay(wm: WindowManager? = null) {
  val activePets = PlayerCacheInteractor.activePets.collectAsState()

  Column(
    modifier = Modifier
      .fillMaxSize()
  ) {
    // Title bar
    TitleBarComponent(
      title = "Dragon Breaths, Risos, Battle Pets and Other Creatures",
      onClose = { wm?.closeWindow(OverlayType.POKEMON) }
    )

    // Pet list
    if (activePets.value.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        Text(
          text = "No active companions detected, yet...",
          fontSize = 14.sp,
          color = Color(0xFF9CA3AF)
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        itemsIndexed(activePets.value, key = { _, card -> card.petId }) { index, card ->
          PetListItem(
            petName = card.name,
            owner = card.owner,
            damage = card.sessionDamageTotal,
            debuffs = card.recentDebuffAppliedEvents.map { it.debuff }.distinct(),
            petType = card.petType,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
  }
}

@Preview
@Composable
fun PokemonOverlayPreview() {
  Box(
    modifier = Modifier
      .width(400.dp)
      .height(600.dp)
      .background(Color.Black)
  ) {
    PokemonOverlay()
  }
}