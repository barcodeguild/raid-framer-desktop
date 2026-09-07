package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.rememberSectionPulse
import com.reoky.raidframer.ui.LocalDragLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.player_search_placeholder

/**
 * Compact unified player search box (extracted from the Player Browser search pattern).
 * Shows name/guild suggestions; picking one (or Enter) fires [onSelect] with the player name.
 *
 * @param highlightBorder when true, pulses the border gold (via [rememberSectionPulse])
 *   to draw the user's eye — used when the box is the primary way to pick context.
 */
@Composable
fun PlayerSearchBox(
  currentName: String,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  highlightBorder: Boolean = false
) {
  val dragLock = LocalDragLock.current
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
  var search by remember(currentName) { mutableStateOf("") }
  var dropdownOpen by remember { mutableStateOf(false) }
  var isFocused by remember { mutableStateOf(false) }
  val focusRequester = remember { FocusRequester() }

  // Shared select path: report the pick, reset query state, and release focus
  // so the field falls back to the idle "Name [Guild]" display. Without the
  // focus release, isFocused stays true and the cleared query renders blank.
  fun pick(name: String) {
    onSelect(name)
    search = ""
    dropdownOpen = false
    isFocused = false
    dragLock.value = false
    focusManager.clearFocus()
  }

  val allPlayers by produceState<List<PlayerCacheEntity>>(
    initialValue = emptyList(), key1 = Unit
  ) {
    value = try {
      withContext(Dispatchers.IO) { RFDao.playerCacheDao.getRecentPlayerCacheMetadata() }
    } catch (e: Exception) { emptyList() }
  }

  val suggestions = remember(search, allPlayers) {
    if (search.isBlank()) emptyList()
    else allPlayers.filter {
      it.playerName.contains(search, true) || it.lastKnownGuild.contains(search, true)
    }.take(8)
  }

  // Display "Name [Guild]" like the dropdown rows when idle, matching
  // SessionTypeDropdown's value-if-collapsed pattern. While focused/typing,
  // show the raw query instead. `search` is the source of truth for the
  // visible value — no second idleDisplay branch that can go stale.
  val currentEntry = remember(currentName, allPlayers) {
    allPlayers.firstOrNull { it.playerName.equals(currentName, ignoreCase = true) }
  }
  val idleDisplay = remember(currentName, currentEntry) {
    if (currentName.isBlank()) "" else if (currentEntry != null && currentEntry.lastKnownGuild.isNotBlank()) {
      "${currentEntry.playerName} [${currentEntry.lastKnownGuild}]"
    } else currentName
  }

  // Seed the visible query from the selection whenever it changes
  // (window open, AppState switch, suggestion pick). Editing the field
  // afterwards only touches `search`, so typing never fights the seed.
  // Guild suffix matches the dropdown row format.
  LaunchedEffect(idleDisplay) {
    if (!isFocused) search = idleDisplay
  }

  LaunchedEffect(dropdownOpen, search) {
    dragLock.value = dropdownOpen || search.isNotEmpty()
  }

  Column(modifier = modifier) {
    // One-shot pulse on every fresh composition (i.e. every window open).
    // NOTE: gating rememberSectionPulse on `currentName.isBlank()` looks right but
    // fails in practice: when opened from the Player Card a player is already
    // selected, so `active` is false from the first frame and the pulse never runs.
    // A local one-shot trigger fires regardless of selection state.
    var pulseTrigger by remember { mutableStateOf(highlightBorder) }
    LaunchedEffect(Unit) {
      if (pulseTrigger) {
        kotlinx.coroutines.delay(7000)
        pulseTrigger = false
      }
    }
    val pulseBorder = rememberSectionPulse(active = pulseTrigger)
    CompositionLocalProvider(
      androidx.compose.material.LocalContentColor provides Color.White,
      androidx.compose.material.LocalTextStyle provides TextStyle(fontSize = 11.sp, color = Color.White)
    ) {
    // BasicTextField instead of Material TextField: zero internal padding, so the
    // 32.dp compact height fits the text exactly with no clipping.
    BasicTextField(
      value = search,
      onValueChange = { search = it; dropdownOpen = true },
      modifier = Modifier.width(220.dp).height(32.dp).focusRequester(focusRequester)
        .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
        .border(2.dp, pulseBorder, RoundedCornerShape(6.dp))
        .onFocusChanged {
          isFocused = it.isFocused
          dragLock.value = it.isFocused
          if (it.isFocused) {
            // Select-all-on-focus: next keystroke replaces the name.
            search = idleDisplay
          }
        }
        .onKeyEvent {
          if (it.key == Key.Enter) {
            val pick = suggestions.firstOrNull()
            if (pick != null) pick(pick.playerName)
            true
          } else false
        },
      singleLine = true,
      textStyle = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold),
      cursorBrush = SolidColor(RFColors.AccentRed),
      decorationBox = { innerTextField ->
        Box(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          if (search.isEmpty()) {
            Text(placeholder ?: stringResource(Res.string.player_search_placeholder), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1)
          }
          innerTextField()
        }
      }
    )
    }
    if (dropdownOpen && suggestions.isNotEmpty()) {
      // MaterialTheme surface override kills the default white popup background
      // (the source of the white corner pixels) — same trick as SessionTypeDropdown.
      MaterialTheme(
        colors = MaterialTheme.colors.copy(surface = Color(0xFF1E1E1E))
      ) {
        DropdownMenu(
          expanded = true,
          onDismissRequest = { dropdownOpen = false; dragLock.value = false },
          modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
            .border(1.dp, RFColors.CardBorder, RoundedCornerShape(6.dp))
        ) {
        suggestions.forEach { p ->
          DropdownMenuItem(onClick = { pick(p.playerName) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(p.playerName, color = RFColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              if (p.lastKnownGuild.isNotBlank()) {
                Text("  [${p.lastKnownGuild}]", color = RFColors.TextTertiary, fontSize = 10.sp)
              }
            }
          }
        }
        }
      }
    }
  }
}
