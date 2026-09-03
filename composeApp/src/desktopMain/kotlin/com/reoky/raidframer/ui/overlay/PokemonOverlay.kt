package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PetCard
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PetListItem
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.titleBarCaptureActions
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.pokemon_no_companions
import raid_framer_desktop.composeapp.generated.resources.pokemon_title

@Preview
@Composable
fun PokemonOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    PokemonOverlay(wm = null)
  }
}

@Composable
fun PokemonOverlay(wm: WindowManager? = null) {
  val activePets = PlayerCacheInteractor.activePets.collectAsState()
  val config = RFConfig.state.collectAsState()
  val sortedPets = remember(activePets.value) {
    activePets.value.sortedWith(
      compareByDescending<PetCard> {
        it.sessionBreathCasts.isNotEmpty() || it.sessionRocketCasts.isNotEmpty()
      }.thenByDescending { it.sessionDamageTotal }
    )
  }
  val ownFaction = remember(config.value.playerFaction) {
    Faction.fromString(config.value.playerFaction)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
  ) {
    // Title bar
    TitleBarComponent(
      title = stringResource(Res.string.pokemon_title),
      onClose = { wm?.closeWindow(OverlayType.POKEMON) },
      captureActions = wm?.nativeWindow(OverlayType.POKEMON)?.let { window ->
        titleBarCaptureActions(window as androidx.compose.ui.awt.ComposeWindow, wm, "Dragon Breaths")
      }
    )

    // Pet list
    if (activePets.value.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        Text(
          text = stringResource(Res.string.pokemon_no_companions),
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
        itemsIndexed(sortedPets, key = { _, card -> card.petId }) { index, card ->
          // Look up owner's faction for badge coloring
          val ownerCard = remember(card.owner) { PlayerCacheInteractor.getCard(card.owner) }
          val ownerFaction = remember(ownerCard) {
            ownerCard?.lastKnownFaction?.let { Faction.fromString(it) } ?: Faction.UNKNOWN
          }
          val ownerFactionColor = remember(ownFaction, ownerFaction) {
            ownFaction.getFactionHighlightColor(ownerFaction)
          }

          PetListItem(
            petName = card.name,
            owner = card.owner,
            damage = card.sessionDamageTotal,
            debuffs = card.recentDebuffAppliedEvents.map { it.debuff }.distinct(),
            petTypes = card.petTypes,
            breathCasts = card.sessionBreathCasts,
            rocketCasts = card.sessionRocketCasts,
            ownerFactionColor = ownerFactionColor,
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
    PokemonOverlay(wm = null)
  }
}
