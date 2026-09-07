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

private val LAST_SEEN_OPTIONS = listOf("All" to 0L, "7d" to 7L, "30d" to 30L, "90d" to 90L)
private val OP_OPTIONS = listOf(">", ">=", "<", "=")
private val ROLE_OPTIONS = listOf("All", "None", "Raid Lead", "Guild Lead", "Hero", "Shot Caller")
private const val TICKER_MS = 6000L

private fun roleLabel(v: Int): String = when (LeadershipRole.fromInt(v)) {
  LeadershipRole.NONE -> "None"
  LeadershipRole.RAID_LEAD -> "Raid Lead"
  LeadershipRole.GUILD_LEAD -> "Guild Lead"
  LeadershipRole.FACTION_HERO -> "Hero"
  LeadershipRole.SHOT_CALLER -> "Shot Caller"
  LeadershipRole.GM -> "GM"
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerBrowserOverlay(wm: WindowManager?) {
  val dragLock = LocalDragLock.current
  val scope = rememberCoroutineScope()
  var allPlayers by remember { mutableStateOf<List<PlayerCacheEntity>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }
  var search by remember { mutableStateOf("") }
  var dropdownOpen by remember { mutableStateOf(false) }
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

  LaunchedEffect(dropdownOpen, search) {
    dragLock.value = dropdownOpen || search.isNotEmpty()
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

  val searchQuery = remember(search) { search.substringBefore(" [").trim() }

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

  fun pickPlayer(p: PlayerCacheEntity) {
    selected = p
    search = p.playerName
    dropdownOpen = false
    dragLock.value = false
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

  Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
    TitleBarComponent(title = "Player Browser (Lifetime Totals)", onClose = { wm?.closeWindow(OverlayType.PLAYER_BROWSER) })

    Column(Modifier.fillMaxSize().padding(10.dp)) {
      // Compact search (32dp, BasicTextField so text is never clipped)
      Box(Modifier.fillMaxWidth()) {
        Column {
          BasicTextField(
            value = search,
            onValueChange = { search = it; dropdownOpen = true },
            modifier = Modifier.fillMaxWidth().height(32.dp).focusRequester(focusRequester)
              .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(6.dp))
              .onFocusChanged { dragLock.value = it.isFocused }
              .onKeyEvent {
                if (it.key == Key.Enter) {
                  suggestions.firstOrNull()?.let { p -> pickPlayer(p) }
                  true
                } else false
              },
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
            cursorBrush = SolidColor(RFColors.AccentRed),
            decorationBox = { inner ->
              Box(Modifier.fillMaxSize().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                if (search.isEmpty()) Text("Search players or guilds...", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1)
                inner()
              }
            }
          )
          if (dropdownOpen && suggestions.isNotEmpty()) {
            Column(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(Color(0xFF1E1E1E)).border(1.dp, RFColors.CardBorder, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                .padding(vertical = 2.dp)
            ) {
              suggestions.forEach { p ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth().clickable { pickPlayer(p) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                  Text(p.playerName, color = RFColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  if (p.lastKnownGuild.isNotBlank()) {
                    Text("  [${p.lastKnownGuild}]", color = RFColors.TextTertiary, fontSize = 11.sp)
                  }
                }
              }
            }
          }
        }
      }

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
        roleFilter = roleFilter, onRole = { roleFilter = it },
        sortKey = sortKey, onSort = { sortKey = it },
        descending = descending, onDir = { descending = it },
        conditions = conditions, onAdd = { conditions = (conditions + it).take(5) },
        onRemove = { conditions = conditions - it }
      )

      Spacer(Modifier.height(6.dp))

      // Toolbar: lifetime-totals hint + always-visible export
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "Lifetime totals per player — use Session History to export per-session stats.",
          color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Button(
          onClick = {
            exportStatus = ""
            val rows = exportRows.toList()
            if (rows.isEmpty()) { exportStatus = "Nothing to export."; return@Button }
            val summary = buildExportSummary(sortKey, descending, factionFilter, guildFilter, specFilter, gearFilter, lastSeenFilter, conditions)
            val exportScope = CoroutineScope(Dispatchers.IO)
            wm?.closeWindow(OverlayType.PLAYER_BROWSER)
            showCsvSaveChooser(
              suggestedName = summary,
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
          Text("Export ${exportRows.size} Rows to CSV", color = Color.White, fontSize = 12.sp)
        }
      }
      if (exportStatus.isNotBlank()) Text(exportStatus, color = RFColors.TextTertiary, fontSize = 10.sp)

      Spacer(Modifier.height(6.dp))

      Box(Modifier.fillMaxSize().weight(1f)) {
        Column(Modifier.fillMaxSize()) {
          if (loading) {
            Text("Loading players...", color = RFColors.TextTertiary, fontSize = 13.sp)
          } else {
            Text("${filtered.size} players (lifetime)", color = RFColors.TextTertiary, fontSize = 11.sp)
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
              Text("${fp.lastKnownGuild} • ${fp.lastKnownSpec} • GS ${fp.lastKnownGearScore} • ${roleLabel(fp.leaderships)}",
                color = RFColors.TextSecondary, fontSize = 11.sp)
              Spacer(Modifier.height(8.dp))
              Text("LIFETIME TOTALS", color = RFColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    FaIcon(codepoint = "\uf02d", useSolid = true, sizeSp = 13)
  }
}

@Composable
private fun RowScope.EditFaButton(onClick: () -> Unit) {
  Box(Modifier.padding(2.dp).clip(RoundedCornerShape(4.dp)).clickable { onClick() }.padding(4.dp)) {
    FaIcon(codepoint = "\uf303", useSolid = true, sizeSp = 13)
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
  roleFilter: String, onRole: (String) -> Unit,
  sortKey: String, onSort: (String) -> Unit,
  descending: Boolean, onDir: (Boolean) -> Unit,
  conditions: List<BrowserCondition>, onAdd: (BrowserCondition) -> Unit,
  onRemove: (BrowserCondition) -> Unit
) {
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      MiniDropdown("Faction: $factionFilter", listOf("All", "Nuia", "Haranya", "Pirate")) { onFaction(it) }
      MiniField("Guild", guildFilter, onGuild, Modifier.weight(1f))
      MiniField("Spec", specFilter, onSpec, Modifier.weight(1f))
      MiniField("Min GS", gearFilter, onGear, Modifier.width(90.dp))
      MiniDropdown("Seen: $lastSeenFilter", LAST_SEEN_OPTIONS.map { it.first }) { onLastSeen(it) }
      MiniDropdown("Role: $roleFilter", ROLE_OPTIONS) { onRole(it) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      MiniDropdown("Sort: ${BROWSER_STATS.firstOrNull { it.key == sortKey }?.label ?: sortKey}",
        BROWSER_STATS.map { it.key }) { onSort(it) }
      Text(if (descending) "DESC" else "ASC", color = RFColors.AccentRed, fontSize = 11.sp,
        modifier = Modifier.clickable { onDir(!descending) })
      conditions.forEach { c ->
        Text("${c.statKey} ${c.op} ${c.value}  ✕", color = RFColors.TextPrimary, fontSize = 11.sp,
          modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A)).padding(horizontal = 6.dp, vertical = 2.dp)
            .clickable { onRemove(c) })
      }
      if (conditions.size < 5) ConditionAdder { onAdd(it) }
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
private fun ConditionAdder(onAdd: (BrowserCondition) -> Unit) {
  var stat by remember { mutableStateOf("damage") }
  var op by remember { mutableStateOf(">") }
  var value by remember { mutableStateOf("") }
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    MiniDropdown(BROWSER_STATS.firstOrNull { it.key == stat }?.label ?: stat, BROWSER_STATS.map { it.key }) { stat = it }
    MiniDropdown(op, OP_OPTIONS) { op = it }
    MiniField("val (5M)", value, { value = it }, Modifier.width(90.dp))
    Text("+ Add", color = RFColors.AccentRed, fontSize = 11.sp, modifier = Modifier.clickable {
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

private fun showCsvSaveChooser(suggestedName: String, onFileSelected: (File) -> Unit, onCancel: () -> Unit = {}) {
  try {
    SwingUtilities.invokeLater {
      try {
        val chooser = JFileChooser()
        chooser.dialogTitle = "Export Player Browser CSV"
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
