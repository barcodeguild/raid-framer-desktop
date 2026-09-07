package com.reoky.raidframer.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.browser.BROWSER_STATS
import com.reoky.raidframer.core.browser.BrowserCondition
import com.reoky.raidframer.core.browser.applyBrowserFilters
import com.reoky.raidframer.core.browser.exportBrowserCsv
import com.reoky.raidframer.core.browser.parseShorthand
import com.reoky.raidframer.core.browser.sortBrowser
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.LeadershipRole
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.helpers.FaIcon
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PocketNav
import com.reoky.raidframer.ui.component.TitleBarComponent
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.player_browser_asc
import raid_framer_desktop.composeapp.generated.resources.player_browser_add
import raid_framer_desktop.composeapp.generated.resources.player_browser_count_format
import raid_framer_desktop.composeapp.generated.resources.player_browser_desc
import raid_framer_desktop.composeapp.generated.resources.player_browser_export_dialog_title
import raid_framer_desktop.composeapp.generated.resources.player_browser_export_rows_format
import raid_framer_desktop.composeapp.generated.resources.player_browser_faction_format
import raid_framer_desktop.composeapp.generated.resources.player_browser_filter_guild
import raid_framer_desktop.composeapp.generated.resources.player_browser_filter_min_gs
import raid_framer_desktop.composeapp.generated.resources.player_browser_filter_spec
import raid_framer_desktop.composeapp.generated.resources.player_browser_filter_val_hint
import raid_framer_desktop.composeapp.generated.resources.player_browser_hint
import raid_framer_desktop.composeapp.generated.resources.player_browser_lifetime_totals
import raid_framer_desktop.composeapp.generated.resources.player_browser_loading
import raid_framer_desktop.composeapp.generated.resources.player_browser_nothing_to_export
import raid_framer_desktop.composeapp.generated.resources.player_browser_role_all
import raid_framer_desktop.composeapp.generated.resources.player_browser_role_guild_lead
import raid_framer_desktop.composeapp.generated.resources.player_browser_role_hero
import raid_framer_desktop.composeapp.generated.resources.player_browser_role_none
import raid_framer_desktop.composeapp.generated.resources.player_browser_role_raid_lead
import raid_framer_desktop.composeapp.generated.resources.player_browser_role_shot_caller
import raid_framer_desktop.composeapp.generated.resources.player_browser_search_placeholder
import raid_framer_desktop.composeapp.generated.resources.player_browser_title
import raid_framer_desktop.composeapp.generated.resources.player_browser_role_label_format
import raid_framer_desktop.composeapp.generated.resources.player_browser_seen_format
import raid_framer_desktop.composeapp.generated.resources.player_browser_sort_format

private val LAST_SEEN_OPTIONS = listOf("All" to 0L, "7d" to 7L, "30d" to 30L, "90d" to 90L)
private val OP_OPTIONS = listOf(">", ">=", "<", "=")
private val ROLE_OPTIONS = listOf("All", "None", "Raid Lead", "Guild Lead", "Hero", "Shot Caller")
private const val TICKER_MS = 6000L

private fun roleLabelRes(v: Int): org.jetbrains.compose.resources.StringResource = when (LeadershipRole.fromInt(v)) {
  LeadershipRole.NONE -> Res.string.player_browser_role_none
  LeadershipRole.RAID_LEAD -> Res.string.player_browser_role_raid_lead
  LeadershipRole.GUILD_LEAD -> Res.string.player_browser_role_guild_lead
  LeadershipRole.FACTION_HERO -> Res.string.player_browser_role_hero
  LeadershipRole.SHOT_CALLER -> Res.string.player_browser_role_shot_caller
  LeadershipRole.GM -> Res.string.player_browser_role_none
}

