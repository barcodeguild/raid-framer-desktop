package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
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
  var search by remember { mutableStateOf("") }
  var dropdownVisible by remember { mutableStateOf(false) }
  var isFocused by remember { mutableStateOf(false) }
  // Tracks which idleDisplay value was last seeded into `search`, so typing
  // a char doesn't get overwritten on the next recomposition.
  var seededFor by remember { mutableStateOf<String?>(null) }
  // Keyboard highlight within the suggestion list. -1 = follow the text field.
  var activeIndex by remember { mutableStateOf(-1) }
  val focusRequester = remember { FocusRequester() }

  // Shared select path: report the pick, reset query state, and release focus
  // so the field falls back to the idle "Name [Guild]" display. Without the
  // focus release, isFocused stays true and the cleared query renders blank.
  fun pick(name: String) {
    onSelect(name)
    search = ""
    seededFor = null
    dropdownVisible = false
    activeIndex = -1
    isFocused = false
    dragLock.value = false
    focusManager.clearFocus()
  }

  // Reconcile drag lock from the two sources of truth (dropdown + focus)
  // instead of writing it from individual event handlers, which raced and
  // left it stuck (e.g. pick() cleared it, then a stale focus event set it).
  fun syncDragLock() {
    dragLock.value = dropdownVisible || isFocused
  }

  val allPlayers by produceState<List<PlayerCacheEntity>>(
    initialValue = emptyList(), key1 = Unit
  ) {
    value = try {
      withContext(Dispatchers.IO) { RFDao.playerCacheDao.getRecentPlayerCacheMetadata() }
    } catch (e: Exception) { emptyList() }
  }

  val suggestions = remember(search, allPlayers, currentName) {
    if (search.isBlank()) emptyList()
    else {
      val matches = allPlayers.filter {
        it.playerName.contains(search, true) || it.lastKnownGuild.contains(search, true)
      }.take(8)
      // Exclude the already-selected player so typing never offers a
      // one-item self-match (whose pick() resets the field mid-typing).
      matches.filterNot { it.playerName.equals(currentName, ignoreCase = true) && currentName.isNotBlank() }
    }
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

  // Seed the visible query only for genuine selection changes (window open,
  // AppState switch). Deliberately NOT keyed on the DB result: when the player
  // list loads, currentEntry resolves and idleDisplay flips from the bare name
  // to "Name [Guild]" — re-seeding then would wipe whatever the user typed.
  // Typing never triggers this: keystrokes only touch `search`, and picks go
  // through pick() which resets explicitly.
  LaunchedEffect(currentName) {
    if (!isFocused) {
      search = idleDisplay
      seededFor = idleDisplay
    }
  }

  LaunchedEffect(dropdownVisible, isFocused) {
    syncDragLock()
  }

  // Clamp the keyboard highlight whenever the list changes.
  LaunchedEffect(suggestions) {
    if (activeIndex >= suggestions.size) activeIndex = suggestions.size - 1
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
      onValueChange = { search = it; dropdownVisible = true; activeIndex = -1 },
      modifier = Modifier.width(220.dp).height(32.dp).focusRequester(focusRequester)
        .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
        .border(2.dp, pulseBorder, RoundedCornerShape(6.dp))
        .onFocusChanged {
          isFocused = it.isFocused
          if (!it.isFocused) {
            // Commit on blur: keep raw query as typed; idle display re-seeds on next selection change.
            dropdownVisible = false
            activeIndex = -1
          }
          syncDragLock()
        }
        .onKeyEvent {
          when (it.key) {
            Key.Enter -> {
              val target = if (activeIndex in suggestions.indices) suggestions[activeIndex]
              else suggestions.firstOrNull()
              if (target != null) pick(target.playerName)
              true
            }
            Key.Escape -> {
              dropdownVisible = false
              activeIndex = -1
              syncDragLock()
              true
            }
            Key.DirectionDown -> {
              if (suggestions.isNotEmpty()) {
                dropdownVisible = true
                activeIndex = ((activeIndex + 1) % suggestions.size)
              }
              true
            }
            Key.DirectionUp -> {
              if (suggestions.isNotEmpty()) {
                dropdownVisible = true
                activeIndex = if (activeIndex < 0) suggestions.size - 1
                else ((activeIndex - 1 + suggestions.size) % suggestions.size)
              }
              true
            }
            else -> false
          }
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
    if (dropdownVisible && suggestions.isNotEmpty()) {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
          .background(Color(0xFF1E1E1E)).border(1.dp, RFColors.CardBorder, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
          .padding(vertical = 2.dp)
      ) {
        suggestions.forEachIndexed { index, p ->
          val highlighted = index == activeIndex
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
              .background(if (highlighted) RFColors.AccentRed.copy(alpha = 0.25f) else Color.Transparent)
              .clickable { pick(p.playerName) }
              .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
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
