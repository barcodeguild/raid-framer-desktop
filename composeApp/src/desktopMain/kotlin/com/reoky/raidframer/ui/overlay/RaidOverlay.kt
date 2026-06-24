package com.reoky.raidframer.ui.overlay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.interactor.CompanionInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.hasPvPParticipation
import com.reoky.raidframer.core.serialization.IPCMessagePayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CheckBoxComponent
import com.reoky.raidframer.ui.component.GearScoreHistogram
import com.reoky.raidframer.ui.component.RaidComponent
import com.reoky.raidframer.ui.component.SelectableTextField
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.nearby_avg_gs_format
import raid_framer_desktop.composeapp.generated.resources.raid_attendance_title
import raid_framer_desktop.composeapp.generated.resources.raid_close
import raid_framer_desktop.composeapp.generated.resources.raid_copy_attendance
import raid_framer_desktop.composeapp.generated.resources.raid_coraid_label
import raid_framer_desktop.composeapp.generated.resources.raid_haranya_faction
import raid_framer_desktop.composeapp.generated.resources.raid_include_coraid
import raid_framer_desktop.composeapp.generated.resources.raid_include_departed
import raid_framer_desktop.composeapp.generated.resources.raid_include_main_raid
import raid_framer_desktop.composeapp.generated.resources.raid_include_nearby_opposite_faction
import raid_framer_desktop.composeapp.generated.resources.raid_include_nearby_same_faction
import raid_framer_desktop.composeapp.generated.resources.raid_main_raid_label
import raid_framer_desktop.composeapp.generated.resources.raid_nearby_disclaimer
import raid_framer_desktop.composeapp.generated.resources.raid_nearby_require_pvp
import raid_framer_desktop.composeapp.generated.resources.raid_no_raid_detected
import raid_framer_desktop.composeapp.generated.resources.raid_nuian_faction
import raid_framer_desktop.composeapp.generated.resources.raid_pirate_faction
import raid_framer_desktop.composeapp.generated.resources.raid_require_pvp_filter
import raid_framer_desktop.composeapp.generated.resources.raid_tab_attendance
import raid_framer_desktop.composeapp.generated.resources.raid_tab_nearby
import raid_framer_desktop.composeapp.generated.resources.raid_tab_nearby_gear
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
private enum class RaidTab { ATTENDANCE, NEARBY, NEARBY_GEAR }
@Composable
fun RaidOverlay(wm: WindowManager? = null) {
  val playerFaction = Faction.fromString(RFConfig.state.collectAsState().value.playerFaction)
  val mainRaid = PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid = PlayerCacheInteractor.getRaidById(1).collectAsState()
  val nearbyNuia = PlayerCacheInteractor.nearbyNuianRaidParties.collectAsState()
  val nearbyHaranya = PlayerCacheInteractor.nearbyHaraniRaidParties.collectAsState()
  val nearbyPirate = PlayerCacheInteractor.nearbyPirateRaidParties.collectAsState()
  val raidDepartures = PlayerCacheInteractor.raidDeparturesFlow.collectAsState()
  var selectedTab by remember { mutableStateOf(RaidTab.ATTENDANCE) }
  var requirePvPParticipation by rememberSaveable { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxSize()) {
    if (mainRaid.value.isEmpty() && coRaid.value.isEmpty()) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(Res.string.raid_no_raid_detected),
          color = Color.LightGray,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              CoroutineScope(Dispatchers.Main).launch {
                CompanionInteractor.sendMessage(IPCMessagePayload.TestPing())
              }
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
          ) {
            Text("Refresh", color = Color.Black)
          }
          Button(
            onClick = { wm?.closeWindow(OverlayType.RAID) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
          ) {
            Text(text = stringResource(Res.string.raid_close), color = Color.Black)
          }
        }
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        TitleBarComponent(
          title = stringResource(Res.string.raid_attendance_title),
          onClose = { wm?.closeWindow(OverlayType.RAID) }
        )
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // RaidHeaderStrip() I might want something like this in the future
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF141414).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
              .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            TabRow(
              selectedTabIndex = selectedTab.ordinal,
              backgroundColor = Color.Transparent,
              contentColor = Color.White,
              divider = {},
              indicator = {},
              modifier = Modifier.fillMaxWidth()
            ) {
              val tabs = listOf(
                RaidTab.ATTENDANCE to Res.string.raid_tab_attendance,
                RaidTab.NEARBY to Res.string.raid_tab_nearby,
                RaidTab.NEARBY_GEAR to Res.string.raid_tab_nearby_gear
              )
              tabs.forEach { (tab, label) ->
                Tab(
                  selected = selectedTab == tab,
                  onClick = { selectedTab = tab },
                  text = {
                    Text(
                      text = stringResource(label),
                      color = if (selectedTab == tab) Color.White else Color.LightGray
                    )
                  }
                )
              }
            }
          }
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.TopStart
        ) {
          when (selectedTab) {
            RaidTab.ATTENDANCE -> AttendanceTab(
              mainRaid = mainRaid.value,
              coRaid = coRaid.value,
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              playerFaction = playerFaction,
              raidDepartures = raidDepartures.value,
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
            )
            RaidTab.NEARBY -> NearbyTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
            )
            RaidTab.NEARBY_GEAR -> NearbyGearTab(
              nearbyNuia = nearbyNuia.value,
              nearbyHaranya = nearbyHaranya.value,
              nearbyPirate = nearbyPirate.value,
              requirePvPParticipation = requirePvPParticipation,
              onRequirePvPParticipationChange = { requirePvPParticipation = it }
            )
          }
        }
      }
    }
  }
}
@Composable
private fun RaidHeaderStrip() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF161616).copy(alpha = 0.80f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = "Raid Control Deck",
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp
    )
    Text(
      text = "Raid roster on the left, controls on the right, with nearby intel tucked neatly underneath.",
      color = Color.LightGray,
      fontSize = 12.sp
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      RaidHeaderChip("Roster left")
      RaidHeaderChip("Controls right")
      RaidHeaderChip("Tabs below")
    }
  }
}
@Composable
private fun RaidHeaderChip(label: String) {
  Box(
    modifier = Modifier
      .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}
@Composable
private fun AttendanceTab(
  mainRaid: List<List<RaidFramePayload>>,
  coRaid: List<List<RaidFramePayload>>,
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  playerFaction: Faction,
  raidDepartures: Map<Int, Set<String>>,
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()
  var includeMain by rememberSaveable { mutableStateOf(true) }
  var includeCo by rememberSaveable { mutableStateOf(true) }
  var includeNearbySameFaction by rememberSaveable { mutableStateOf(false) }
  var includeNearbyOppositeFaction by rememberSaveable { mutableStateOf(false) }
  var includePlayersThatLeftRaid by rememberSaveable { mutableStateOf(false) }
  fun String.meetsPvP(): Boolean =
    if (!requirePvPParticipation) true
    else PlayerCacheInteractor.getCard(this)?.hasPvPParticipation() ?: false
  val attendanceNames = run {
    val names = mutableListOf<String>()
    if (includeMain) {
      names += mainRaid.flatten().mapNotNull { frame -> frame.playerName.takeIf { it.isNotBlank() } }.filter { it.meetsPvP() }
    }
    if (includeCo) {
      names += coRaid.flatten().mapNotNull { frame -> frame.playerName.takeIf { it.isNotBlank() } }.filter { it.meetsPvP() }
    }
    if (includeNearbySameFaction) {
      val sameFactionCards: List<PlayerCard> = when (playerFaction) {
        Faction.HARANYA -> nearbyHaranya
        Faction.NUIA -> nearbyNuia
        Faction.PIRATE -> nearbyPirate
        else -> emptyList()
      }
      names += sameFactionCards
        .let { if (requirePvPParticipation) it.filter { card -> card.hasPvPParticipation() } else it }
        .map { it.name }
    }
    if (includeNearbyOppositeFaction) {
      val oppCards: List<PlayerCard> = when (playerFaction) {
        Faction.HARANYA -> nearbyNuia + nearbyPirate
        Faction.NUIA -> nearbyHaranya + nearbyPirate
        Faction.PIRATE -> nearbyHaranya + nearbyNuia
        else -> emptyList()
      }
      names += oppCards
        .let { if (requirePvPParticipation) it.filter { card -> card.hasPvPParticipation() } else it }
        .map { it.name }
    }
    if (includePlayersThatLeftRaid) {
      val departed: Set<String> =
        (raidDepartures[0] ?: emptySet()) +
          (raidDepartures[1] ?: emptySet())
      names += departed.filter { it.meetsPvP() }
    }
    names.filter { it.isNotBlank() }.distinct().joinToString(", ")
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val wideEnoughForTwoColumns = maxWidth >= 760.dp
      if (wideEnoughForTwoColumns) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top
        ) {
          AttendanceRaidPane(
            mainRaid = mainRaid,
            coRaid = coRaid,
            modifier = Modifier.weight(1f)
          )
          AttendanceControlsPane(
            includeMain = includeMain,
            onIncludeMainChange = { includeMain = it },
            includeCo = includeCo,
            onIncludeCoChange = { includeCo = it },
            includeNearbySameFaction = includeNearbySameFaction,
            onIncludeNearbySameFactionChange = { includeNearbySameFaction = it },
            includeNearbyOppositeFaction = includeNearbyOppositeFaction,
            onIncludeNearbyOppositeFactionChange = { includeNearbyOppositeFaction = it },
            requirePvPParticipation = requirePvPParticipation,
            onRequirePvPParticipationChange = onRequirePvPParticipationChange,
            includePlayersThatLeftRaid = includePlayersThatLeftRaid,
            onIncludePlayersThatLeftRaidChange = { includePlayersThatLeftRaid = it },
            attendanceNames = attendanceNames,
            modifier = Modifier.widthIn(min = 320.dp, max = 390.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          AttendanceRaidPane(
            mainRaid = mainRaid,
            coRaid = coRaid,
            modifier = Modifier.fillMaxWidth()
          )
          AttendanceControlsPane(
            includeMain = includeMain,
            onIncludeMainChange = { includeMain = it },
            includeCo = includeCo,
            onIncludeCoChange = { includeCo = it },
            includeNearbySameFaction = includeNearbySameFaction,
            onIncludeNearbySameFactionChange = { includeNearbySameFaction = it },
            includeNearbyOppositeFaction = includeNearbyOppositeFaction,
            onIncludeNearbyOppositeFactionChange = { includeNearbyOppositeFaction = it },
            requirePvPParticipation = requirePvPParticipation,
            onRequirePvPParticipationChange = onRequirePvPParticipationChange,
            includePlayersThatLeftRaid = includePlayersThatLeftRaid,
            onIncludePlayersThatLeftRaidChange = { includePlayersThatLeftRaid = it },
            attendanceNames = attendanceNames,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
  }
}
@Composable
private fun AttendanceRaidPane(
  mainRaid: List<List<RaidFramePayload>>,
  coRaid: List<List<RaidFramePayload>>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(Color(0xFF1A1A1A).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    FlowRow(
      modifier = Modifier.wrapContentWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (mainRaid.isNotEmpty()) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = stringResource(Res.string.raid_main_raid_label),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          RaidComponent(
            parties = mainRaid,
            modifier = Modifier.wrapContentSize()
          )
        }
      }
      if (coRaid.isNotEmpty()) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = stringResource(Res.string.raid_coraid_label),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          RaidComponent(
            parties = coRaid,
            modifier = Modifier.wrapContentSize()
          )
        }
      }
    }
  }
}
@Composable
private fun AttendanceControlsPane(
  includeMain: Boolean,
  onIncludeMainChange: (Boolean) -> Unit,
  includeCo: Boolean,
  onIncludeCoChange: (Boolean) -> Unit,
  includeNearbySameFaction: Boolean,
  onIncludeNearbySameFactionChange: (Boolean) -> Unit,
  includeNearbyOppositeFaction: Boolean,
  onIncludeNearbyOppositeFactionChange: (Boolean) -> Unit,
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit,
  includePlayersThatLeftRaid: Boolean,
  onIncludePlayersThatLeftRaidChange: (Boolean) -> Unit,
  attendanceNames: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(Color(0xFF1A1A1A).copy(alpha = 0.76f), RoundedCornerShape(14.dp))
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_main_raid),
      initialChecked = includeMain,
      onCheckedChange = onIncludeMainChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_coraid),
      initialChecked = includeCo,
      onCheckedChange = onIncludeCoChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_nearby_same_faction),
      initialChecked = includeNearbySameFaction,
      onCheckedChange = onIncludeNearbySameFactionChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_nearby_opposite_faction),
      initialChecked = includeNearbyOppositeFaction,
      onCheckedChange = onIncludeNearbyOppositeFactionChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_require_pvp_filter),
      initialChecked = requirePvPParticipation,
      onCheckedChange = onRequirePvPParticipationChange,
      textColor = Color.White
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_include_departed),
      initialChecked = includePlayersThatLeftRaid,
      onCheckedChange = onIncludePlayersThatLeftRaidChange,
      textColor = Color.White
    )
    SelectableTextField(
      value = attendanceNames,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
    )
    Button(
      onClick = {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(attendanceNames), null)
      },
      colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
      modifier = Modifier.padding(top = 2.dp)
    ) {
      Text(text = stringResource(Res.string.raid_copy_attendance), color = Color.Black)
    }
  }
}
@Composable
private fun NearbyTab(
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()

  val filteredHaranya = if (requirePvPParticipation) nearbyHaranya.filter { it.hasPvPParticipation() } else nearbyHaranya
  val filteredNuia = if (requirePvPParticipation) nearbyNuia.filter { it.hasPvPParticipation() } else nearbyNuia
  val filteredPirate = if (requirePvPParticipation) nearbyPirate.filter { it.hasPvPParticipation() } else nearbyPirate

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = String.format(stringResource(Res.string.raid_haranya_faction), filteredHaranya.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = filteredHaranya.mapIndexed { index, card ->
            RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole, gearScore = card.lastKnownGearScore)
          }.chunked(5),
          modifier = Modifier.wrapContentSize()
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = String.format(stringResource(Res.string.raid_nuian_faction), filteredNuia.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = filteredNuia.mapIndexed { index, card ->
            RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole, gearScore = card.lastKnownGearScore)
          }.chunked(5),
          modifier = Modifier.wrapContentSize()
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = String.format(stringResource(Res.string.raid_pirate_faction), filteredPirate.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        RaidComponent(
          parties = filteredPirate.mapIndexed { index, card ->
            RaidFramePayload(slot = index, playerName = card.name, role = card.currentRole, gearScore = card.lastKnownGearScore)
          }.chunked(5),
          modifier = Modifier.wrapContentSize()
        )
      }
    }
    Text(
      text = stringResource(Res.string.raid_nearby_disclaimer),
      color = Color.LightGray,
      fontSize = 11.sp,
      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
    CheckBoxComponent(
      label = stringResource(Res.string.raid_nearby_require_pvp),
      initialChecked = requirePvPParticipation,
      onCheckedChange = onRequirePvPParticipationChange,
      textColor = Color.White
    )
  }
}