private fun roleLabel(v: Int): String = when (LeadershipRole.fromInt(v)) {
  LeadershipRole.NONE -> "None"
  LeadershipRole.RAID_LEAD -> "Raid Lead"
  LeadershipRole.GUILD_LEAD -> "Guild Lead"
  LeadershipRole.FACTION_HERO -> "Hero"
  LeadershipRole.SHOT_CALLER -> "Shot Caller"
  LeadershipRole.GM -> "GM"
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PlayerBrowserOverlay(wm: WindowManager?) {
  val dragLock = LocalDragLock.current
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
  val scope = rememberCoroutineScope()
  var allPlayers by remember { mutableStateOf<List<PlayerCacheEntity>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }
  var search by remember { mutableStateOf("") }
  var dropdownVisible by remember { mutableStateOf(false) }
  // Keyboard highlight within the suggestion list. -1 = follow the text field.
  var activeIndex by remember { mutableStateOf(-1) }
  var selected by remember { mutableStateOf<PlayerCacheEntity?>(null) }
  var flyout by remember { mutableStateOf<PlayerCacheEntity?>(null) }
  var sortKey by remember { mutableStateOf("damage") }
  var descending by remember { mutableStateOf(true) }
  var factionFilter by remember { mutableStateOf("All") }
  var guildFilter by remember { mutableStateOf("") }
  var specFilter by remember { mutableStateOf("") }
  var gearFilter by remember { mutableStateOf("") }
  var lastSeenFilter by remember { mutableStateOf("All") }
  var roleFilter by remember { mutableStateOf("All") }
  var conditions by remember { mutableStateOf(listOf<BrowserCondition>()) }
  var checked by remember { mutableStateOf(setOf<String>()) }
  var tickerStats by remember { mutableStateOf(BROWSER_STATS.shuffled().take(4)) }
  var exportStatus by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    loading = true
    val data = withContext(Dispatchers.IO) {
      try { RFDao.playerCacheDao.getRecentPlayerCacheMetadata() } catch (e: Exception) { emptyList() }
    }
    // Hard limiter: never show GM-role players (e.g. event dragon admin).
    allPlayers = data.filter { it.leaderships != LeadershipRole.GM.value }
    loading = false
  }

  var searchFocused by remember { mutableStateOf(false) }

  // Reconcile drag lock from the two sources of truth (dropdown + focus)
  // instead of writing it from individual event handlers, which raced and
  // left it stuck (e.g. pickPlayer() cleared it, then a stale focus event set it).
  fun syncBrowserDragLock() {
    dragLock.value = dropdownVisible || searchFocused
  }

  LaunchedEffect(dropdownVisible, searchFocused) {
    syncBrowserDragLock()
  }

  LaunchedEffect(Unit) {
    while (true) {
      delay(TICKER_MS)
      tickerStats = BROWSER_STATS.shuffled().take(4)
    }
  }

  val lastSeenAfter = remember(lastSeenFilter) {
    val days = LAST_SEEN_OPTIONS.firstOrNull { it.first == lastSeenFilter }?.second ?: 0L
    if (days <= 0L) 0L else System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
  }

  val searchQuery = remember(search) { search.trim() }

  val filtered = remember(allPlayers, searchQuery, factionFilter, guildFilter, specFilter, gearFilter, lastSeenAfter, roleFilter, conditions, sortKey, descending) {
    val gear = gearFilter.toIntOrNull() ?: 0
    var list = sortBrowser(
      applyBrowserFilters(allPlayers, searchQuery, factionFilter, guildFilter, specFilter, gear, lastSeenAfter, conditions),
      sortKey, descending
    )
    if (roleFilter != "All") list = list.filter { roleLabel(it.leaderships) == roleFilter }
    list
  }

  LaunchedEffect(filtered) {
    checked = checked.intersect(filtered.map { it.playerName }.toSet())
  }

  val suggestions = remember(searchQuery, allPlayers) {
    if (searchQuery.isBlank()) emptyList()
    else allPlayers.filter {
      it.playerName.contains(searchQuery, true) || it.lastKnownGuild.contains(searchQuery, true)
    }.take(8)
  }

  // Clamp the keyboard highlight whenever the list changes.
  LaunchedEffect(suggestions) {
    if (activeIndex >= suggestions.size) activeIndex = suggestions.size - 1
  }

  // Display text: picked player renders "Name [Guild]" (guild is visual only —
  // filtering always uses the stripped query). Typing shows the raw query.
  fun displayFor(p: PlayerCacheEntity): String =
    if (p.lastKnownGuild.isNotBlank()) "${p.playerName} [${p.lastKnownGuild}]" else p.playerName

  fun pickPlayer(p: PlayerCacheEntity) {
    selected = p
    search = displayFor(p)
    dropdownVisible = false
    activeIndex = -1
    searchFocused = false
    dragLock.value = false
    focusManager.clearFocus()
    syncBrowserDragLock()
  }

  // Mouse clicks on dropdown rows arrive while the text field is focused; the
  // focus change can recompose (and hide) the dropdown before clickable fires.
  // Press handlers dodge that race: they run on the down event. Deferred
  // onSelect-equivalent keeps the pointer-up (which the window-drag
  // MouseListener would otherwise see as a tooltip drag) suppressed.
  var lastPickMs by remember { mutableStateOf(0L) }
  fun pickPlayerGuarded(p: PlayerCacheEntity, via: (PlayerCacheEntity) -> Unit) {
    val now = System.currentTimeMillis()
    if (now - lastPickMs < 500) return
    lastPickMs = now
    via(p)
  }
  fun pickPlayerOnPressDown(p: PlayerCacheEntity) {
    selected = p
    search = displayFor(p)
    dropdownVisible = false
    activeIndex = -1
    searchFocused = false
    focusManager.clearFocus()
    dragLock.value = true
    scope.launch {
      delay(150)
      dragLock.value = false
      syncBrowserDragLock()
    }
  }

  fun clearSearch() {
    selected = null
    search = ""
    dropdownVisible = false
    activeIndex = -1
    searchFocused = false
    focusManager.clearFocus()
    syncBrowserDragLock()
  }

  fun openJournalFor(name: String) {
    PocketNav.journalQuery = "#$name"
    wm?.openWindow(OverlayType.POCKET_JOURNAL)
  }

  fun openEditorFor(name: String) {
    scope.launch(Dispatchers.IO) {
      try {
        PocketDraftCoordinator.createDraft(title = name, markdown = "#$name ")
        withContext(Dispatchers.Main) { wm?.openWindow(OverlayType.POCKET_EDITOR) }
      } catch (e: Exception) { }
    }
  }

  val exportRows = remember(filtered, checked) {
    if (checked.isEmpty()) filtered else filtered.filter { checked.contains(it.playerName) }
  }
  val searchPlaceholder = stringResource(Res.string.player_browser_search_placeholder)
  val titleText = stringResource(Res.string.player_browser_title)
  val hintText = stringResource(Res.string.player_browser_hint)
  val exportRowsText = stringResource(Res.string.player_browser_export_rows_format, exportRows.size)
  val exportDialogTitle = stringResource(Res.string.player_browser_export_dialog_title)
  val loadingText = stringResource(Res.string.player_browser_loading)
  val countText = stringResource(Res.string.player_browser_count_format, filtered.size)
  val lifetimeTotalsText = stringResource(Res.string.player_browser_lifetime_totals)
  val nothingToExportText = stringResource(Res.string.player_browser_nothing_to_export)
  val allText = stringResource(Res.string.player_browser_role_all)
  val roleNoneText = stringResource(Res.string.player_browser_role_none)
  val roleRaidLeadText = stringResource(Res.string.player_browser_role_raid_lead)
  val roleGuildLeadText = stringResource(Res.string.player_browser_role_guild_lead)
  val roleHeroText = stringResource(Res.string.player_browser_role_hero)
  val roleShotCallerText = stringResource(Res.string.player_browser_role_shot_caller)
  fun roleValueToLabel(v: String): String = when (v) {
    "None" -> roleNoneText
    "Raid Lead" -> roleRaidLeadText
    "Guild Lead" -> roleGuildLeadText
    "Hero" -> roleHeroText
    "Shot Caller" -> roleShotCallerText
    else -> allText
  }
  fun roleLabelToValue(label: String): String = when (label) {
    roleNoneText -> "None"
    roleRaidLeadText -> "Raid Lead"
    roleGuildLeadText -> "Guild Lead"
    roleHeroText -> "Hero"
    roleShotCallerText -> "Shot Caller"
    else -> "All"
  }

  Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
    TitleBarComponent(title = titleText, onClose = { wm?.closeWindow(OverlayType.PLAYER_BROWSER) })

    Column(Modifier.fillMaxSize().padding(10.dp)) {
      // Compact search (32dp, BasicTextField so text is never clipped)
      Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(32.dp)) {
          BasicTextField(
            value = search,
            onValueChange = { search = it; dropdownVisible = true; activeIndex = -1 },
            modifier = Modifier.fillMaxWidth().height(32.dp).focusRequester(focusRequester)
              .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(6.dp))
              .onFocusChanged {
                searchFocused = it.isFocused
                syncBrowserDragLock()
                if (!it.isFocused) {
                  dropdownVisible = false
                  activeIndex = -1
                }
              }
              .onKeyEvent {
                when (it.key) {
                  Key.Enter -> {
                    val target = if (activeIndex in suggestions.indices) suggestions[activeIndex]
                    else suggestions.firstOrNull()
                    target?.let { p -> pickPlayer(p) }
                    true
                  }
                  Key.Escape -> {
                    dropdownVisible = false
                    activeIndex = -1
                    syncBrowserDragLock()
                    true
                  }
                  Key.DirectionDown -> {
                    if (suggestions.isNotEmpty()) {
                      dropdownVisible = true
                      activeIndex = (activeIndex + 1) % suggestions.size
                    }
                    true
                  }
                  Key.DirectionUp -> {
                    if (suggestions.isNotEmpty()) {
                      dropdownVisible = true
                      activeIndex = if (activeIndex < 0) suggestions.size - 1
                      else (activeIndex - 1 + suggestions.size) % suggestions.size
                    }
                    true
                  }
                  else -> false
                }
              },
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
            cursorBrush = SolidColor(RFColors.AccentRed),
            decorationBox = { inner ->
              Box(Modifier.fillMaxSize().padding(end = 28.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                if (search.isEmpty()) Text(searchPlaceholder, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1)
                inner()
              }
            }
          )
          if (search.isNotEmpty()) {
            var lastClearMs by remember { mutableStateOf(0L) }
            fun clearGuarded() {
              val now = System.currentTimeMillis()
              if (now - lastClearMs < 500) return
              lastClearMs = now
              clearSearch()
            }
            Box(
              modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .onPointerEvent(PointerEventType.Press) {
                  // Same tooltip-drag race as suggestion rows: the native
                  // mousePressed arms a window drag before clickable fires.
                  // Suppress drag on press so the click lands as a clear.
                  searchFocused = false
                  dropdownVisible = false
                  dragLock.value = true
                  clearGuarded()
                  scope.launch {
                    delay(150)
                    dragLock.value = false
                    syncBrowserDragLock()
                  }
                }
                .clickable { clearGuarded() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("✕", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        } // end 32dp field box
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
                  .onPointerEvent(PointerEventType.Press) { pickPlayerGuarded(p, ::pickPlayerOnPressDown) }
                  .clickable { pickPlayerGuarded(p, ::pickPlayer) }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Text(p.playerName, color = RFColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (p.lastKnownGuild.isNotBlank()) {
                  Text("  [${p.lastKnownGuild}]", color = RFColors.TextTertiary, fontSize = 11.sp)
                }
              }
            }
          }
        } // end dropdown
      } // end search column

      Spacer(Modifier.height(8.dp))

      // Animated ticker: 4 random stat leaders, cross-fading every 6s
      AnimatedContent(
        targetState = tickerStats.map { it.key },
        transitionSpec = { fadeIn() togetherWith fadeOut() }
      ) { _ ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          tickerStats.forEach { def ->
            val top = sortBrowser(filtered, def.key, true).take(3)
            Column(
              modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141414))
                .clickable { sortKey = def.key }
                .padding(8.dp)
            ) {
              Text(def.label.uppercase(), color = def.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              top.forEachIndexed { r, p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("#${r + 1} ", color = RFColors.TextTertiary, fontSize = 11.sp)
                  Text(p.playerName, color = RFColors.TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                  Text(def.extract(p).humanReadableAbbreviation(), color = def.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
              if (top.isEmpty()) Text("—", color = RFColors.TextTertiary, fontSize = 11.sp)
            }
          }
        }
      }

      Spacer(Modifier.height(6.dp))

      FilterBar(
        factionFilter = factionFilter, onFaction = { factionFilter = it },
        guildFilter = guildFilter, onGuild = { guildFilter = it },
        specFilter = specFilter, onSpec = { specFilter = it },
        gearFilter = gearFilter, onGear = { gearFilter = it },
        lastSeenFilter = lastSeenFilter, onLastSeen = { lastSeenFilter = it },
        roleFilter = roleFilter,
        sortKey = sortKey, onSort = { sortKey = it },
        descending = descending, onDir = { descending = it },
        conditions = conditions, onAdd = { conditions = (conditions + it).take(5) },
        onRemove = { conditions = conditions - it },
        roleDisplay = { roleValueToLabel(it) },
        allText = allText,
        onRole2 = { roleFilter = roleLabelToValue(it) }
      )

      Spacer(Modifier.height(6.dp))

      // Toolbar: lifetime-totals hint + always-visible export
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          hintText,
          color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Button(
          onClick = {
            exportStatus = ""
            val rows = exportRows.toList()
            if (rows.isEmpty()) { exportStatus = nothingToExportText; return@Button }
            val summary = buildExportSummary(sortKey, descending, factionFilter, guildFilter, specFilter, gearFilter, lastSeenFilter, conditions)
            val exportScope = CoroutineScope(Dispatchers.IO)
            wm?.closeWindow(OverlayType.PLAYER_BROWSER)
            showCsvSaveChooser(
              suggestedName = summary,
              dialogTitle = exportDialogTitle,
              onFileSelected = { file ->
                exportScope.launch {
                  try {
                    exportBrowserCsv(rows, file)
                    try { Desktop.getDesktop().open(file.parentFile) } catch (e: Exception) { }
                  } catch (e: Exception) { }
                  withContext(Dispatchers.Main) { wm?.openWindow(OverlayType.PLAYER_BROWSER) }
                }
              },
              onCancel = { wm?.openWindow(OverlayType.PLAYER_BROWSER) }
            )
          },
          colors = ButtonDefaults.buttonColors(RFColors.AccentRed), modifier = Modifier.height(32.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
          Text(exportRowsText, color = Color.White, fontSize = 12.sp)
        }
      }
      if (exportStatus.isNotBlank()) Text(exportStatus, color = RFColors.TextTertiary, fontSize = 10.sp)

      Spacer(Modifier.height(6.dp))

      Box(Modifier.fillMaxSize().weight(1f)) {
        Column(Modifier.fillMaxSize()) {
          if (loading) {
            Text(loadingText, color = RFColors.TextTertiary, fontSize = 13.sp)
          } else {
            Text(countText, color = RFColors.TextTertiary, fontSize = 11.sp)
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              itemsIndexed(filtered.take(400), key = { _, p -> p.playerName }) { i, p ->
                BrowserRow(
                  index = i, p = p, sortKey = sortKey,
                  isChecked = checked.isEmpty() || checked.contains(p.playerName),
                  showCheck = checked.isNotEmpty(),
                  onCheck = { on ->
                    checked = if (on) checked + p.playerName else checked - p.playerName
                  },
                  onClick = { flyout = p },
                  onJournal = { openJournalFor(p.playerName) },
                  onEdit = { openEditorFor(p.playerName) }
                )
              }
            }
          }
        }
        // Flyout lifetime breakdown (dismiss on outside click)
        flyout?.let { fp ->
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { flyout = null }) {
            Column(
              Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(300.dp)
                .clip(RoundedCornerShape(10.dp)).background(RFColors.PopupBackground)
                .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp)).padding(12.dp)
                .clickable(enabled = false) { }
            ) {
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(fp.playerName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("✕", color = RFColors.TextTertiary, fontSize = 13.sp, modifier = Modifier.clickable { flyout = null }.padding(4.dp))
              }
              Text(
                "${fp.lastKnownGuild} • ${fp.lastKnownSpec} • GS ${fp.lastKnownGearScore} • ${stringResource(roleLabelRes(fp.leaderships))}",
                color = RFColors.TextSecondary, fontSize = 11.sp)
              Spacer(Modifier.height(8.dp))
              Text(lifetimeTotalsText, color = RFColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              Spacer(Modifier.height(4.dp))
              BROWSER_STATS.forEach { def ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text(def.label, color = RFColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                  Text(def.extract(fp).humanReadableAbbreviation(), color = def.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BrowserRow(
  index: Int, p: PlayerCacheEntity, sortKey: String,
  isChecked: Boolean, showCheck: Boolean, onCheck: (Boolean) -> Unit,
  onClick: () -> Unit, onJournal: () -> Unit, onEdit: () -> Unit
) {
  val def = (BROWSER_STATS).firstOrNull { it.key == sortKey } ?: BROWSER_STATS[0]
  val dmgDef = BROWSER_STATS[0]; val healDef = BROWSER_STATS[1]; val ccDef = BROWSER_STATS[2]
  val killDef = BROWSER_STATS.first { it.key == "kills" }
  val spec = remember(p.lastKnownSpec) {
    try { SpecType.fromName(p.lastKnownSpec) } catch (e: Exception) { SpecType.UNKNOWN }
  }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF181818)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    if (showCheck) {
      Checkbox(
        checked = isChecked, onCheckedChange = onCheck,
        colors = CheckboxDefaults.colors(checkedColor = RFColors.AccentRed, uncheckedColor = RFColors.TextTertiary, checkmarkColor = Color.White),
        modifier = Modifier.size(20.dp)
      )
      Spacer(Modifier.width(4.dp))
    } else {
      Checkbox(
        checked = true, onCheckedChange = { if (!it) onCheck(false) },
        colors = CheckboxDefaults.colors(checkedColor = RFColors.AccentRed, uncheckedColor = RFColors.TextTertiary, checkmarkColor = Color.White),
        modifier = Modifier.size(20.dp)
      )
      Spacer(Modifier.width(4.dp))
    }
    Text("${index + 1}.", color = RFColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(30.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (spec == null || spec == SpecType.UNKNOWN) {
        Image(skillTreeIconPainterFor(null), "?", Modifier.size(16.dp))
        Image(skillTreeIconPainterFor(null), "?", Modifier.size(16.dp))
        Image(skillTreeIconPainterFor(null), "?", Modifier.size(16.dp))
      } else {
        spec.trees.sortedByDisplayOrder().forEach { tree ->
          Image(skillTreeIconPainterFor(tree), null, Modifier.size(16.dp).padding(horizontal = 1.dp))
        }
      }
    }
    Spacer(Modifier.width(6.dp))
    Column(Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(p.playerName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
          maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(4.dp))
        Box(Modifier.size(6.dp).background(
          Faction.fromString(RFConfig.state.value.playerFaction)
            .getFactionHighlightColor(Faction.fromString(p.lastKnownFaction)), androidx.compose.foundation.shape.CircleShape))
      }
      Text("${p.lastKnownGuild} • ${p.lastKnownSpec} • GS ${p.lastKnownGearScore}",
        color = RFColors.TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(2.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip("DMG", p.lifetimeTotalDamage.humanReadableAbbreviation(), dmgDef.color)
        StatChip("Heal", p.lifetimeTotalHealing.humanReadableAbbreviation(), healDef.color)
        StatChip("CC", p.lifetimeTotalCCDelivered.humanReadableAbbreviation(), ccDef.color)
        StatChip("K", p.lifetimeTotalKills.toString(), killDef.color)
      }
    }
    Column(horizontalAlignment = Alignment.End) {
      Text(def.extract(p).humanReadableAbbreviation(), color = def.color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Text(def.label, color = RFColors.TextTertiary, fontSize = 9.sp)
    }
    Spacer(Modifier.width(6.dp))
    JournalFaButton(onClick = onJournal)
    EditFaButton(onClick = onEdit)
  }
}

@Composable
private fun RowScope.JournalFaButton(onClick: () -> Unit) {
  Box(Modifier.padding(2.dp).clip(RoundedCornerShape(4.dp)).clickable { onClick() }.padding(4.dp)) {
    FaIcon(codepoint = "\uf02d", useSolid = true, sizeSp = 13, color = RFColors.TextPrimary)
  }
}

@Composable
private fun RowScope.EditFaButton(onClick: () -> Unit) {
  Box(Modifier.padding(2.dp).clip(RoundedCornerShape(4.dp)).clickable { onClick() }.padding(4.dp)) {
    FaIcon(codepoint = "\uf303", useSolid = true, sizeSp = 13, color = RFColors.TextPrimary)
  }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(label, color = RFColors.TextTertiary, fontSize = 9.sp)
    Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun FilterBar(
  factionFilter: String, onFaction: (String) -> Unit,
  guildFilter: String, onGuild: (String) -> Unit,
  specFilter: String, onSpec: (String) -> Unit,
  gearFilter: String, onGear: (String) -> Unit,
  lastSeenFilter: String, onLastSeen: (String) -> Unit,
  roleFilter: String,
  sortKey: String, onSort: (String) -> Unit,
  descending: Boolean, onDir: (Boolean) -> Unit,
  conditions: List<BrowserCondition>, onAdd: (BrowserCondition) -> Unit,
  onRemove: (BrowserCondition) -> Unit,
  roleDisplay: (String) -> String,
  allText: String,
  onRole2: (String) -> Unit
) {
  val guildText = stringResource(Res.string.player_browser_filter_guild)
  val specText = stringResource(Res.string.player_browser_filter_spec)
  val minGsText = stringResource(Res.string.player_browser_filter_min_gs)
  val descText = stringResource(Res.string.player_browser_desc)
  val ascText = stringResource(Res.string.player_browser_asc)
  val addText = stringResource(Res.string.player_browser_add)
  val valHint = stringResource(Res.string.player_browser_filter_val_hint)
  val factionFmt = stringResource(Res.string.player_browser_faction_format, factionFilter)
  val seenFmt = stringResource(Res.string.player_browser_seen_format, lastSeenFilter)
  val roleFmt = stringResource(Res.string.player_browser_role_label_format, roleDisplay(roleFilter))
  val sortFmt = stringResource(Res.string.player_browser_sort_format, BROWSER_STATS.firstOrNull { it.key == sortKey }?.label ?: sortKey)
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      MiniDropdown(factionFmt, listOf(allText, "Nuia", "Haranya", "Pirate")) { onFaction(it) }
      MiniField(guildText, guildFilter, onGuild, Modifier.weight(1f))
      MiniField(specText, specFilter, onSpec, Modifier.weight(1f))
      MiniField(minGsText, gearFilter, onGear, Modifier.width(90.dp))
      MiniDropdown(seenFmt, LAST_SEEN_OPTIONS.map { it.first }) { onLastSeen(it) }
      MiniDropdown(roleFmt, ROLE_OPTIONS.map { roleDisplay(it) }) { label -> onRole2(label) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      MiniDropdown(sortFmt,
        BROWSER_STATS.map { it.key }) { onSort(it) }
      Text(if (descending) descText else ascText, color = RFColors.AccentRed, fontSize = 11.sp,
        modifier = Modifier.clickable { onDir(!descending) })
      conditions.forEach { c ->
        Text("${c.statKey} ${c.op} ${c.value}  ✕", color = RFColors.TextPrimary, fontSize = 11.sp,
          modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A)).padding(horizontal = 6.dp, vertical = 2.dp)
            .clickable { onRemove(c) })
      }
      if (conditions.size < 5) ConditionAdder(valHint, addText) { onAdd(it) }
    }
  }
}

@Composable
private fun MiniDropdown(label: String, options: List<String>, onPick: (String) -> Unit) {
  var open by remember { mutableStateOf(false) }
  Box {
    Text(label, color = Color.White, fontSize = 11.sp,
      modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A))
        .clickable { open = true }.padding(horizontal = 8.dp, vertical = 4.dp))
    MaterialTheme(
      colors = MaterialTheme.colors.copy(surface = Color(0xFF1E1E1E))
    ) {
    DropdownMenu(
      expanded = open,
      onDismissRequest = { open = false },
      modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp)).border(1.dp, RFColors.CardBorder, RoundedCornerShape(6.dp))
    ) {
      options.take(60).forEach { o ->
        DropdownMenuItem(onClick = { onPick(o); open = false }) {
          Text(o, color = Color.White, fontSize = 11.sp)
        }
      }
    }
    }
  }
}

