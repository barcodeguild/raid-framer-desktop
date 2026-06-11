package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.hasPvPParticipation
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.RaidComponent
import com.reoky.raidframer.ui.component.SelectableTextField
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.CheckBoxComponent
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun RaidOverlay(wm: WindowManager? = null) {
  // Collect data for both raids
  val playerFaction = Faction.fromString(RFConfig.state.collectAsState().value.playerFaction)
  val mainRaid = PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid = PlayerCacheInteractor.getRaidById(1).collectAsState()
  val nearbyNuia = PlayerCacheInteractor.nearbyNuianRaidParties.collectAsState()
  val nearbyHaranya = PlayerCacheInteractor.nearbyHaraniRaidParties.collectAsState()
  val nearbyPirate = PlayerCacheInteractor.nearbyPirateRaidParties.collectAsState()
  val raidDepartures = PlayerCacheInteractor.raidDeparturesFlow.collectAsState()

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      TitleBarComponent(
        title = "Raid Management, Attendance, and Nearby Players",
        onClose = { wm?.closeWindow(OverlayType.RAID) }
      )

      // Main Content Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(4.dp),
        contentAlignment = Alignment.TopStart
      ) {
        // Wrap content in a Column to prevent stacking on top of each other
        Column(
          modifier = Modifier
            .wrapContentWidth()
            .verticalScroll(rememberScrollState()), // Allow scrolling if list gets too long
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 8.dp),
            horizontalArrangement = Arrangement.Start
          ) {
            // if not in a raid, show a text saying "No Raid Detected"
            if (mainRaid.value.isEmpty() && coRaid.value.isEmpty()) {
              Box((Modifier.fillMaxWidth().fillMaxHeight())) {
                Text(
                  text = "Please join a raid to use raid management features. You must have the Lua Addon installed. Windows with title bars are not overlays, consider placing on another monitor to avoid covering the game UI.",
                  color = Color.LightGray,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.align(Alignment.Center)
                )
              }
              return@Column
            } else {

              // --- Main Raid Section ---
              Column(
                modifier = Modifier.width(IntrinsicSize.Min).padding(0.5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  modifier = Modifier.align(Alignment.CenterHorizontally),
                  text = "Main Raid",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                RaidComponent(
                  parties = mainRaid.value,
                  modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally)
                )
              }

              // --- Co-Raid Section ---
              Column(
                modifier = Modifier.width(IntrinsicSize.Min).padding(0.5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  modifier = Modifier.align(Alignment.CenterHorizontally),
                  text = "Co-Raid",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                RaidComponent(
                  parties = coRaid.value,
                  modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally)
                )
              }
            }
            Spacer(modifier = Modifier.weight(1f))

            var includeMain by rememberSaveable { mutableStateOf(true) }
            var includeCo by rememberSaveable { mutableStateOf(true) }
            var includeNearbySameFaction by rememberSaveable { mutableStateOf(false) }
            var includeNearbyOppositeFaction by rememberSaveable { mutableStateOf(false) }
            var requirePvPParticipation by rememberSaveable { mutableStateOf(false) }
            var includePlayersThatLeftRaid by rememberSaveable { mutableStateOf(false) }

            // Use centralized PlayerCard.hasPvPParticipation() extension
            fun String.meetsPvP(): Boolean =
              if (!requirePvPParticipation) true
              else PlayerCacheInteractor.getCard(this)?.hasPvPParticipation() ?: false

            val attendanceNames = run {
              val names = mutableListOf<String>()
              if (includeMain) {
                names += mainRaid.value.flatten().mapNotNull { it.playerName }.filter { it.meetsPvP() }
              }
              if (includeCo) {
                names += coRaid.value.flatten().mapNotNull { it.playerName }.filter { it.meetsPvP() }
              }
              if (includeNearbySameFaction) {
                val sameFactionCards: List<PlayerCard> = when (playerFaction) {
                  Faction.HARANYA -> nearbyHaranya.value
                  Faction.NUIA -> nearbyNuia.value
                  Faction.PIRATE -> nearbyPirate.value
                  else -> emptyList()
                }
                names += sameFactionCards
                  .let { if (requirePvPParticipation) it.filter { c -> c.hasPvPParticipation() } else it }
                  .map { it.name }
              }
              if (includeNearbyOppositeFaction) {
                val oppCards: List<PlayerCard> = when (playerFaction) {
                  Faction.HARANYA -> nearbyNuia.value + nearbyPirate.value
                  Faction.NUIA -> nearbyHaranya.value + nearbyPirate.value
                  Faction.PIRATE -> nearbyHaranya.value + nearbyNuia.value
                  else -> emptyList()
                }
                names += oppCards
                  .let { if (requirePvPParticipation) it.filter { c -> c.hasPvPParticipation() } else it }
                  .map { it.name }
              }
              if (includePlayersThatLeftRaid) {
                val departed: Set<String> =
                  (raidDepartures.value[0] ?: emptySet()) +
                  (raidDepartures.value[1] ?: emptySet())
                names += departed.filter { it.meetsPvP() }
              }
              names.filter { it.isNotBlank() }.distinct().joinToString(", ")
            }

            Column(
              modifier = Modifier
                .widthIn(min = 300.dp)
                .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              horizontalAlignment = Alignment.Start
            ) {
              CheckBoxComponent(
                label = "Include Main",
                initialChecked = includeMain,
                onCheckedChange = { includeMain = it },
                textColor = Color.White
              )
              CheckBoxComponent(
                label = "Include Co-Raid",
                initialChecked = includeCo,
                onCheckedChange = { includeCo = it },
                textColor = Color.White
              )
              CheckBoxComponent(
                label = "Include Nearby Same-Faction Players",
                initialChecked = includeNearbySameFaction,
                onCheckedChange = { includeNearbySameFaction = it },
                textColor = Color.White
              )
              CheckBoxComponent(
                label = "Include Nearby Opposite-Faction Players",
                initialChecked = includeNearbyOppositeFaction,
                onCheckedChange = { includeNearbyOppositeFaction = it },
                textColor = Color.White
              )
              CheckBoxComponent(
                label = "Require at least some PvP participation (25k dmg, 25k heals, or 25+ cc points to filter non-combatants)",
                initialChecked = requirePvPParticipation,
                onCheckedChange = { requirePvPParticipation = it },
                textColor = Color.White
              )
              CheckBoxComponent(
                label = "Include players that left raid",
                initialChecked = includePlayersThatLeftRaid,
                onCheckedChange = { includePlayersThatLeftRaid = it },
                textColor = Color.White
              )
              SelectableTextField(
                value = attendanceNames,
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 56.dp)
              )

              Button(
                onClick = {
                  val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                  clipboard.setContents(StringSelection(attendanceNames), null)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
              ) {
                Text(text = "Copy Attendance", color = Color.Black)
              }
            }
          }

          Divider(color = Color.LightGray, thickness = 1.dp)

          Text(
            text = "Out-of-Raid / Opposite Faction / Nearby Players",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )

          // --- Row: Nearby Players (Below Raids) ---
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Haranya
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(4.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Haranya Faction (${nearbyHaranya.value.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
              )
              RaidComponent(
                parties = nearbyHaranya.value.mapIndexed { index, card ->
                  RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole)
                }.chunked(5),
                modifier = Modifier.wrapContentSize()
              )
            }

            // Nuian
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(4.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Nuian Faction (${nearbyNuia.value.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              RaidComponent(
                parties = nearbyNuia.value.mapIndexed { index, card ->
                  RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole)
                }.chunked(5),
                modifier = Modifier.wrapContentSize()
              )
            }

            // Pirate
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(4.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Pirate Faction (${nearbyPirate.value.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              RaidComponent(
                parties = nearbyPirate.value.mapIndexed { index, card ->
                  RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole)
                }.chunked(5),
                modifier = Modifier.wrapContentSize()
              )
            }
          }
        }
      }
    }
  }
}
