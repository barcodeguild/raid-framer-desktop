package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.browser.BROWSER_STATS
import com.reoky.raidframer.core.browser.BrowserCondition
import com.reoky.raidframer.core.browser.HERO_STATS
import com.reoky.raidframer.core.browser.applyBrowserFilters
import com.reoky.raidframer.core.browser.exportBrowserCsv
import com.reoky.raidframer.core.browser.parseShorthand
import com.reoky.raidframer.core.browser.sortBrowser
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.core.helpers.togglePlayerCard
import com.reoky.raidframer.core.helpers.togglePocketJournal
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CompactSessionTotals
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

private val LAST_SEEN_OPTIONS = listOf("All" to 0L, "7d" to 7L, "30d" to 30L, "90d" to 90L)
private val OP_OPTIONS = listOf(">", ">=", "<", "=")
private const val HERO_CYCLE_MS = 6000L

@Composable
fun PlayerBrowserOverlay(wm: WindowManager?) {
  val dragLock = LocalDragLock.current
  val scope = rememberCoroutineScope()
  var allPlayers by remember { mutableStateOf<List<PlayerCacheEntity>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }
  var search by remember { mutableStateOf("") }
  var dropdownOpen by remember { mutableStateOf(false) }
  var selected by remember { mutableStateOf<PlayerCacheEntity?>(null) }
  var sortKey by remember { mutableStateOf("damage") }
  var descending by remember { mutableStateOf(true) }
  var factionFilter by remember { mutableStateOf("All") }
  var guildFilter by remember { mutableStateOf("") }
  var specFilter by remember { mutableStateOf("") }
  var gearFilter by remember { mutableStateOf("") }
  var lastSeenFilter by remember { mutableStateOf("All") }
  var conditions by remember { mutableStateOf(listOf<BrowserCondition>()) }
  var heroIndex by remember { mutableStateOf(0) }
  var heroPaused by remember { mutableStateOf(false) }
  var minorKey by remember { mutableStateOf(BROWSER_STATS.random().key) }
  var exportStatus by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    loading = true
    val data = withContext(Dispatchers.IO) {
      try { RFDao.playerCacheDao.getRecentPlayerCacheMetadata() } catch (e: Exception) { emptyList() }
    }
    allPlayers = data
    loading = false
  }

  LaunchedEffect(dropdownOpen, search) {
    dragLock.value = dropdownOpen || search.isNotEmpty()
  }

  LaunchedEffect(heroPaused) {
    while (!heroPaused) {
      delay(HERO_CYCLE_MS)
      heroIndex = (heroIndex + 1) % HERO_STATS.size
    }
  }

  val lastSeenAfter = remember(lastSeenFilter) {
    val days = LAST_SEEN_OPTIONS.firstOrNull { it.first == lastSeenFilter }?.second ?: 0L
    if (days <= 0L) 0L else System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
  }

  val filtered = remember(allPlayers, search, factionFilter, guildFilter, specFilter, gearFilter, lastSeenAfter, conditions, sortKey, descending) {
    val gear = gearFilter.toIntOrNull() ?: 0
    sortBrowser(
      applyBrowserFilters(allPlayers, search, factionFilter, guildFilter, specFilter, gear, lastSeenAfter, conditions),
      sortKey, descending
    )
  }

  val suggestions = remember(search, allPlayers) {
    if (search.isBlank()) emptyList()
    else allPlayers.filter {
      it.playerName.contains(search, true) || it.lastKnownGuild.contains(search, true)
    }.take(8)
  }

  val hero = HERO_STATS[heroIndex % HERO_STATS.size]
  val heroTop = remember(filtered, hero) { sortBrowser(filtered, hero.key, true).take(5) }
  val minorDef = remember(minorKey) { BROWSER_STATS.firstOrNull { it.key == minorKey } ?: BROWSER_STATS[0] }
  val minorTop = remember(filtered, minorDef) { sortBrowser(filtered, minorDef.key, true).firstOrNull() }

  Column(
    modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E)).padding(10.dp)
  ) {
    TitleBarComponent(title = "Player Browser", onClose = { wm?.closeWindow(OverlayType.PLAYER_BROWSER) })

    Box(Modifier.fillMaxWidth()) {
      Column {
        TextField(
          value = search,
          onValueChange = { search = it; dropdownOpen = true },
          placeholder = { Text("Search players or guilds...", color = RFColors.TextTertiary, fontSize = 13.sp) },
          modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            .onFocusChanged { dragLock.value = it.isFocused }
            .onKeyEvent {
              if (it.key == Key.Enter) {
                val pick = suggestions.firstOrNull()
                if (pick != null) { selected = pick; dropdownOpen = false }
                true
              } else false
            },
          singleLine = true,
          colors = TextFieldDefaults.textFieldColors(
            textColor = RFColors.TextPrimary, backgroundColor = Color(0xFF1E1E1E),
            focusedIndicatorColor = RFColors.AccentRed, unfocusedIndicatorColor = RFColors.CardBorder,
            cursorColor = RFColors.AccentRed
          ),
          textStyle = TextStyle(fontSize = 13.sp)
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
                modifier = Modifier.fillMaxWidth().clickable { selected = p; dropdownOpen = false; dragLock.value = false }
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

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      HERO_STATS.forEachIndexed { i, def ->
        val isActive = i == heroIndex % HERO_STATS.size
        Column(
          modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
            .background(if (isActive) RFColors.CardBackground else Color(0xFF141414))
            .clickable { heroIndex = i; heroPaused = true; sortKey = def.key }
            .padding(8.dp)
        ) {
          Text(def.label, color = def.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          val top = sortBrowser(filtered, def.key, true).take(3)
          top.forEachIndexed { r, p ->
            Text("#${r + 1} ${p.playerName} ${def.extract(p).humanReadableAbbreviation()}",
              color = RFColors.TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          if (top.isEmpty()) Text("—", color = RFColors.TextTertiary, fontSize = 11.sp)
        }
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
      Text("Key fact: ", color = RFColors.TextTertiary, fontSize = 11.sp)
      Text(
        minorTop?.let { "#1 ${minorDef.label}: ${it.playerName} (${minorDef.extract(it).humanReadableAbbreviation()})" } ?: "—",
        color = minorDef.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
      )
      Spacer(Modifier.width(8.dp))
      Text("shuffle", color = RFColors.AccentRed, fontSize = 11.sp,
        modifier = Modifier.clickable { minorKey = BROWSER_STATS.random().key })
    }

    FilterBar(
      factionFilter = factionFilter, onFaction = { factionFilter = it },
      guildFilter = guildFilter, onGuild = { guildFilter = it },
      specFilter = specFilter, onSpec = { specFilter = it },
      gearFilter = gearFilter, onGear = { gearFilter = it },
      lastSeenFilter = lastSeenFilter, onLastSeen = { lastSeenFilter = it },
      sortKey = sortKey, onSort = { sortKey = it },
      descending = descending, onDir = { descending = it },
      conditions = conditions, onAdd = { conditions = (conditions + it).take(5) },
      onRemove = { conditions = conditions - it }
    )

    Spacer(Modifier.height(6.dp))

    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(Modifier.weight(1.6f).fillMaxHeight()) {
        if (loading) {
          Text("Loading players...", color = RFColors.TextTertiary, fontSize = 13.sp)
        } else {
          Text("${filtered.size} players", color = RFColors.TextTertiary, fontSize = 11.sp)
          LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(filtered.take(400)) { i, p ->
              BrowserRow(i, p, sortKey, onClick = { selected = p })
            }
          }
        }
      }
      Column(
        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
          .background(RFColors.CardBackground).padding(10.dp).verticalScroll(rememberScrollState())
      ) {
        val sel = selected
        if (sel == null) {
          Text("Select a player to preview", color = RFColors.TextTertiary, fontSize = 12.sp)
        } else {
          Text(sel.playerName, color = RFColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          Text("${sel.lastKnownGuild}  •  ${sel.lastKnownSpec}  •  GS ${sel.lastKnownGearScore}",
            color = RFColors.TextSecondary, fontSize = 11.sp)
          Spacer(Modifier.height(6.dp))
          CompactSessionTotals(sel.playerName)
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { togglePlayerCard(wm, sel.playerName) },
              colors = ButtonDefaults.buttonColors(RFColors.AccentRed)) {
              Text("Player Card", color = Color.White, fontSize = 12.sp)
            }
            Button(onClick = { togglePocketJournal(wm) },
              colors = ButtonDefaults.buttonColors(RFColors.CardBorder)) {
              Text("Journal", color = Color.White, fontSize = 12.sp)
            }
          }
        }
        Spacer(Modifier.height(12.dp))
        Text("Export", color = RFColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("${filtered.size} rows match current filters", color = RFColors.TextTertiary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Button(
          onClick = {
            exportStatus = ""
            val rows = filtered.toList()
            val summary = buildExportSummary(sortKey, descending, factionFilter, guildFilter, specFilter, gearFilter, lastSeenFilter, conditions)
            // NOTE: do NOT use the composable's rememberCoroutineScope here — closing the
            // browser window disposes the composition and cancels that scope, which is why
            // the export silently never ran. Use an independent scope instead.
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
          colors = ButtonDefaults.buttonColors(RFColors.AccentRed), modifier = Modifier.fillMaxWidth()
        ) {
          Text("Export CSV", color = Color.White, fontSize = 12.sp)
        }
        if (exportStatus.isNotBlank()) {
          Spacer(Modifier.height(4.dp))
          Text(exportStatus, color = RFColors.TextTertiary, fontSize = 10.sp)
        }
      }
    }
  }
}

@Composable
private fun BrowserRow(index: Int, p: PlayerCacheEntity, sortKey: String, onClick: () -> Unit) {
  val def = (BROWSER_STATS + HERO_STATS).firstOrNull { it.key == sortKey } ?: BROWSER_STATS[0]
  val spec = remember(p.lastKnownSpec) {
    try { SpecType.fromName(p.lastKnownSpec) } catch (e: Exception) { SpecType.UNKNOWN }
  }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF181818)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
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
            .getFactionHighlightColor(Faction.fromString(p.lastKnownFaction)), CircleShape))
      }
      Text("${p.lastKnownGuild} • GS ${p.lastKnownGearScore}",
        color = RFColors.TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Column(horizontalAlignment = Alignment.End) {
      Text(def.extract(p).humanReadableAbbreviation(), color = def.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      Text("DMG ${p.lifetimeTotalDamage.humanReadableAbbreviation()} • Heal ${p.lifetimeTotalHealing.humanReadableAbbreviation()} • CC ${p.lifetimeTotalCCDelivered.humanReadableAbbreviation()}",
        color = RFColors.TextTertiary, fontSize = 9.sp, maxLines = 1)
      Text("Glid ${p.lifetimeTotalGliderUses} • Items ${p.lifetimeTotalItemSkillsUsed} • C/D/S ${p.lifetimeTotalCharms}/${p.lifetimeTotalDistresses}/${p.lifetimeTotalSilences} • Sun ${p.lifetimeTotalRegularSunder}/${p.lifetimeTotalMistSunder}",
        color = RFColors.TextTertiary, fontSize = 9.sp, maxLines = 1)
      Text("Recv ${p.lifetimeTotalDamageTaken.humanReadableAbbreviation()}/${p.lifetimeTotalHealsReceived.humanReadableAbbreviation()} • Rez ${p.lifetimeTotalRevive} • Def ${p.lifetimeTotalDefiance} • Cour ${p.lifetimeTotalCourageousAction} • K ${p.lifetimeTotalKills}/${p.lifetimeTotalKillsKB}",
        color = RFColors.TextTertiary, fontSize = 9.sp, maxLines = 1)
    }
  }
}

@Composable
private fun FilterBar(
  factionFilter: String, onFaction: (String) -> Unit,
  guildFilter: String, onGuild: (String) -> Unit,
  specFilter: String, onSpec: (String) -> Unit,
  gearFilter: String, onGear: (String) -> Unit,
  lastSeenFilter: String, onLastSeen: (String) -> Unit,
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
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      MiniDropdown("Sort: ${(BROWSER_STATS + HERO_STATS).firstOrNull { it.key == sortKey }?.label ?: sortKey}",
        (BROWSER_STATS + HERO_STATS).distinctBy { it.key }.map { it.key }) { onSort(it) }
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
    Text(label, color = RFColors.TextPrimary, fontSize = 11.sp,
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
          Text(o, color = RFColors.TextPrimary, fontSize = 11.sp)
        }
      }
    }
    }
  }
}

@Composable
private fun MiniField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
  val dragLock = LocalDragLock.current
  TextField(
    value = value, onValueChange = onChange, placeholder = { Text(label, color = RFColors.TextTertiary, fontSize = 11.sp) },
    singleLine = true, modifier = modifier.height(32.dp).onFocusChanged { dragLock.value = it.isFocused },
    colors = TextFieldDefaults.textFieldColors(
      textColor = RFColors.TextPrimary, backgroundColor = Color(0xFF1E1E1E),
      focusedIndicatorColor = RFColors.CardBorder, unfocusedIndicatorColor = RFColors.CardBorder,
      cursorColor = RFColors.AccentRed),
    textStyle = TextStyle(fontSize = 11.sp)
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
        // Parent to the browser window and force topmost so the always-on-top
        // tool-tip overlay doesn't swallow the dialog behind it.
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