@Composable
private fun MiniField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
  val dragLock = LocalDragLock.current
  BasicTextField(
    value = value, onValueChange = onChange,
    singleLine = true, modifier = modifier.height(32.dp)
      .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
      .border(1.dp, RFColors.CardBorder, RoundedCornerShape(4.dp))
      .onFocusChanged { dragLock.value = it.isFocused },
    textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
    cursorBrush = SolidColor(RFColors.AccentRed),
    decorationBox = { inner ->
      Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1)
        inner()
      }
    }
  )
}

@Composable
private fun ConditionAdder(valHint: String, addText: String, onAdd: (BrowserCondition) -> Unit) {
  var stat by remember { mutableStateOf("damage") }
  var op by remember { mutableStateOf(">") }
  var value by remember { mutableStateOf("") }
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    MiniDropdown(BROWSER_STATS.firstOrNull { it.key == stat }?.label ?: stat, BROWSER_STATS.map { it.key }) { stat = it }
    MiniDropdown(op, OP_OPTIONS) { op = it }
    MiniField(valHint, value, { value = it }, Modifier.width(90.dp))
    Text(addText, color = RFColors.AccentRed, fontSize = 11.sp, modifier = Modifier.clickable {
      val v = parseShorthand(value)
      onAdd(BrowserCondition(stat, op, v))
      value = ""
    })
  }
}

