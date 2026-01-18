package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
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
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CheckBoxComponent
import com.reoky.raidframer.ui.component.RaidComponent
import com.reoky.raidframer.ui.component.SelectableTextField
import com.reoky.raidframer.ui.component.TitleBarComponent
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.text.chunked

@Composable
fun RaidOverlay(wm: WindowManager? = null) {
  // Collect data for both raids
  val mainRaid = PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid = PlayerCacheInteractor.getRaidById(1).collectAsState()
  val nearbyNuia = PlayerCacheInteractor.nearbyNuianPlayers.collectAsState()
  val nearbyHaranya = PlayerCacheInteractor.nearbyHaraniPlayers.collectAsState()
  val nearbyPirate = PlayerCacheInteractor.nearbyPiratePlayers.collectAsState()

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
              Column((Modifier.fillMaxWidth())) {
                Text(
                  modifier = Modifier.align(Alignment.CenterHorizontally),
                  text = "< Please join a raid to see raid frames here >",
                  color = Color.LightGray,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
              }
              return@Column
            } else {

              // --- Main Raid Section ---
              Column(
                modifier = Modifier.width(IntrinsicSize.Min),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                modifier = Modifier.width(IntrinsicSize.Min),
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
            var includeNearby by rememberSaveable { mutableStateOf(false) }

            val attendanceNames = run {
              val names = mutableListOf<String>()
              if (includeMain) names += mainRaid.value.flatten().mapNotNull { it.playerName }
              if (includeCo) names += coRaid.value.flatten().mapNotNull { it.playerName }
              if (includeNearby) {
                names += nearbyHaranya.value.mapNotNull { it.name }
                names += nearbyNuia.value.mapNotNull { it.name }
                names += nearbyPirate.value.mapNotNull { it.name }
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
              Text(
                text = "Attendance",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeMain, onCheckedChange = { includeMain = it })
                Text(
                  text = "Include Main",
                  color = Color.White,
                  modifier = Modifier.padding(start = 8.dp)
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeCo, onCheckedChange = { includeCo = it })
                Text(
                  text = "Include Co",
                  color = Color.White,
                  modifier = Modifier.padding(start = 8.dp)
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeNearby, onCheckedChange = { includeNearby = it })
                Text(
                  text = "Include Nearby",
                  color = Color.White,
                  modifier = Modifier.padding(start = 8.dp)
                )
              }

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
            text = "Unknown Raid / No Raid / Casting Nearby",
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
                text = "Haranya Faction",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
              )
              RaidComponent(
                parties = nearbyHaranya.value.mapIndexed { index, card ->
                  RaidFramePayload(slot = index, playerName = card.name, role = 1)
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
                text = "Nuian Faction",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              RaidComponent(
                parties = nearbyNuia.value.mapIndexed { index, card ->
                  RaidFramePayload(slot = index, playerName = card.name)
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
                text = "Pirate Faction",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              RaidComponent(
                parties = nearbyPirate.value.mapIndexed { index, card ->
                  RaidFramePayload(slot = index, playerName = card.name)
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