private fun averageGearScore(players: List<PlayerCard>): Int {
  val known = players.filter { it.lastKnownGearScore > 0 }
  return if (known.isEmpty()) 0 else known.map { it.lastKnownGearScore }.average().toInt()
}

@Composable
private fun NearbyGearTab(
  nearbyNuia: List<PlayerCard>,
  nearbyHaranya: List<PlayerCard>,
  nearbyPirate: List<PlayerCard>,
  requirePvPParticipation: Boolean,
  onRequirePvPParticipationChange: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()

  val filteredHaranya = if (requirePvPParticipation) nearbyHaranya.filter { it.hasPvPParticipation() } else nearbyHaranya
  val filteredNuia = if (requirePvPParticipation) nearbyNuia.filter { it.hasPvPParticipation() } else nearbyNuia
  val filteredPirate = if (requirePvPParticipation) nearbyPirate.filter { it.hasPvPParticipation() } else nearbyPirate

  val avgHaranya = averageGearScore(filteredHaranya)
  val avgNuia = averageGearScore(filteredNuia)
  val avgPirate = averageGearScore(filteredPirate)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = String.format(stringResource(Res.string.raid_haranya_faction), filteredHaranya.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = String.format(stringResource(Res.string.nearby_avg_gs_format), avgHaranya, filteredHaranya.count { it.lastKnownGearScore > 0 }),
          color = Color.LightGray,
          fontSize = 11.sp
        )
        GearScoreHistogram(
          players = filteredHaranya,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = String.format(stringResource(Res.string.raid_nuian_faction), filteredNuia.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = String.format(stringResource(Res.string.nearby_avg_gs_format), avgNuia, filteredNuia.count { it.lastKnownGearScore > 0 }),
          color = Color.LightGray,
          fontSize = 11.sp
        )
        GearScoreHistogram(
          players = filteredNuia,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = String.format(stringResource(Res.string.raid_pirate_faction), filteredPirate.size),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = String.format(stringResource(Res.string.nearby_avg_gs_format), avgPirate, filteredPirate.count { it.lastKnownGearScore > 0 }),
          color = Color.LightGray,
          fontSize = 11.sp
        )
        GearScoreHistogram(
          players = filteredPirate,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
    CheckBoxComponent(
      label = stringResource(Res.string.raid_nearby_require_pvp),
      initialChecked = requirePvPParticipation,
      onCheckedChange = onRequirePvPParticipationChange,
      textColor = Color.White
    )
  }
}