private fun buildExportSummary(
  sortKey: String,
  descending: Boolean,
  faction: String,
  guild: String,
  spec: String,
  gear: String,
  lastSeen: String,
  conditions: List<BrowserCondition>
): String {
  val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(java.util.Date())
  val parts = mutableListOf("sort-$sortKey-${if (descending) "desc" else "asc"}")
  if (faction != "All") parts.add("faction-$faction")
  if (guild.isNotBlank()) parts.add("guild-$guild")
  if (spec.isNotBlank()) parts.add("spec-$spec")
  if (gear.isNotBlank()) parts.add("gs$gear")
  if (lastSeen != "All") parts.add("seen-$lastSeen")
  conditions.forEach { parts.add("${it.statKey}${it.op}${it.value}") }
  val slug = parts.joinToString("_").replace(Regex("[^A-Za-z0-9_.-]"), "")
  return "player-browser_${stamp}_${slug.take(80)}.csv"
}

private fun showCsvSaveChooser(suggestedName: String, dialogTitle: String, onFileSelected: (File) -> Unit, onCancel: () -> Unit = {}) {
  try {
    SwingUtilities.invokeLater {
      try {
        val chooser = JFileChooser()
            chooser.dialogTitle = dialogTitle
        chooser.selectedFile = File(suggestedName)
        chooser.setFileFilter(FileNameExtensionFilter("CSV (*.csv)", "csv"))
        val parent = wm_parentWindow()
        val result = chooser.showSaveDialog(parent)
        if (result == JFileChooser.APPROVE_OPTION) {
          var file = chooser.selectedFile
          if (!file.name.endsWith(".csv", ignoreCase = true)) {
            file = File(file.parentFile, "${file.nameWithoutExtension}.csv")
          }
          onFileSelected(file)
        } else {
          onCancel()
        }
      } catch (e: Exception) { onCancel() }
    }
  } catch (e: Exception) { onCancel() }
}

private fun wm_parentWindow(): java.awt.Component? {
  return try {
    java.awt.Window.getWindows().firstOrNull { it.isVisible && it is java.awt.Frame } as? java.awt.Component
  } catch (e: Exception) { null }
}
